package com.xs0.gqlktx

import com.xs0.gqlktx.dom.Value
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.reflect.KCallable

class PublicParamInfo(
    val name: String,
    val type: SemiType,
    val defaultValue: Value?
)

interface FieldGetter<in CTX> {
    val publicType: SemiType
    val name: String
    val publicParams: Map<String, PublicParamInfo>

    suspend fun invoke(receiver: Any, context: CTX, arguments: Map<String, Any?>): Any?
}

class FieldGetterRegularFunction<in CTX>(
    override val publicType: SemiType,
    override val name: String,
    private val callable: KCallable<*>,
    private val params: Array<ParamInfo<CTX>>,
    override val publicParams: Map<String, PublicParamInfo>
): FieldGetter<CTX> {
    init {
        params.filter { it.kind == ParamKind.CONTINUATION }.
               forEach { throw IllegalArgumentException(it.toString()) }
    }

    override suspend fun invoke(receiver: Any, context: CTX, arguments: Map<String, Any?>): Any? {
        val args = arrayOfNulls<Any?>(params.size)

        for (i in params.indices) {
            val param = params[i]

            args[i] = when (param.kind) {
                ParamKind.THIS -> receiver
                ParamKind.PUBLIC -> arguments[param.name]
                ParamKind.CONTEXT -> param.ctxGetter!!.invoke(context)
                ParamKind.NULL -> null

                else ->
                    throw Error("but we checked for this in constructor :(")
            }
        }

        try {
            return callable.call(*args)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }
}

class FieldGetterCoroutine<in CTX>(
        override val publicType: SemiType,
        override val name: String,
        private val callable: KCallable<*>,
        private val params: Array<ParamInfo<CTX>>,
        override val publicParams: Map<String, PublicParamInfo>
): FieldGetter<CTX> {
    init {
        if (params.count { it.kind == ParamKind.CONTINUATION } != 1)
            throw Error("There should be exactly one continuation parameter")
    }

    override suspend fun invoke(receiver: Any, context: CTX, arguments: Map<String, Any?>): Any? {
        return suspendCoroutineUninterceptedOrReturn { cont ->
            val args = arrayOfNulls<Any?>(params.size)

            for (i in params.indices) {
                val param = params[i]

                args[i] = when (param.kind) {
                    ParamKind.THIS -> receiver
                    ParamKind.PUBLIC -> arguments[param.name]
                    ParamKind.CONTEXT -> param.ctxGetter!!.invoke(context)
                    ParamKind.NULL -> null
                    ParamKind.CONTINUATION -> cont
                }
            }

            try {
                return@suspendCoroutineUninterceptedOrReturn callable.call(*args)
            } catch (e: InvocationTargetException) {
                throw e.targetException
            }
        }
    }
}


@Suppress("UNUSED") // TODO
class FieldGetterCompletableFuture<in CTX>(
        override val publicType: SemiType,
        override val name: String,
        private val callable: KCallable<*>,
        private val params: Array<ParamInfo<CTX>>,
        override val publicParams: Map<String, PublicParamInfo>
): FieldGetter<CTX> {
    init {
        params.filter { it.kind == ParamKind.CONTINUATION }.
                forEach { throw IllegalArgumentException(it.toString()) }
    }

    override suspend fun invoke(receiver: Any, context: CTX, arguments: Map<String, Any?>): Any? {
        return suspendCoroutine { cont ->
            val args = arrayOfNulls<Any?>(params.size)

            for (i in params.indices) {
                val param = params[i]

                args[i] = when (param.kind) {
                    ParamKind.THIS -> receiver
                    ParamKind.PUBLIC -> arguments[param.name]
                    ParamKind.CONTEXT -> param.ctxGetter!!.invoke(context)
                    ParamKind.NULL -> null

                    else ->
                        throw Error("but we checked for this in constructor :(")
                }
            }

            @Suppress("UNCHECKED_CAST")
            val future: CompletableFuture<Any?>? = try {
                callable.call(*args)
            } catch (e: InvocationTargetException) {
                throw e.targetException
            } as? CompletableFuture<Any?>

            if (future == null) {
                cont.resumeWithException(IllegalStateException("Future was not returned from $callable"))
            } else {
                future.handle { result, error ->
                    if (error == null) {
                        cont.resume(result)
                    } else {
                        cont.resumeWithException(error)
                    }
                }
            }
        }
    }
}
