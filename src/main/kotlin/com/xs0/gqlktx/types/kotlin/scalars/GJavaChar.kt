package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ScalarCoercion
import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import kotlin.reflect.KType

class GJavaChar<CTX>(type: KType, gqlType: GType) : GJavaScalarLikeType<CTX>(type, gqlType) {
    init {
        if (type.classifier != Char::class)
            throw IllegalArgumentException("Not a char type: $type")
    }

    override fun getFromJson(value: Any, inputVarParser: InputVarParser<CTX>): Char {
        if (value !is CharSequence) {
            throw ValidationException("Expected a character (one char string), but got something else")
        }

        // this would be far more i18n-ready, if we took a full codepoint.. but Java char is
        // not a full codepoint, so things wouldn't work..
        if (value.length > 1)
            throw ValidationException("Expected a character, but received a multi-char string")
        if (value.length < 1)
            throw ValidationException("Expected a character, but received an empty string")

        return value[0]
    }

    override fun toJson(result: Any, coercion: ScalarCoercion): Any {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET ->
                result.toString()

            ScalarCoercion.NONE ->
                result
        }
    }
}
