package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import kotlin.reflect.full.starProjectedType

class GJavaCharArray<CTX>(gqlType: GType) : GJavaScalarLikeType<CTX>(CharArray::class.starProjectedType, gqlType) {
    override fun getFromJson(value: Any, inputVarParser: InputVarParser<CTX>): CharArray {
        if (value is CharSequence)
            return value.toString().toCharArray()

        throw ValidationException("Expected a string")
    }

    override fun toJson(result: Any): Any {
        return String(result as CharArray)
    }
}
