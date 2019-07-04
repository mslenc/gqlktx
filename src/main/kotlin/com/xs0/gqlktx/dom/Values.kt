package com.xs0.gqlktx.dom

import com.xs0.gqlktx.parser.Token

sealed class ValueOrVar {
    override fun toString(): String = StringBuilder().also { toString(it) }.toString()
    abstract fun toString(sb: StringBuilder)
}

class Variable(val token: Token<String>) : ValueOrVar() {
    val name = token.value

    override fun toString(sb: StringBuilder) {
        sb.append('$').append(name)
    }
}

sealed class ValueOrNull : ValueOrVar()

class ValueNull(val token: Token<String>? = null) : ValueOrNull() {
    override fun toString(sb: StringBuilder) {
        sb.append("null")
    }
}

sealed class Value : ValueOrNull()

sealed class ValueScalar<out T: Any>(val value: T, internal val token: Token<*>? = null) : Value() {
    override fun toString(sb: StringBuilder) {
        sb.append(value)
    }
}

class ValueBool(value: Boolean, token: Token<String>? = null) : ValueScalar<Boolean>(value, token)
class ValueNumber(strRep: String, token: Token<String>? = null) : ValueScalar<String>(strRep, token)

class ValueString(value: String, token: Token<String>? = null) : ValueScalar<String>(value, token) {
    override fun toString(sb: StringBuilder) {
        sb.append('"').append(value.replace("\"", "\\\"")).append('"')
    }
}

class ValueEnum(value: String, token: Token<String>? = null) : ValueScalar<String>(value, token) {
    override fun toString(sb: StringBuilder) {
        sb.append(value)
    }
}


class ValueObject(val elements: Map<String, ValueOrVar>) : Value() {
    override fun toString(sb: StringBuilder) {
        sb.append("{ ")
        var first = true
        for ((key, value) in elements) {
            if (first) {
                first = false
            } else {
                sb.append(", ")
            }
            sb.append(key).append(": ")
            value.toString(sb)
        }
        sb.append(if (first) "}" else " }")
    }
}

class ValueList(val elements: List<ValueOrVar>) : Value() {
    override fun toString(sb: StringBuilder) {
        sb.append("[ ")
        var first = true
        for (value in elements) {
            if (first) {
                first = false
            } else {
                sb.append(", ")
            }
            value.toString(sb)
        }
        sb.append(if (first) "]" else " ]")
    }
}
