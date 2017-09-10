package com.xs0.gqlktx.dom

import com.xs0.gqlktx.parser.Token
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

abstract class ValueOrVar {
    override fun toString(): String = StringBuilder().also { toString(it) }.toString()
    abstract fun toString(sb: StringBuilder)
}

class Variable(name: Token<String>) : ValueOrVar() {
    val name: String = name.value

    override fun toString(sb: StringBuilder) {
        sb.append('$').append(name)
    }
}

abstract class Value : ValueOrVar() {
    abstract fun toJson(): Any?
}

abstract class ValueScalar<out T: Any>
protected constructor(protected val token: Token<*>, val value: T) : Value() {

    override fun toJson(): Any {
        return value
    }

    override fun toString(sb: StringBuilder) {
        sb.append(value)
    }
}

class ValueBool(token: Token<String>, value: Boolean) : ValueScalar<Boolean>(token, value)
class ValueInt(token: Token<Int>) : ValueScalar<Int>(token, token.value)
class ValueLong(token: Token<Long>) : ValueScalar<Long>(token, token.value)
class ValueFloat(token: Token<Double>) : ValueScalar<Double>(token, token.value)

class ValueString(token: Token<String>) : ValueScalar<String>(token, token.value) {
    override fun toString(sb: StringBuilder) {
        sb.append('"').append(value.replace("\"", "\\\"")).append('"')
    }
}

class ValueEnum(token: Token<String>) : ValueScalar<String>(token, token.value) {
    override fun toString(sb: StringBuilder) {
        sb.append(value)
    }
}

class ValueNull(token: Token<Unit>) : ValueScalar<Unit>(token, Unit) {
    override fun toString(sb: StringBuilder) {
        sb.append("null")
    }
}

class ValueObject(val elements: Map<String, ValueOrVar>) : Value() {
    override fun toJson(): JsonObject {
        val res = JsonObject()
        for ((key, value) in elements) {
            if (value is Value) {
                res.put(key, value.toJson())
            } else {
                // this should already be prevented by the parser
                throw Error("Variable in default value")
            }
        }
        return res
    }

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
    override fun toJson(): JsonArray {
        val res = JsonArray()
        for (value in elements) {
            if (value is Value) {
                val elVal = value.toJson()
                if (elVal == null) {
                    res.addNull()
                } else {
                    res.add(elVal)
                }
            } else {
                // this should already be prevented by the parser
                throw Error("Variable in default value")
            }
        }
        return res
    }

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
