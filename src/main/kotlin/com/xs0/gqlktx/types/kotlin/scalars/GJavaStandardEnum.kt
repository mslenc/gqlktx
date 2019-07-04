package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ScalarCoercion
import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.dom.ValueEnum
import com.xs0.gqlktx.dom.ValueString
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GEnumType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSuperclassOf

class GJavaStandardEnum<CTX>(type: KType, gqlType: GEnumType, private val valuesByName: Map<String, Any>) : GJavaScalarLikeType<CTX>(type, gqlType) {
    init {
        if (!Enum::class.isSuperclassOf(type.classifier as? KClass<*> ?: String::class))
            throw IllegalArgumentException("Not an enum type: $type")
    }

    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): Any {
        // So when we parse GQL, we produce ValueEnum.. but when we get variables from JSON, it becomes just a regular string..
        // Luckily, we can tell the two apart by the presence of the parser token ..
        val s = when (value) {
            is ValueEnum -> value.value
            is ValueString -> if (value.token == null) value.value else throw ValidationException("The value should be an enum keyword, not a string")
            else -> throw ValidationException("Expected a string for enum, but got something else")
        }

        return valuesByName[s] ?: throw ValidationException("Unrecognized value " + s + " for enum " + gqlType.gqlTypeString)
    }

    override fun toJson(result: Any, coercion: ScalarCoercion): Any {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET ->
                (result as Enum<*>).name

            ScalarCoercion.NONE ->
                result
        }
    }
}
