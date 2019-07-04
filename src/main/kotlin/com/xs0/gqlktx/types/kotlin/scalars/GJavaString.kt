package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ScalarCoercion
import com.xs0.gqlktx.ScalarUtils
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import kotlin.reflect.KType

class GJavaString<CTX>(type: KType, gqlType: GType) : GJavaScalarLikeType<CTX>(type, gqlType) {
    init {
        if (type.classifier != String::class)
            throw IllegalArgumentException("Not a String class: ${type.classifier}")
    }

    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): String {
        return ScalarUtils.validateString(value)
    }

    override fun toJson(result: Any, coercion: ScalarCoercion): Any {
        return result
    }
}
