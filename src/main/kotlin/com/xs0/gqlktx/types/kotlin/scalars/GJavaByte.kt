package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ScalarCoercion
import com.xs0.gqlktx.ScalarUtils
import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import kotlin.reflect.KType

class GJavaByte<CTX>(type: KType, gqlType: GType) : GJavaScalarLikeType<CTX>(type, gqlType) {
    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): Byte {
        val intVal = ScalarUtils.validateInteger(value)

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
