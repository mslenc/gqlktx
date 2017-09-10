package com.xs0.gqlktx.types.kotlin

import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GNotNullType
import kotlin.reflect.KType
class GJavaNotNullType<CTX>(type: KType, val innerType: GJavaType<CTX>, gqlType: GNotNullType) : GJavaType<CTX>(type, gqlType) {
    override fun isNullAllowed(): Boolean {
        return false
    }

    override fun getFromJson(value: Any, inputVarParser: InputVarParser<CTX>): Any {
        return innerType.getFromJson(value, inputVarParser)
    }

    override fun checkUsage(isInput: Boolean) {
        innerType.checkUsage(isInput)
    }
}
