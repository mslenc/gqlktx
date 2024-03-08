package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.schema.intro.GqlIntroInputValue

class GArgument(val name: String, val type: GType, val defaultValue: Value?, val description: String?) {
    private val introspector: GqlIntroInputValue = GqlIntroInputValue(this)

    fun introspector(): GqlIntroInputValue {
        return introspector
    }

    override fun toString(): String {
        val sb = StringBuilder()
        toString(sb)
        return sb.toString()
    }

    fun toString(sb: StringBuilder) {
        sb.append(name).append(": ").append(type.gqlTypeString)
        if (defaultValue != null) {
            sb.append(" = ")
            defaultValue.toString(sb)
        }
    }
}
