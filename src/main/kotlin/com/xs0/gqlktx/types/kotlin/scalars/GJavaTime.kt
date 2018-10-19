package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import java.time.LocalTime
import kotlin.reflect.KType

class GJavaTime<CTX>(type: KType, gqlType: GType) : GJavaScalarLikeType<CTX>(type, gqlType) {
    override fun getFromJson(value: Any, inputVarParser: InputVarParser<CTX>): LocalTime {
        val string = value as? CharSequence

        if (string == null)
            throw ValidationException("Expected a string for Time, but got something else")

        try {
            return LocalTime.parse(string)
        } catch (e: Exception) {
            throw ValidationException(e.message ?: "Couldn't parse as LocalTime")
        }
    }

    override fun toJson(result: Any): Any {
        return result.toString()
    }
}
