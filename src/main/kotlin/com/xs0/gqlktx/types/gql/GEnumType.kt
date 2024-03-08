package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.QueryException
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.dom.ValueEnum
import com.xs0.gqlktx.dom.ValueString
import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.schema.intro.GqlIntroEnumValue

import java.util.ArrayList

class GEnumType(name: String, val enumValues: List<GqlIntroEnumValue>, description: String?) : GValueType(name, description) {
    val stringValues = enumValues.map { it.name }

    override val kind: TypeKind
        get() = TypeKind.ENUM

    override fun coerceValue(raw: Value): ValueEnum {
        if (raw is ValueEnum) {
            check(raw.value)
            return raw
        }

        if (raw is ValueString) {
            check(raw.value)
            return ValueEnum(raw.value)
        }

        throw QueryException("Expected an enum value (String)")
    }

    private fun check(string: String): String {
        if (stringValues.contains(string))
            return string

        throw QueryException("Invalid enum value ($string). The possibilities are: $stringValues")
    }

    override fun toString(sb: StringBuilder) {
        sb.append("enum ").append(name).append(" {\n")
        for (`val` in stringValues)
            sb.append("  ").append(`val`).append("\n")
        sb.append("}\n")
    }

    fun getValuesForIntrospection(includeDeprecated: Boolean): List<GqlIntroEnumValue> {
        if (includeDeprecated) {
            return enumValues
        } else {
            return enumValues.filter { !it.isDeprecated }
        }
    }
}
