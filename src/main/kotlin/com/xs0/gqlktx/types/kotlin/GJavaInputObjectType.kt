package com.xs0.gqlktx.types.kotlin

import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.schema.builder.InputMethodInfo
import com.xs0.gqlktx.types.gql.GInputObjType
import io.vertx.core.json.JsonObject
import kotlin.reflect.KType

class GJavaInputObjectType<CTX>(type: KType, gqlType: GInputObjType, private val fields: Map<String, InputMethodInfo<CTX>>) : GJavaType<CTX>(type, gqlType) {

    override fun checkUsage(isInput: Boolean) {
        if (!isInput)
            throw IllegalStateException(type.toString() + " is used as both input and output")
    }

    override fun getFromJson(value: Any, inputVarParser: InputVarParser<CTX>): Any {
        val input: JsonObject
        if (value == null) {
            input = JsonObject()
        } else if (value is JsonObject) {
            input = value
        } else {
            throw ValidationException("Expected a JSON object, but got something else")
        }

        val result = type.newInstance()

        for ((fieldName, setter) in fields) {

            if (!input.containsKey(fieldName)) {
                if (inputVarParser.isNotNullType(setter.type)) {
                    throw IllegalStateException("Missing value for " + fieldName) // which should've been caught in validation
                } else {
                    continue
                }
            }

            val fieldValue: Any?
            val `val` = input.getValue(fieldName)
            if (`val` != null) {
                fieldValue = inputVarParser.getCoercedVar(`val`, setter.type)
            } else {
                fieldValue = null
            }

            if (fieldValue == null && inputVarParser.isNotNullType(setter.type))
                throw IllegalStateException("Missing value for " + fieldName) // which should've been caught in validation

            setter.execute(inputVarParser.queryCtx, result, fieldValue)
        }

        return result
    }
}
