package com.xs0.gqlktx.types.kotlin

import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.utils.Maybe
import kotlin.reflect.KType

class GJavaMaybeType<CTX>(type: KType, val innerType: GJavaType<CTX>, gqlType: GType) : GJavaType<CTX>(type, gqlType) {
    override fun isNullAllowed(): Boolean {
        return true
    }

    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): Any {
        return Maybe(innerType.getFromJson(value, inputVarParser))
    }

    override fun checkUsage(isInput: Boolean) {
        if (!isInput)
            throw IllegalStateException("Maybe is only supported for input (type $type)")

        innerType.checkUsage(isInput)
    }
}
