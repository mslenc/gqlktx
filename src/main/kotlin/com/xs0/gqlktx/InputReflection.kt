package com.xs0.gqlktx

import com.xs0.gqlktx.ann.GQLArg
import com.xs0.gqlktx.ann.GraphQLInput
import com.xs0.gqlktx.parser.GraphQLParser
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.valueParameters

enum class PropMode {
    REQUIRED,
    OPTIONAL,
    FORBIDDEN
}

class PropInMode(
    val name: String,
    val propMode: PropMode,
    val setter: FieldSetter? // if it's null it goes into constructor (unless FORBIDDEN, of course)
)

class ReflectedInputMode(
    val factory: KCallable<Any>,
    val props: Array<PropInMode>
)

class InputPropInfo(
    val type: SemiType,
    val defaultValue: Any?
) {
    val alwaysRequired: Boolean
        get() = !type.nullable
}

class ReflectedInput(
    val propTypes: Map<String, InputPropInfo>,
    val modes: Array<ReflectedInputMode>
)

fun reflectInputObject(klass: KClass<*>): ReflectedInput {
    if (klass.isAbstract || klass.isInner || klass.isSealed || klass.ignored || klass.isCompanion)
        throw IllegalArgumentException("Input class can't be abstract, inner, sealed, ignored or companion")
    if (!klass.isPublic)
        throw IllegalArgumentException("Input class must be public")

    val propTypes = LinkedHashMap<String, InputPropInfo>()
    val propSetters = HashMap<String, FieldSetter>()

    for (func in klass.memberFunctions) {
        val ann = func.findAnnotation<GraphQLInput>()
        val forced = ann != null

        if (!func.isPublic || func.ignored || func.valueParameters.size != 1 || func.returnType.classifier != Unit::class) {
            throwIf(forced) { "Function $func isn't public, is @GqlIgnore'd, doesn't take a single parameter or returns a value" }
            continue
        }
        if (func.isInfix || func.isInline || func.isSuspend || func.isAbstract) {
            throwIf(forced) { "Function $func is infix, inline, suspend or abstract, but it shouldn't be"}
            continue
        }

        val name = ann?.value.trimToNull() ?: setterName(func.name)
        if (name == null || !validGraphQLName(name, false)) {
            throwIf(forced) { "$name is not a valid GraphQL name (in $klass)" }
            continue
        }

        val param = func.valueParameters.first()
        if (param.isOptional || param.type.classifier !is KClass<*>) {
            throwIf(forced) { "$func has a type that is unsupported at this time" }
            continue
        }

        val type = SemiType.create(param.type)
        if (type == null) {
            throwIf(forced) { "$func has a type that is unsupported at this time" }
            continue
        }

        val defaultValue = ann?.defaultsTo.trimToNull()?.parseGqlValue()

        val prev = propTypes.put(name, InputPropInfo(type, defaultValue))
        if (prev != null)
            throw IllegalStateException("More than one setter resolved to name $name in $klass")

        propSetters.put(name, FieldSetterRegularFunction(type, name, func))
    }

    for (prop in klass.memberProperties) {
        val ann = prop.findAnnotation<GraphQLInput>()
        val forced = ann != null

        if (!prop.isPublic || prop.ignored || prop.isConst) {
            throwIf(forced) { "Property $prop isn't public, is @GqlIgnore'd or is const" }
            continue
        }
        if (prop !is KMutableProperty1<*,*>) {
            throwIf(forced) { "Property $prop isn't mutable" }
            continue
        }
        @Suppress("UNCHECKED_CAST")
        prop as KMutableProperty1<Any, Any?>

        val name = ann?.value.trimToNull() ?: prop.name
        if (!validGraphQLName(name, false)) {
            throwIf(forced) { "$name is not a valid GraphQL name (in $klass)" }
            continue
        }

        val type = SemiType.create(prop.setter.valueParameters.first().type)
        if (type == null) {
            throwIf(forced) { "$prop has a setter type that is unsupported at this time" }
            continue
        }

        val defaultValue = ann?.defaultsTo.trimToNull()?.parseGqlValue()

        val prev = propTypes.put(name, InputPropInfo(type, defaultValue))
        if (prev != null)
            throw IllegalStateException("More than one setter resolved to name $name in $klass")

        propSetters.put(name, FieldSetterProperty(type, name, prop))
    }

    val pendingModes = ArrayList<Pair<KCallable<Any>, ArrayList<PropInMode>>>()

    nextCons@
    for (cons in klass.constructors) {
        if (cons.ignored || !cons.isPublic)
            continue

        val args = ArrayList<PropInMode>()
        val thisConsProps = LinkedHashMap<String, SemiType>()

        for (param in cons.parameters) {
            val ann = param.findAnnotation<GQLArg>()
            val forced = ann != null

            if (param.ignored)
                throw IllegalArgumentException("Can't use @GqlIgnore on constructor parameters - use it on the whole constructor instead")
            if (ann != null && ann.defaultsTo.trimToNull() != null)
                throw IllegalArgumentException("Can't use defaultsTo on constructor parameters at this time")

            val type = SemiType.create(param.type)
            if (type == null) {
                throwIf(forced) { "Type of $param is unusable at this time" }
                continue@nextCons
            }

            val name = ann?.value.trimToNull() ?: param.name
            if (name == null) {
                throwIf(forced) { "Couldn't determine name of $param"}
                continue@nextCons
            }

            if (name in thisConsProps.keys) {
                throwIf(forced) { "Name $name is used on multiple parameters when using $cons" }
                continue@nextCons
            }

            val propMode: PropMode = if (type.nullable) PropMode.OPTIONAL else PropMode.REQUIRED
            thisConsProps[name] = type
            args.add(PropInMode(name, propMode, null))
        }

        for ((name, type) in thisConsProps) {
            val existing = propTypes[name]
            if (existing == null) {
                propTypes[name] = InputPropInfo(type, null)
            } else {
                if (!typesCompatible(type, existing.type))
                    throw IllegalStateException("In $klass, property $name has two incompatible types: $type and ${existing.type}")
                if (existing.defaultValue != null)
                    throw IllegalStateException("In $klass, property $name has a default value provided via setter, but is also used in a constructor, which doesn't support default values")
                if (existing.alwaysRequired && type.nullable)
                    propTypes[name] = InputPropInfo(type, null)
            }
        }

        pendingModes.add(Pair(cons, args))
    }

    val modes = ArrayList<ReflectedInputMode>()
    for ((cons, args) in pendingModes) {
        val namesDone = HashSet<String>()
        for (arg in args)
            namesDone.add(arg.name)

        for (name in propSetters.keys)
            if (namesDone.add(name))
                args.add(PropInMode(name, PropMode.OPTIONAL, propSetters[name]))

        for (name in propTypes.keys)
            if (namesDone.add(name))
                args.add(PropInMode(name, PropMode.FORBIDDEN, null))

        modes.add(ReflectedInputMode(cons, args.toTypedArray()))
    }

    if (modes.isEmpty())
        throw IllegalArgumentException("No valid modes found in $klass (that probably means no usable public constructor)")

    return ReflectedInput(propTypes, modes.toTypedArray())
}

fun typesCompatible(a: SemiType, b: SemiType): Boolean {
    if (a.kind != b.kind)
        return false
    if (a.inner != b.inner)
        return false
    if (a.klass != b.klass)
        return false
    // nullable, OTOH, can be different
    return true
}

fun String.parseGqlValue(): Any? {
    return GraphQLParser.parseValue(this).toJson()
}

inline fun throwIf(condition: Boolean, msg: () -> String) {
    if (condition)
        throw IllegalStateException(msg())
}