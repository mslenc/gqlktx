package com.xs0.gqlktx.types.kotlin

import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GUnionType
import kotlin.reflect.KClass
import kotlin.reflect.KType

class GJavaUnionType<CTX>(type: KType, gqlType: GUnionType, implementations: Array<KClass<*>>) : GJavaImplementableType<CTX>(type, gqlType, implementations) {

    override fun checkUsage(isInput: Boolean) {
        if (isInput)
            throw IllegalStateException("Union $type is used as input")
    }

    @Throws(Exception::class)
    override fun getFromJson(value: Any, inputVarParser: InputVarParser<CTX>): Any {
        throw UnsupportedOperationException()
    }
}
