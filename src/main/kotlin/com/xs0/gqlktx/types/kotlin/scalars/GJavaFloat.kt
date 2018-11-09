package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import kotlin.reflect.KType

class GJavaFloat<CTX>(type: KType, gqlType: GType) : GJavaScalarLikeType<CTX>(type, gqlType) {
    override fun getFromJson(value: Any, inputVarParser: InputVarParser<CTX>): Float {
        val d: Double = when (value) {
            is Double -> value
            is Number -> value.toDouble()
            else -> throw ValidationException("Expected a number, but got something else")
        }

        if (d.isNaN())
            throw ValidationException("NaN not supported")

        if (Math.abs(d) > java.lang.Float.MAX_VALUE)
            throw ValidationException("Value outside of range")

        return d.toFloat()
    }
}
