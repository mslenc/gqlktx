package com.xs0.gqlktx.utils

import java.util.*
import kotlin.math.min

fun base64EncodeULong(value: Long): String {
    val skipBytes = min(63, java.lang.Long.numberOfLeadingZeros(value)).ushr(3)
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