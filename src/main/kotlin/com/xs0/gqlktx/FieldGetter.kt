package com.xs0.gqlktx

import com.xs0.gqlktx.dom.Value
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1

class PublicParamInfo(
    val name: String,
    val type: SemiType,
    val defaultValue: Value?,
    val description: String?,
)

enum class ParamGetterMode {
    METHOD,
    SUSPEND_METHOD,
    COMP_FUTURE
}

interface FieldGetter<in CTX> {
    val isSuspending: Boolean
    val publicType: SemiType
    val name: String
    val publicParams: Map<String, PublicParamInfo>
    val mode: ParamGetterMode
    val description: String?
    val isDeprecated: Boolean
    val deprecationReason: String?

    suspend fun invoke(receiver: Any, context: CTX, arguments: Map<String, Any?>): Any?
    fun codeGenCall(objExpr: String, ctxExpr: String): String
}

class FieldGetterRegularFunction<in CTX>(
    override val publicType: SemiType,
    override val name: String,
    private val callable: KCallable<*>,
    private val params: Array<ParamInfo<CTX>>,
    override val publicParams: Map<String, PublicParamInfo>,
    override val description: String?,
    override val isDeprecated: Boolean,
    override val deprecationReason: String?,
): FieldGetter<CTX> {
    init {
        params.filter { it.kind == ParamKind.CONTINUATION }.
               forEach { throw IllegalArgumentException(it.toString()) }
    }

    override val isSuspending: Boolean
        get() = false

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

    override val mode: ParamGetterMode
        get() = ParamGetterMode.METHOD

    override fun codeGenCall(objExpr: String, ctxExpr: String): String {
        if (callable is KFunction) {
            val sb = StringBuilder()
            sb.append(objExpr).append('.')
            sb.append(callable.name).append('(')
            var first = true
            for (param in params) {
                if (param.kind == ParamKind.THIS)
                    continue

                if (first) {
                    first = false
                } else {
                    sb.append(", ")
                }

                if (param.kind == ParamKind.NULL) {
                    sb.append("null")
                } else
                if (param.kind == ParamKind.CONTEXT) {
                    sb.append(param.ctxGetter!!.codeGen(ctxExpr))
                } else {
                    sb.append("_" + param.name)
                }
            }
            return sb.append(")").toString()
        } else
        if (callable is KProperty1<*,*>) {
            return objExpr + "." + callable.name
        } else {
            TODO("Not yet implemented")
        }
    }
}

class FieldGetterCoroutine<in CTX>(
    override val publicType: SemiType,
    override val name: String,
    private val callable: KCallable<*>,
    private val params: Array<ParamInfo<CTX>>,
    override val publicParams: Map<String, PublicParamInfo>,
    override val description: String?,
    override val isDeprecated: Boolean,
    override val deprecationReason: String?,
): FieldGetter<CTX> {
    init {
        if (params.count { it.kind == ParamKind.CONTINUATION } != 1)
            throw Error("There should be exactly one continuation parameter")
    }

    override val isSuspending: Boolean
        get() = true

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

    override val mode: ParamGetterMode
        get() = ParamGetterMode.SUSPEND_METHOD

    override fun codeGenCall(objExpr: String, ctxExpr: String): String {
        if (callable is KFunction) {
            val sb = StringBuilder()
            sb.append(objExpr).append('.')
            sb.append(callable.name).append('(')
            var first = true
            for (param in params) {
                if (param.kind == ParamKind.THIS || param.kind == ParamKind.CONTINUATION)
                    continue

                if (first) {
                    first = false
                } else {
                    sb.append(", ")
                }

                if (param.kind == ParamKind.NULL) {
                    sb.append("null")
                } else
                if (param.kind == ParamKind.CONTEXT) {
                    sb.append(param.ctxGetter!!.codeGen(ctxExpr))
                } else {
                    sb.append("_" + param.name)
                }
            }
            return sb.append(")").toString()
        } else {
            TODO("Not yet implemented")
        }
    }
}



class FieldGetterCompletableFuture<in CTX>(
    override val publicType: SemiType,
    override val name: String,
    private val callable: KCallable<*>,
    private val params: Array<ParamInfo<CTX>>,
    override val publicParams: Map<String, PublicParamInfo>,
    override val description: String?,
    override val isDeprecated: Boolean,
    override val deprecationReason: String?,
): FieldGetter<CTX> {
    init {
        params.filter { it.kind == ParamKind.CONTINUATION }.
                forEach { throw IllegalArgumentException(it.toString()) }
    }

    override val isSuspending: Boolean
        get() = true

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

    override val mode: ParamGetterMode
        get() = ParamGetterMode.COMP_FUTURE

    override fun codeGenCall(objExpr: String, ctxExpr: String): String {
        if (callable is KFunction) {
            val sb = StringBuilder()
            sb.append(objExpr).append('.')
            sb.append(callable.name).append('(')
            var first = true
            for (param in params) {
                if (param.kind == ParamKind.THIS || param.kind == ParamKind.CONTINUATION)
                    continue

                if (first) {
                    first = false
                } else {
                    sb.append(", ")
                }

                if (param.kind == ParamKind.NULL) {
                    sb.append("null")
                } else
                if (param.kind == ParamKind.CONTEXT) {
                    sb.append(param.ctxGetter!!.codeGen(ctxExpr))
                } else {
                    sb.append("_" + param.name)
                }
            }
            return sb.append(")").toString()
        } else {
            TODO("Not yet implemented")
        }
    }
}
