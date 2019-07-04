package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ScalarCoercion
import com.xs0.gqlktx.ScalarUtils
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import kotlin.reflect.KType

class GJavaLong<CTX>(type: KType, gqlType: GType) : GJavaScalarLikeType<CTX>(type, gqlType) {
    init {
        if (type.classifier != Long::class)
            throw IllegalArgumentException("Not a long type: ${type.classifier}")
    }

    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): Long {
        return ScalarUtils.validateLong(value)
    }

    override fun toJson(result: Any, coercion: ScalarCoercion): Any {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON ->
                (result as Number).toDouble()

            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET,
            ScalarCoercion.NONE ->
                result
        }
    }
}
