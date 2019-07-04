package com.xs0.gqlktx.types.kotlin

import com.xs0.gqlktx.FieldGetter
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GObjectType
import kotlin.reflect.KType

class GJavaObjectType<CTX>(type: KType, gqlType: GObjectType, val fields: Map<String, FieldGetter<CTX>>) : GJavaType<CTX>(type, gqlType) {

    override val gqlType: GObjectType
        get() = super.gqlType as GObjectType

    override fun checkUsage(isInput: Boolean) {
        if (isInput)
            throw IllegalStateException("$type is used as both input and output")
    }

    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): Any {
        throw UnsupportedOperationException()
    }
}
