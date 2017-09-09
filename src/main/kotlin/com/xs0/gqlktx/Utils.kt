package com.xs0.gqlktx

import java.beans.Introspector

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