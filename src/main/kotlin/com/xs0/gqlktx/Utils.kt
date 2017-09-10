package com.xs0.gqlktx

import java.beans.Introspector
import java.lang.reflect.Array
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*
import java.util.concurrent.ConcurrentHashMap
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

fun setterName(methodName: String): String? {
    val suffix: String
    if (methodName.startsWith("set")) {
        suffix = methodName.substring(3)
    } else {
        return null
    }

    return if (suffix.isEmpty()) null else Introspector.decapitalize(suffix)

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

fun getNullValue(type: Type): Any? {
    if (type is Class<*>) {
        if (type.isPrimitive) {
            if (type == Void.TYPE)
                throw IllegalArgumentException("There is no null value for void")

            if (type == Int::class.javaPrimitiveType)
                return 0
            if (type == Boolean::class.javaPrimitiveType)
                return false
            if (type == Double::class.javaPrimitiveType)
                return 0.0
            if (type == Long::class.javaPrimitiveType)
                return 0L
            if (type == Byte::class.javaPrimitiveType)
                return 0.toByte()
            if (type == Short::class.javaPrimitiveType)
                return 0.toShort()
            if (type == Float::class.javaPrimitiveType)
                return 0.0f
            if (type == Char::class.javaPrimitiveType)
                return '\u0000'

            throw Error("A new primitive type seems to have been introduced")
        }
    }

    return null
}

interface GqlValueValidator {
    fun validateAndNormalize(incoming: Any): Any
}

private val toBase64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray()

private var fromBase64: IntArray = run {
    val fromBase64: IntArray = IntArray('z'.toInt() + 1)
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

    return if (pos < len) "Input string has incorrect ending byte at " + pos else null

}

private val arrayClasses = ConcurrentHashMap<Class<*>,Class<*>>()

fun classOfArray(klass: Class<*>): Class<*> {
    return arrayClasses.computeIfAbsent(klass) { k -> Array.newInstance(k, 0).javaClass }
}

fun toConcreteType(type: Type): Class<*> {
    if (type is Class<*>)
        return type

    if (type is ParameterizedType)
        return toConcreteType(type.rawType)

    if (type is GenericArrayType)
        return classOfArray(toConcreteType(type.genericComponentType))

    throw IllegalStateException("Can't convert $type to a concrete class")
}

fun <K, V> appendLists(target: MutableMap<K, MutableList<V>>, source: MutableMap<K, MutableList<V>>) {
    for ((key, value) in source) {
        target.merge(key, value) { targetList, sourceList ->
            if (targetList == null) {
                sourceList
            } else {
                targetList.addAll(sourceList)
                targetList
            }
        }
    }
}