package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import kotlin.reflect.KType

import java.util.UUID

class GJavaUUID<CTX>(type: KType, gqlType: GType) : GJavaScalarLikeType<CTX>(type, gqlType) {
    override fun getFromJson(value: Any, inputVarParser: InputVarParser<CTX>): UUID {
        val string: String
        if (value is CharSequence) {
            string = value.toString()
        } else {
            throw ValidationException("Expected a string for UUID, but got something else")
        }

        try {
            return UUID.fromString(string)
        } catch (e: IllegalArgumentException) {
            throw ValidationException(e.message)
        }
    }

    override fun toJson(result: Any): Any {
        return result.toString()
    }
}
