package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ScalarCoercion
import com.xs0.gqlktx.ScalarUtils
import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.KType

class GJavaDateTime<CTX>(type: KType, gqlType: GType) : GJavaScalarLikeType<CTX>(type, gqlType) {
    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): LocalDateTime {
        val string = ScalarUtils.validateString(value)

        try {
            return LocalDateTime.parse(string, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (e: Exception) {
            throw ValidationException(e.message ?: "Couldn't parse as LocalDate")
        }
    }

    override fun toJson(result: Any, coercion: ScalarCoercion): Any {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON ->
                DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(result as LocalDateTime)

            ScalarCoercion.SPREADSHEET,
            ScalarCoercion.NONE ->
                result
        }
    }
}