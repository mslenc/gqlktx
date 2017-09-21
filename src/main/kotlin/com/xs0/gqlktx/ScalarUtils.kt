package com.xs0.gqlktx

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

object ScalarUtils {
    fun validateInteger(value: Any): Int {
        return if (value is Number) {
            validateInteger(value)
        } else {
            throw ValidationException("Expected an integer value")
        }
    }

    fun validateInteger(num: Number): Int {
        if (num is Int)
            return num

        if (num.toInt().toDouble() != num.toDouble())
        // 32-bit ints fit exactly in double
            throw ValidationException("Expected an integer value, but it has a fractional part and/or is out of range")

        return num.toInt()
    }

    fun validateLong(value: Any): Long {
        return if (value is Number) {
            validateLong(value)
        } else {
            throw ValidationException("Expected a long value")
        }
    }

    fun validateLong(num: Number): Long {
        if (num is Long)
            return num

        if (num is Float || num is Double) {
            val dval = num.toDouble()
            if (dval < java.lang.Long.MIN_VALUE || dval > java.lang.Long.MAX_VALUE)
                throw ValidationException("Long value out of range")
            if (Math.ceil(dval) != Math.floor(dval))
                throw ValidationException("Long value can't have fractional digits")
            return dval.toLong()
        }

        return num.toLong() // hopefully we caught all other common types (byte, short, int, AtomicInteger, AtomicLong)
    }

    fun validateString(value: Any): String {
        if (value is CharSequence)
            return value.toString()

        throw ValidationException("Expected a String value")
    }

    fun validateID(str: Any): String {
        if (str is CharSequence) {
            if (str.isEmpty())
                throw ValidationException("An ID can't be empty")

            return str.toString()
        }

        throw ValidationException("Expected an ID (String) value")
    }

    fun validateFloat(value: Any): Double {
        return if (value is Number) {
            validateFloat(value)
        } else {
            throw ValidationException("Expected a Float value")
        }
    }

    fun validateFloat(num: Number): Double {
        val d = num.toDouble()

        if (d.isNaN())
            throw ValidationException("NaN encountered")
        if (d.isInfinite())
            throw ValidationException("Infinity encountered")

        return d
    }

    fun validateBoolean(value: Any): Boolean {
        if (value is Boolean)
            return value

        throw ValidationException("Expected a Boolean value")
    }

    fun validateBytes(str: Any): String {
        if (str is String) {
            val err = validateBase64(str)
            if (err != null)
                throw ValidationException("Not a valid base-64 encoded value: " + err)
            return str
        }

        throw ValidationException("Expected a Bytes (base-64 encoded String) value")
    }

    val DATE_PATTERN = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
    fun validateDate(str: Any): String {
        if (str is CharSequence && DATE_PATTERN.matcher(str).matches())
            return str.toString()

        throw ValidationException("Expected a valid date string in format \"YYYY-MM-DD\"")
    }

    val DATETIME_PATTERN = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}$")
    fun validateDateTime(str: Any): String {
        if (str is CharSequence && DATETIME_PATTERN.matcher(str).matches())
            return str.toString()

        throw ValidationException("Expected a valid datetime string in format \"YYYY-MM-DDThh:mm:ss\"")
    }
}
