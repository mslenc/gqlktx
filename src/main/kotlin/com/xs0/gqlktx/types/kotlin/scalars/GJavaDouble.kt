package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ScalarCoercion
import com.xs0.gqlktx.ScalarUtils
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import kotlin.reflect.KType

class GJavaDouble<CTX>(type: KType, gqlType: GType) : GJavaScalarLikeType<CTX>(type, gqlType) {
    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): Double {
        return ScalarUtils.validateFloat(value)
    }

    override fun toJson(result: Any, coercion: ScalarCoercion): Any {
        return result
    }
}
