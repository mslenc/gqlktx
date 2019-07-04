package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.schema.intro.GqlIntroInputValue

import java.util.ArrayList

class GField(val name: String, val type: GType, val arguments: Map<String, GArgument>) {

    override fun toString(): String {
        val sb = StringBuilder()
        toString(sb)
        return sb.toString()
    }

    fun toString(sb: StringBuilder) {
        sb.append(name)
        if (arguments.isNotEmpty()) {
            sb.append("(")
            var first = true
            for (arg in arguments.values) {
                if (first) {
                    first = false
                } else {
                    sb.append(", ")
                }
                arg.toString(sb)
            }
            sb.append(")")
        }
        sb.append(": ").append(type.gqlTypeString)
    }

    // TODO
    val description: String?
        get() = null

    val argumentsForIntrospection: List<GqlIntroInputValue> by lazy {
        val res = ArrayList<GqlIntroInputValue>(arguments.size)
        for (arg in arguments.values) {
            res.add(arg.introspector())
        }
        res
    }
}
