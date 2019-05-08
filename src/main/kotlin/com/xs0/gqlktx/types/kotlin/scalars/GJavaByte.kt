package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ScalarCoercion
import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import kotlin.reflect.KType

class GJavaByte<CTX>(type: KType, gqlType: GType) : GJavaScalarLikeType<CTX>(type, gqlType) {
    override fun getFromJson(value: Any, inputVarParser: InputVarParser<CTX>): Byte {
        if (value is Byte)
            return value

        if (value !is Number)
            throw ValidationException("Expected an integer value, but got something else instead")

        if (value.toInt().toDouble() != value.toDouble())
        // 32-bit ints fit exactly in double
            throw ValidationException("Expected an integer value, but it has a fractional part and/or is out of range")

        val intVal = value.toInt()
        if (intVal < Byte.MIN_VALUE || intVal > Byte.MAX_VALUE)
            throw ValidationException("Value is out of range")

        return intVal.toByte()
    }

    override fun toJson(result: Any, coercion: ScalarCoercion): Any {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON ->
                (result as Number).toDouble()

            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET ->
                (result as Number).toInt()

            ScalarCoercion.NONE ->
                result
        }
    }
}
