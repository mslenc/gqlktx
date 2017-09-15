package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.schema.intro.GqlIntroField
import com.xs0.gqlktx.schema.intro.GqlIntroInputValue

import java.util.ArrayList

abstract class GFieldedType protected constructor(
        name: String,
        val fields: Map<String, GField>) // note that fields are populated later not at construction time
    : GBaseType(name) {

    override val validAsQueryFieldType: Boolean
        get() {
            return kind == TypeKind.INPUT_OBJECT
        }

    protected fun dumpFieldsToString(sb: StringBuilder) {
        for ((_, value) in fields) {
            sb.append("  ")
            value.toString(sb)
            sb.append("\n")
        }
    }

    fun getFieldsForIntrospection(includeDeprecated: Boolean): List<GqlIntroField>? {
        if (kind == TypeKind.INPUT_OBJECT)
            return null

        val res = ArrayList<GqlIntroField>(fields.size)
        for ((_, value) in fields)
            res.add(GqlIntroField(value))

        return res
    }

    val inputFieldsForIntrospection: List<GqlIntroInputValue>?
        get() {
            if (kind !== TypeKind.INPUT_OBJECT)
                return null

            val res = ArrayList<GqlIntroInputValue>(fields.size)
            for ((_, value) in fields)
                res.add(GqlIntroInputValue(value))

            return res
        }
}
