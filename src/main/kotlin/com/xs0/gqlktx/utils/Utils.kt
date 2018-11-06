package com.xs0.gqlktx.utils

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*


private val toBase64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray()
private val fromBase64: IntArray = run {
    val fromBase64 = IntArray('z'.toInt() + 1, { -1 })
    for (i in 0..63)
        fromBase64[toBase64[i].toInt()] = i
    fromBase64['='.toInt()] = -2
    return@run fromBase64
}

fun base64EncodeULong(value: Long): String {
    val skipBytes = Math.min(63, java.lang.Long.numberOfLeadingZeros(value)).ushr(3)
    val buff = ByteArray(8 - skipBytes)

    for (i in skipBytes..7) {
        val shift = 8 * (7 - i)
        buff[i - skipBytes] = value.ushr(shift).toByte()
    }

    return Base64.getUrlEncoder().withoutPadding().encodeToString(buff)
}

fun base64DecodeULong(encoded: String): Long {
    val bytes = Base64.getUrlDecoder().decode(encoded)

    if (bytes.isEmpty() || bytes.size > 8)
        throw IllegalArgumentException("Invalid length")

    var result: Long = 0

    for (b in bytes) {
        result = result shl 8
        result = result or (b.toInt() and 255).toLong()
    }

    return result
}

fun transformForJson(value: Any?): Any? {
    return when (value) {
        null -> null
        is Enum<*> -> value.name
        is ByteArray -> Base64.getUrlEncoder().withoutPadding().encodeToString(value)
        is Instant -> DateTimeFormatter.ISO_INSTANT.format(value)
        else -> value
    }
}