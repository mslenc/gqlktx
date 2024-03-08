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

sealed class Value : ValueOrNull() {
    abstract fun codeGen(): String
}

sealed class ValueScalar<out T: Any>(val value: T, val token: Token<*>? = null) : Value() {
    override fun toString(sb: StringBuilder) {
        sb.append(value)
    }
}

class ValueBool(value: Boolean, token: Token<String>? = null) : ValueScalar<Boolean>(value, token) {
    override fun codeGen(): String {
        return "ValueBool($value)"
    }
}

class ValueNumber(strRep: String, token: Token<String>? = null) : ValueScalar<String>(strRep, token) {
    override fun codeGen(): String {
        return "ValueNumber(" + value.javaEscape() + ")"
    }
}

class ValueString(value: String, token: Token<String>? = null) : ValueScalar<String>(value, token) {
    override fun toString(sb: StringBuilder) {
        sb.append(value.javaEscape())
    }

    override fun codeGen(): String {
        return "ValueString(" + value.javaEscape() + ")"
    }
}

class ValueEnum(value: String, token: Token<String>? = null) : ValueScalar<String>(value, token) {
    override fun toString(sb: StringBuilder) {
        sb.append(value)
    }

    override fun codeGen(): String {
        return "ValueEnum(" + value.javaEscape() + ")"
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

    override fun codeGen(): String {
        val sb = StringBuilder()
        sb.append("ValueObject(mapOf(")
        var first = true
        for ((name, value) in elements) {
            if (first) {
                first = false
            } else {
                sb.append(", ")
            }

            when (value) {
                is ValueNull -> sb.append(name.javaEscape()).append(" to ValueNull()")
                is Value -> sb.append(name.javaEscape()).append(" to ").append(value.codeGen())
                else -> throw IllegalStateException("Can't codegen variables")
            }
        }
        sb.append("))")

        return sb.toString()
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

    override fun codeGen(): String {
        val sb = StringBuilder()
        sb.append("ValueList(listOf(")
        var first = true
        for (value in elements) {
            if (first) {
                first = false
            } else {
                sb.append(", ")
            }

            when (value) {
                is ValueNull -> sb.append("ValueNull()")
                is Value -> sb.append(value.codeGen())
                else -> throw IllegalStateException("Can't codegen variables")
            }
        }
        sb.append("))")

        return sb.toString()
    }
}


fun String.javaEscape(): String {
    val sb = StringBuilder()
    sb.append('"').append(this.replace("\"", "\\\"")).append('"')
    return sb.toString()
}
