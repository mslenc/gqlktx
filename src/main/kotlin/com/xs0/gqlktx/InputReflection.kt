package com.xs0.gqlktx

import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.parser.GraphQLParser
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.*

enum class PropMode {
    REQUIRED,
    OPTIONAL,
    MAYBE
}

class InputPropInfo(
    val name: String,
    val propMode: PropMode,
    val type: SemiType,
    val defaultValue: Value?
)

class ReflectedInput(
    val constructor: KCallable<Any>,
    val props: Array<InputPropInfo>
) {
    val propIndex = props.associateBy { it.name }
}

fun reflectInputObject(klass: KClass<*>): ReflectedInput {
    if (klass.isAbstract || klass.isInner || klass.isSealed || klass.ignored || klass.isCompanion)
        throw IllegalArgumentException("Input class can't be abstract, inner, sealed, ignored or companion")
    if (!klass.isPublic)
        throw IllegalArgumentException("Input class must be public")

    val cons = klass.primaryConstructor?.let {
        if (it.isPublic && !it.ignored) it else null
    } ?: klass.constructors.singleOrNull {
        it.isPublic && !it.ignored
    } ?: throw IllegalArgumentException("In input class $klass, neither a primary constructor nor a single other applicable constructor were found")

    val props = ArrayList<InputPropInfo>()
    val namesSeen = HashSet<String>()

    for (param in cons.parameters) {
        val ann = param.findAnnotation<GqlParam>()

        if (param.ignored)
            throw IllegalArgumentException("Can't use @GqlIgnore on constructor parameters")

        val defaultValue = ann?.defaultsTo?.trimToNull()?.let {
            GraphQLParser.parseValue(it)
        }

        val type = SemiType.create(param.type) ?: throw IllegalStateException("Type of $param is unusable at this time")

        val name = ann?.name.trimToNull() ?: param.name ?: throw IllegalStateException("Couldn't determine name of $param")

        if (namesSeen.contains(name))
            throw IllegalStateException("Name $name is used on multiple parameters when using $cons")

        val propMode = when {
            type.kind == SemiTypeKind.MAYBE -> PropMode.MAYBE
            type.nullable -> PropMode.OPTIONAL
            else -> PropMode.REQUIRED
        }

        props.add(InputPropInfo(name, propMode, type, defaultValue))
        namesSeen.add(name)
    }

    if (props.isEmpty())
        throw IllegalStateException("Couldn't find any input parameters in $klass, but there must be at least one")

    return ReflectedInput(cons, props.toTypedArray())
}
