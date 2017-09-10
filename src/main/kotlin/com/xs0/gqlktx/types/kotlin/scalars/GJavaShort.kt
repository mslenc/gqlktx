package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import kotlin.reflect.KType

class GJavaShort<CTX>(type: KType, gqlType: GType) : GJavaScalarLikeType<CTX>(type, gqlType) {
    init {
        if (type.classifier != Short::class)
            throw IllegalArgumentException("Not a short class ${type.classifier}")
    }

    override fun getFromJson(value: Any, inputVarParser: InputVarParser<CTX>): Short {
        if (value is Short)
            return value

        if (value !is Number)
            throw ValidationException("Expected an integer value, but got something else instead")

        if (value.toInt().toDouble() != value.toDouble())
        // 32-bit ints fit exactly in double
            throw ValidationException("Expected an integer value, but it has a fractional part and/or is out of range")

        val intVal = value.toInt()
        if (intVal < java.lang.Short.MIN_VALUE || intVal > java.lang.Short.MAX_VALUE)
            throw ValidationException("Value is out of range")

        return intVal.toShort()
    }

    override fun toJson(result: Any): Any {
        return (result as Number).toInt()
    }
}
