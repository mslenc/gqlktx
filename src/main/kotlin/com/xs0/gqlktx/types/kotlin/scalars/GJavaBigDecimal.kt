package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ScalarCoercion
import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.reflect.KType

class GJavaBigDecimal<CTX>(type: KType, gqlType: GType) : GJavaScalarLikeType<CTX>(type, gqlType) {
    override fun getFromJson(value: Any, inputVarParser: InputVarParser<CTX>): BigDecimal {
        val double: Double
        if (value is Number) {
            double = value.toDouble()
        } else {
            throw ValidationException("Expected a number, but got something else")
        }

        val bd = BigDecimal(double)
        if (bd.scale() > 6) {
            return bd.setScale(6, RoundingMode.HALF_UP)
        } else {
            return bd
        }
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