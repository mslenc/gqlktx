package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.dom.Value

class GInputField(val name: String, val type: GType, val defaultValue: Value?, val description: String?) {

    override fun toString(): String {
        val sb = StringBuilder()
        toString(sb)
        return sb.toString()
    }

    fun toString(sb: StringBuilder) {
        sb.append(name)
        sb.append(": ").append(type.gqlTypeString)
        if (defaultValue != null)
            sb.append(" = ").append(defaultValue)
    }
}
