package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ScalarCoercion
import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.dom.ValueNumber
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import java.math.BigDecimal
import kotlin.reflect.KType

class GJavaBigDecimal<CTX>(type: KType, gqlType: GType) : GJavaScalarLikeType<CTX>(type, gqlType) {
    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): BigDecimal {
        if (value !is ValueNumber)
            throw ValidationException("Expected a number, but got something else")

        return value.value.toBigDecimalOrNull() ?: throw ValidationException("Expected a number, but got something else")
    }

    override fun toJson(result: Any, coercion: ScalarCoercion): Any {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON ->
                (result as BigDecimal).toDouble()

            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET,
            ScalarCoercion.NONE ->
                result
        }
    }
}