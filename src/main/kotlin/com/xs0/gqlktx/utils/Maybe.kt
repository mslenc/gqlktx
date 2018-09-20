package com.xs0.gqlktx.utils

import com.xs0.gqlktx.trimToNull

class Maybe<out T> (val value: T)
class Otherwise(val runOtherwise: Boolean)

val runOtherwise = Otherwise(true)
val skipOtherwise = Otherwise(false)

fun <T: Any> Maybe<T?>?.newValueOr(oldValue: T?): T? {
    return if (this != null) value else oldValue
}

fun <T: Any> Maybe<T?>?.nonNullValueOr(oldValue: T): T {
    return if (this != null && value != null) value else oldValue
}

inline fun <T: Any> Maybe<T?>?.ifSet(block: (T?) -> Unit): Otherwise {
    return if (this != null) {
        block(value)
        skipOtherwise
    } else {
        runOtherwise
    }
}

inline fun <T: Any> Maybe<T?>?.ifNonNull(block: (T) -> Unit): Otherwise {
    return if (this != null && value != null) {
        block(value)
        skipOtherwise
    } else {
        runOtherwise
    }
}

inline infix fun Otherwise.otherwise(block: () -> Unit) {
    if (runOtherwise) {
        block()
    }
}

inline fun <T: Any> Maybe<T?>?.exceptIfUnset(block: () -> Nothing): T? {
    if (this != null) {
        return value
    } else {
        block()
    }
}

inline fun <T: Any> Maybe<T?>?.exceptIfNull(block: () -> Nothing): T {
    if (this != null && value != null) {
        return value
    } else {
        block()
    }
}

inline fun Maybe<String?>?.trimToNull(): String? {
    return if (this != null) {
        value.trimToNull()
    } else {
        null
    }
}