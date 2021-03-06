package com.xs0.gqlktx

import com.xs0.gqlktx.dom.*
import java.beans.Introspector
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf

fun validGraphQLName(name: String?, allowIntrospectionNames: Boolean): Boolean {
    if (name == null || name.isBlank())
        return false

    if (!allowIntrospectionNames && name.startsWith("__"))
        return false

    var i = 0
    val n = name.length
    while (i < n) {
        val c = name[i]
        if (!(c == '_' ||
              c in 'a'..'z' ||
              c in 'A'..'Z' ||
              c in '0'..'9' && i > 0)) {
            return false
        }
        i++
    }

    return when (name) {
        "query", "mutation", "subscription", "true", "false", "null" -> false
        else -> true
    }
}

fun getterName(methodName: String, isBoolean: Boolean): String? {
    val suffix = when {
        isBoolean && methodName.startsWith("is") -> methodName.substring(2)
        methodName.startsWith("get") -> methodName.substring(3)
        else -> return null
    }

    if (suffix.isEmpty())
        return null

    return Introspector.decapitalize(suffix)
}

fun extractTypeParam(source: KType?, vararg types: KClass<*>): KType? {
    var curr = source
    for (type in types) {
        if (curr == null || curr.arguments.size != 1)
            return null

        val classifier = curr.classifier as? KClass<*> ?: return null

        if (!classifier.isSubclassOf(type))
            return null

        curr = curr.arguments[0].type
    }

    return curr
}

private val toBase64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray()

private var fromBase64: IntArray = run {
    val fromBase64 = IntArray('z'.toInt() + 1)
    Arrays.fill(fromBase64, -1)
    for (i in 0..63)
        fromBase64[toBase64[i].toInt()] = i
    fromBase64['='.toInt()] = -2
    fromBase64
}


fun validateBase64(s: String): String? {
    val base64 = fromBase64
    var pos = 0
    val len = s.length
    var shift = 3
    while (pos < len) {
        val c = s[pos++]
        if (c.toInt() >= base64.size)
            return "Invalid character $c encountered"

        val b = base64[c.toInt()]
        if (b == -1)
            return "Invalid character $c encountered"

        if (b == -2) {
            if (shift == 1 && (pos == len || s[pos++] != '=') || shift == 3) {
                return "Input string has wrong 4-byte ending unit"
            }
            break
        }

        if (--shift < 0)
            shift = 3
    }

    if (shift == 2)
        return "Last unit does not have enough valid bits"

    return if (pos < len) "Input string has incorrect ending byte at $pos" else null

}

fun <K, V> appendLists(target: MutableMap<K, MutableList<V>>, source: MutableMap<K, MutableList<V>>) {
    for ((key, value) in source) {
        target.merge(key, value) { targetList, sourceList ->
            targetList.addAll(sourceList)
            targetList
        }
    }
}

fun String?.trimToNull(): String? {
    if (this == null)
        return null
    val trimmed = this.trim()
    if (trimmed.isEmpty())
        return null
    return trimmed
}

fun importVariables(json: Map<String, Any?>): Map<String, ValueOrNull> {
    if (json.isEmpty())
        return emptyMap()

    val result = LinkedHashMap<String, ValueOrNull>()

    for ((key, value) in json.entries) {
        result[key] = importVariable(value)
    }

    return result
}

private fun importVariable(value: Any?): ValueOrNull {
    if (value == null)
        return ValueNull()

    return when (value) {
        is String -> ValueString(value)
        is Number -> ValueNumber(value.toString())
        is Boolean -> ValueBool(value)
        is Map<*,*> -> {
            @Suppress("UNCHECKED_CAST")
            val map = value as Map<String, Any?>
            ValueObject(importVariables(map))
        }
        is Iterable<*> -> {
            ValueList(value.map { importVariable(it) })
        }
        else -> {
            throw IllegalArgumentException("Unknown type of variable encountered")
        }
    }
}