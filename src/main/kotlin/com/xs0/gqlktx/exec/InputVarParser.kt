package com.xs0.gqlktx.exec

import com.xs0.gqlktx.schema.Schema
import com.xs0.gqlktx.types.kotlin.GJavaNotNullType
import io.vertx.core.json.JsonObject

import kotlin.reflect.KType

class InputVarParser<CTX>(
        private val queryCtx: CTX,
        private val inputVariables: JsonObject,
        private val schema: Schema<CTX>) {

    @Throws(Exception::class)
    fun getCoercedVar(jsonValue: Any?, type: KType): Any? {
        val javaType = schema.getJavaType(type)

        return if (jsonValue == null) {
            if (javaType.isNullAllowed()) {
                null
            } else {
                throw IllegalArgumentException("Forbidden null")
            }
        } else {
            javaType.getFromJson(jsonValue, this)
        }
    }

    fun isNotNullType(type: KType): Boolean {
        return schema.getJavaType(type) is GJavaNotNullType<*>
    }
}
