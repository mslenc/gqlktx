package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ScalarCoercion
import com.xs0.gqlktx.ScalarUtils
import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import kotlin.math.abs
import kotlin.reflect.KType

class GJavaFloat<CTX>(type: KType, gqlType: GType) : GJavaScalarLikeType<CTX>(type, gqlType) {
    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): Float {
        val d = ScalarUtils.validateFloat(value)

        if (d.isNaN())
            throw ValidationException("NaN not supported")

        if (abs(d) > Float.MAX_VALUE)
            throw ValidationException("Value outside of range")

        return d.toFloat()
    }

    override fun toJson(result: Any, coercion: ScalarCoercion): Any {
        return when (coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET ->
                (result as Number).toDouble()

            ScalarCoercion.NONE ->
                result
        }
    }
}
