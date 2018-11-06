package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType

import java.util.Base64
import kotlin.reflect.KType

class GJavaByteArray<CTX>(type: KType, gqlType: GType) : GJavaScalarLikeType<CTX>(type, gqlType) {
    init {
        if (type.classifier != ByteArray::class)
            throw IllegalArgumentException("Expected ByteArray type")
    }

    override fun getFromJson(value: Any, inputVarParser: InputVarParser<CTX>): ByteArray {
        if (value !is String)
            throw ValidationException("Bytes values in JSON must be encoded as base64 strings")

        try {
            return Base64.getUrlDecoder().decode(value)
        } catch (e: IllegalArgumentException) {
            throw ValidationException("Couldn't base64-decode value: " + e.message)
        }
    }
}
