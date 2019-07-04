package com.xs0.gqlktx.types.kotlin

import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GBaseType
import kotlin.reflect.KClass
import kotlin.reflect.KType

abstract class GJavaImplementableType<CTX> protected constructor(type: KType, gqlType: GBaseType,
    val implementations: Array<KClass<*>>) : GJavaType<CTX>(type, gqlType) {

    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): Any {
        throw UnsupportedOperationException()
    }

    override fun isNullAllowed(): Boolean {
        throw UnsupportedOperationException()
    }
}
