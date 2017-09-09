package com.xs0.gqlktx

import com.xs0.gqlktx.ann.GqlField
import com.xs0.gqlktx.ann.GqlIgnore
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import kotlin.coroutines.experimental.Continuation
import kotlin.reflect.*
import kotlin.reflect.full.*

enum class CallMode {
    REGULAR,
    SUSPEND,
    VERTX_FUTURE,
    VERTX_HANDLER
}

enum class ParamKind {
    THIS, // receiver object
    PUBLIC, // public parameter
    CONTEXT, // object provided by context
    CONTINUATION, // for suspend functions
    HANDLER, // for VERTX_HANDLER functions
    NULL // for ignored parameters which are also T?
}

val KParameter.ignored: Boolean get() = findAnnotation<GqlIgnore>() != null
val KCallable<*>.ignored: Boolean get() = findAnnotation<GqlIgnore>() != null

class ParamInfo(
    var name: String,
    var kind: ParamKind,
    var type: KType? = null,
    var semiType: SemiType? = null// for PARAM and CONTEXT only
) {
    companion object {
        fun create(param: KParameter, contextTypes: ContextTypes<*>): ParamInfo? {
            if (param.kind == KParameter.Kind.INSTANCE || param.kind == KParameter.Kind.EXTENSION_RECEIVER) {
                return ParamInfo("<this>", ParamKind.THIS)
            }

            if (param.ignored) {
                return if (param.type.isMarkedNullable) {
                    ParamInfo(param.name ?: "<ignored>", ParamKind.NULL)
                } else {
                    null
                }
            }

            if (couldBeHandler(param.type)) {
                val innerType = handlerType(param.type)
                if (innerType != null) {
                    return ParamInfo(param.name ?: "<handler>", ParamKind.HANDLER, innerType)
                } else {
                    return null
                }
            }

            val contextType = contextTypes.get(param.type.classifier)
            if (contextType != null) {
                return ParamInfo(param.name ?: "<ctx>", ParamKind.CONTEXT, param.type.classifier?.starProjectedType)
            }

            val semiType = SemiType.create(param.type)
            if (semiType != null) {
                return ParamInfo(param.name ?: return null, ParamKind.PUBLIC, semiType.sourceType, semiType)
            } else {
                return null
            }
        }
    }
}

fun extractFieldName(member: KCallable<*>, isBool: Boolean, skipNameValidation: Boolean): String? {
    var name: String? = null
    val ann = member.findAnnotation<GqlField>()
    val force = ann != null

    if (ann != null && !ann.name.isEmpty()) {
        if (validGraphQLName(ann.name, false))
            return ann.name
        throw IllegalArgumentException("${ann.name} is not a valid GraphQL field name")
    }

    if (member is KFunction) {
        name = getterName(member.name, isBool)
        if (name == null && force)
            name = member.name
    } else
    if (member is KProperty) {
        name = member.name
    }

    if (force && !validGraphQLName(name, false))
        throw IllegalArgumentException("${ann.name} is not a valid GraphQL field name")

    return name
}

fun couldBeHandler(type: KType): Boolean {
    return type.classifier == Handler::class && type.arguments.size == 1
}

fun handlerType(type: KType): KType? {
    if (!couldBeHandler(type))
        return null

    val asyncRes = type.arguments[0].type
    if (asyncRes?.classifier != AsyncResult::class || asyncRes.arguments.size != 1)
        return null

    val finalRes = asyncRes.arguments[0].type ?: return null
    if (finalRes.classifier is KClass<*>)
        return finalRes

    return null
}


fun couldBeFuture(type: KType): Boolean {
    return type.classifier == Future::class && type.arguments.size == 1
}

fun futureType(type: KType): KType? {
    if (!couldBeFuture(type))
        return null

    val finalRes = type.arguments[0].type ?: return null
    if (finalRes.classifier is KClass<*>)
        return finalRes

    return null
}




fun isVoid(type: KType): Boolean {
    if (Void::class.starProjectedType.isSupertypeOf(type))
        return true
    if (Void.TYPE.kotlin.starProjectedType.isSupertypeOf(type))
        return true


    val c = type.classifier
    return when (c) {
        null -> false
        is KClass<*> -> {
            c == Unit::class || c == Void::class || c == Void.TYPE.kotlin
        }
        else -> false
    }
}






fun <CTX> processFieldFunc(member: KCallable<*>, instanceType: KClass<*>, contextTypes: ContextTypes<CTX>, skipNameValidation: Boolean = false): Invokable<CTX>? {
    if (member.visibility !== KVisibility.PUBLIC)
        return null

    if (member is KProperty2<*, *, *>)
        return null

    if (member.findAnnotation<GqlIgnore>() != null)
        return null

    val thisParam = member.instanceParameter ?: member.extensionReceiverParameter
    if (thisParam != null) {
        val klass = thisParam.type.classifier as? KClass<*> ?: return null

        if (!klass.isSuperclassOf(instanceType))
            return null

        if (klass == Any::class)
            return null
    }

    val forced = member.findAnnotation<GqlField>() != null

    val isSuspend: Boolean
    if (member is KFunction) {
        if (member.isInline || member.isInfix || member.isOperator)
            return null
        isSuspend = member.isSuspend
    } else {
        isSuspend = false
    }

    var retType = futureType(member.returnType)
    val isFuture = retType != null

    if (isFuture && isSuspend) {
        if (forced) {
            throw IllegalStateException("Member $member both returns a Future and is suspend, which is not supported")
        } else {
            return null
        }
    }

    retType = retType ?: SemiType.create(member.returnType)

    if (retType == null) {
        if (forced) {
            throw IllegalStateException("Member $member marked as a field, but has unsupported type")
        } else {
            return null
        }
    }

    val name = extractFieldName(member, retType.isBoolean, skipNameValidation) ?: return null

    val params = ArrayList<ParamInfo>()

    nextParam@
    for (param in member.parameters) {
        when (param.kind) {
            KParameter.Kind.INSTANCE,
            KParameter.Kind.EXTENSION_RECEIVER -> {
                params.add(ParamInfo("<this>", ParamKind.THIS, null))
                continue@nextParam
            } else -> {
                // continue below
            }
        }

        val
    }

    if (isSuspend)
        params.add(ParamInfo("<continuation>", ParamKind.CONTINUATION, Continuation::class.createType(listOf(KTypeProjection(KVariance.IN, retType.sourceType)))))

}



fun <CTX> findFields(klass: KClass<*>, ctxTypes: ContextTypes<CTX>): Map<String, Invokable<CTX>> {
    val result = LinkedHashMap<String, Invokable<CTX>>()

    for (callable in klass.members) {
        val field = processFieldFunc(callable, klass, ctxTypes)
        if (field != null) {
            result.put(field.name, field).let {
                throw IllegalStateException("In class $klass, two or more fields map to the same name ${field.name}. You may rename one or more of them with @GqlField, or use @GqlIgnore to ignore some of them.")
            }
        }
    }

    return result
}