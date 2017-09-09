package com.xs0.gqlktx

import com.xs0.gqlktx.SemiTypeKind.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.starProjectedType

interface Invokable<CTX> {
    val effectiveType: KType
    val name: String
    val isAsync: Boolean

    fun invoke(context: CTX): Any
    suspend fun invokeAsync(context: CTX): Any
}








class OutMethodInfo {
    lateinit var effectiveType: KType
    lateinit var name: String
    lateinit var callable: KFunction<*>
    lateinit var params: Array<ParamInfo>
    lateinit var callMode: CallMode

    val isAsync: Boolean
        get() = callMode != CallMode.REGULAR
}