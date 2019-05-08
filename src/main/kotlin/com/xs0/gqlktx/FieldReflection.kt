package com.xs0.gqlktx

import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.parser.GraphQLParser
import java.util.concurrent.CompletableFuture
import kotlin.reflect.*
import kotlin.reflect.full.*

enum class ParamKind {
    THIS, // receiver object
    PUBLIC, // public parameter
    CONTEXT, // object provided by context
    CONTINUATION, // for suspend functions
    NULL // for ignored parameters which are also T?
}

val KParameter.ignored: Boolean get() = findAnnotation<GqlIgnore>() != null
val KCallable<*>.ignored: Boolean get() = findAnnotation<GqlIgnore>() != null
val KClass<*>.ignored: Boolean get() = findAnnotation<GqlIgnore>() != null
val KCallable<*>.isPublic: Boolean get() = this.visibility == KVisibility.PUBLIC
val KClass<*>.isPublic: Boolean get() = this.visibility == KVisibility.PUBLIC


class ParamInfo<CTX> private constructor(
    val name: String,
    val kind: ParamKind,
    val semiType: SemiType?,
    val ctxGetter: SyncInvokable<CTX>?
) {
    constructor(name: String, semiType: SemiType) : this(name, ParamKind.PUBLIC, semiType, null)
    constructor(ctxGetter: SyncInvokable<CTX>): this("<ctx>", ParamKind.CONTEXT, null, ctxGetter)

    constructor(kind: ParamKind) : this(kind.toString(), kind, null, null) {
        when (kind) {
            ParamKind.THIS,
            ParamKind.CONTINUATION,
            ParamKind.NULL ->
                return
            else ->
                throw IllegalStateException("Missing info about $kind")
        }
    }

    companion object {
        fun <CTX> create(param: KParameter, contextTypes: ContextTypes<CTX>): ParamInfo<CTX>? {
            if (param.kind == KParameter.Kind.INSTANCE || param.kind == KParameter.Kind.EXTENSION_RECEIVER) {
                return ParamInfo(ParamKind.THIS)
            }

            if (param.ignored) {
                return if (param.type.isMarkedNullable) {
                    ParamInfo(ParamKind.NULL)
                } else {
                    null
                }
            }

            val contextType = contextTypes[param.type.classifier]
            if (contextType != null) {
                return ParamInfo(contextType)
            }


            var semiType = SemiType.create(param.type)
            if (semiType != null) {
                if (param.isOptional && !semiType.nullable)
                    semiType = semiType.withNullable()
                return ParamInfo(param.name ?: return null, semiType)
            } else {
                return null
            }
        }
    }
}

fun extractFieldName(member: KCallable<*>, isBool: Boolean): String? {
    var name: String? = null
    val ann = member.findAnnotation<GqlField>()
    val forced = ann != null

    if (ann != null && !ann.name.isEmpty()) {
        if (validGraphQLName(ann.name, false))
            return ann.name
        throw IllegalArgumentException("${ann.name} is not a valid GraphQL field name")
    }

    if (member is KFunction) {
        name = getterName(member.name, isBool)
        if (name == null && forced)
            name = member.name
    } else
    if (member is KProperty) {
        name = member.name
    }

    if (!validGraphQLName(name, false))
        return nullOrThrowIf(forced) { "$name is not a valid GraphQL field name" }

    return name
}

fun isVoid(type: KType): Boolean {
    if (Unit::class.starProjectedType.isSupertypeOf(type))
        return true
    if (Nothing::class.starProjectedType.isSupertypeOf(type))
        return true
    return false
}

inline fun <T> nullOrThrowIf(condition: Boolean, msg: () -> String): T? {
    if (condition)
        throw IllegalStateException(msg())
    return null
}




fun <CTX> processFieldFunc(member: KCallable<*>, instanceType: KClass<*>, contextTypes: ContextTypes<CTX>): FieldGetter<CTX>? {
    if (member.visibility !== KVisibility.PUBLIC)
        return null

    if (member is KProperty2<*, *, *>)
        return null

    if (member.findAnnotation<GqlIgnore>() != null)
        return null

    val forced = member.findAnnotation<GqlField>() != null

    val thisParam = member.instanceParameter ?: member.extensionReceiverParameter
    if (thisParam != null) {
        val klass = thisParam.type.classifier as? KClass<*> ?: return null

        if (!klass.isSuperclassOf(instanceType))
            return nullOrThrowIf(forced) { "Receiver type not valid" } // this shouldn't really happen

        if (klass == Any::class) // skip standard equals, hashCode and toString
            return null
    }



    val isSuspend: Boolean =
    if (member is KFunction) {
        if (member.isInline || member.isInfix || member.isOperator)
            return nullOrThrowIf(forced) { "inline, infix and operator functions not supported" }
        member.isSuspend
    } else {
        false
    }

    var retType = extractTypeParam(member.returnType, CompletableFuture::class)
    val isFuture = retType != null

    if (isFuture && isSuspend)
        return nullOrThrowIf(forced) { "Member $member both returns a Future and is suspend, which is not supported" }

    val isVoid = retType == null && isVoid(member.returnType)

    if (retType == null && !isVoid) {
        retType = SemiType.create(member.returnType)?.sourceType
        if (retType == null)
            return nullOrThrowIf(forced) { "Member $member has a return type that isn't supported"}
    }

    val params = ArrayList<ParamInfo<CTX>>()
    val publicParams = LinkedHashMap<String, PublicParamInfo>()
    val parsedRetType = if (retType != null) SemiType.create(retType) else null

    nextParam@
    for (param in member.parameters) {
        val parsedParam = ParamInfo.create(param, contextTypes)
        if (parsedParam == null)
            return nullOrThrowIf(forced) { "Parameter $param is not supported" }

        params += parsedParam

        val parsedDefault: Value? =
            param.findAnnotation<GqlParam>()?.let {
                it.defaultsTo.trimToNull()?.let {
                    GraphQLParser.parseValue(it)
                }
            }

        when (parsedParam.kind) {
            ParamKind.THIS,
            ParamKind.CONTEXT,
            ParamKind.NULL -> {
                // that's it
            }

            ParamKind.PUBLIC -> {
                publicParams[parsedParam.name] = PublicParamInfo(parsedParam.name, parsedParam.semiType!!, parsedDefault)
            }

            ParamKind.CONTINUATION -> throw Error("continuation became visible")
        }
    }

    if (parsedRetType == null)
        return nullOrThrowIf(forced) { "Couldn't determine return type for $member" }

    val name = extractFieldName(member, parsedRetType.isBoolean) ?: return null

    if (isSuspend)
        params.add(ParamInfo(ParamKind.CONTINUATION))

    val paramArray = params.toTypedArray()

    return when {
        isSuspend -> FieldGetterCoroutine(parsedRetType, name, member, paramArray, publicParams)
        isFuture -> FieldGetterCompletableFuture(parsedRetType, name, member, paramArray, publicParams)
        else -> FieldGetterRegularFunction(parsedRetType, name, member, paramArray, publicParams)
    }
}



fun <CTX> findFields(klass: KClass<*>, ctxTypes: ContextTypes<CTX>): Map<String, FieldGetter<CTX>> {
    val result = LinkedHashMap<String, FieldGetter<CTX>>()

    for (callable in klass.members) {
        if (callable.ignored)
            continue

        val field = processFieldFunc(callable, klass, ctxTypes)
        if (field != null) {
            result.put(field.name, field).let { prev->
                if (prev != null)
                    throw IllegalStateException("In class $klass, two or more fields map to the same name ${field.name}. You may rename one or more of them with @GqlField, or use @GqlIgnore to ignore some of them.")
            }
        }
    }

    return result
}

