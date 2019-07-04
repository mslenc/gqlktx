package com.xs0.gqlktx.types.kotlin

import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType

import kotlin.reflect.KType

abstract class GJavaType<CTX>(val type: KType, open val gqlType: GType) {
    open fun isNullAllowed(): Boolean {
        return type.isMarkedNullable
    }

    abstract fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): Any

    abstract fun checkUsage(isInput: Boolean)
}
