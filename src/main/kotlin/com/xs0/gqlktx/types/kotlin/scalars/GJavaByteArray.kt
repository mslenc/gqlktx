package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ScalarCoercion
import com.xs0.gqlktx.ScalarUtils
import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.dom.Value
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

    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): ByteArray {
        val string = ScalarUtils.validateString(value)

        try {
            return Base64.getUrlDecoder().decode(string)
        } catch (e: IllegalArgumentException) {
            throw ValidationException("Couldn't base64-decode value: " + e.message)
        }
    }

    override fun toJson(result: Any, coercion: ScalarCoercion): Any {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET ->
                Base64.getUrlEncoder().withoutPadding().encodeToString(result as ByteArray)

            ScalarCoercion.NONE ->
                result
        }
    }
}
