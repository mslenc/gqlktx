package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GEnumType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSuperclassOf

class GJavaStandardEnum<CTX, ENUM : Enum<ENUM>>(type: KType, gqlType: GEnumType, private val valuesByName: Map<String, ENUM>) : GJavaScalarLikeType<CTX>(type, gqlType) {
    init {
        if (!Enum::class.isSuperclassOf(type.classifier as? KClass<*> ?: String::class))
            throw IllegalArgumentException("Not an enum type: " + type)
    }

    override fun getFromJson(value: Any, inputVarParser: InputVarParser<CTX>): ENUM {
        val s: String
        if (value is String || value is CharSequence) {
            s = value.toString()
        } else {
            throw ValidationException("Expected a string for enum, but got something else")
        }

        return valuesByName[s] ?: throw ValidationException("Unrecognized value " + s + " for enum " + gqlType.gqlTypeString)
    }

    override fun toJson(result: Any): String {
        return (result as Enum<*>).name
    }
}
