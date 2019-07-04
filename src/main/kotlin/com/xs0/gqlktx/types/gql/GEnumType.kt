package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.QueryException
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.dom.ValueEnum
import com.xs0.gqlktx.dom.ValueString
import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.schema.intro.GqlIntroEnumValue

import java.util.ArrayList

class GEnumType(name: String, val values: Set<String>) : GValueType(name) {

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
        if (values.contains(string))
            return string

        throw QueryException("Invalid enum value ($string). The possibilities are: $values")
    }

    override fun toString(sb: StringBuilder) {
        sb.append("enum ").append(name).append(" {\n")
        for (`val` in values)
            sb.append("  ").append(`val`).append("\n")
        sb.append("}\n")
    }

    fun getValuesForIntrospection(includeDeprecated: Boolean): List<GqlIntroEnumValue> {
        val res = ArrayList<GqlIntroEnumValue>()
        for (value in values)
            res.add(GqlIntroEnumValue(value))
        return res
    }
}
