package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ScalarCoercion
import com.xs0.gqlktx.ScalarUtils
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import kotlin.reflect.KType

class GJavaBoolean<CTX>(type: KType, gqlType: GType) : GJavaScalarLikeType<CTX>(type, gqlType) {
    init {
        if (type.classifier != Boolean::class)
            throw IllegalArgumentException("Not a boolean type: $type")
    }

    override fun getFromJson(value: Any, inputVarParser: InputVarParser<CTX>): Boolean {
        return ScalarUtils.validateBoolean(value)
    }

    override fun toJson(result: Any, coercion: ScalarCoercion): Any {
        return result
    }
}
