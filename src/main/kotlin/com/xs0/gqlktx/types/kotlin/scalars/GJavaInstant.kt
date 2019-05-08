package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ScalarCoercion
import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.reflect.KType

class GJavaInstant<CTX>(type: KType, gqlType: GType) : GJavaScalarLikeType<CTX>(type, gqlType) {
    override fun getFromJson(value: Any, inputVarParser: InputVarParser<CTX>): Instant {
        val string = value as? CharSequence

        if (string == null)
            throw ValidationException("Expected a string for Instant, but got something else")

        try {
            return Instant.parse(string)
        } catch (e: Exception) {
            throw ValidationException(e.message ?: "Couldn't parse as LocalDate")
        }
    }

    override fun toJson(result: Any, coercion: ScalarCoercion): Any {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON ->
                DateTimeFormatter.ISO_INSTANT.format(result as Instant)

            ScalarCoercion.SPREADSHEET,
            ScalarCoercion.NONE ->
                result
        }
    }
}