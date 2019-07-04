package com.xs0.gqlktx

import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.dom.ValueBool
import com.xs0.gqlktx.dom.ValueNumber
import com.xs0.gqlktx.dom.ValueString
import java.time.Instant
import java.util.regex.Pattern
import kotlin.math.ceil
import kotlin.math.floor

object ScalarUtils {
    fun validateInteger(value: Value): Int {
        if (value !is ValueNumber)
            throw ValidationException("Expected an integer value")

        value.value.toIntOrNull()?.let { return it }

        val dval = value.value.toDoubleOrNull() ?: throw ValidationException("Expected an integer value")

        if (dval == dval.toInt().toDouble()) // 32-bit ints fit exactly in double
            throw ValidationException("Expected an integer value, but it has a fractional part and/or is out of range")

        return dval.toInt()
    }

    fun validateShort(value: Value): Short {
        val intVal = validateInteger(value)

        if (intVal !in Short.MIN_VALUE..Short.MAX_VALUE)
            throw ValidationException("Value out of range for short")

        return intVal.toShort()
    }

    fun validateLong(value: Value): Long {
        if (value !is ValueNumber)
            throw ValidationException("Expected a long value")

        value.value.toLongOrNull()?.let { return it }

        val dval = value.value.toDoubleOrNull() ?: throw ValidationException("Expected a long value")

        if (dval < Long.MIN_VALUE || dval > Long.MAX_VALUE)
            throw ValidationException("Long value out of range")

        if (ceil(dval) != floor(dval))
            throw ValidationException("Long value can't have fractional digits")

        return dval.toLong()
    }

    fun validateString(value: Value): String {
        if (value !is ValueString)
            throw ValidationException("Expected a String value")

        return value.value
    }

    fun validateID(value: Value): String {
        val str = validateString(value)

        if (str.isEmpty())
            throw ValidationException("An ID can't be empty")

        return str
    }

    fun validateFloat(value: Value): Double {
        if (value !is ValueNumber)
            throw ValidationException("Expected a Float value")

        val d = value.value.toDoubleOrNull() ?: throw ValidationException("Expected a Float value")

        if (d.isNaN())
            throw ValidationException("NaN encountered")
        if (d.isInfinite())
            throw ValidationException("Infinity encountered")

        return d
    }

    fun validateSingleFloat(value: Value): Float {
        val f = validateFloat(value).toFloat()

        if (f.isInfinite())
            throw ValidationException("Value out of range for float")

        return f
    }

    fun validateBoolean(value: Value): Boolean {
        if (value is ValueBool)
            return value.value

        throw ValidationException("Expected a Boolean value")
    }

    fun validateBytes(value: Value): String {
        val str = validateString(value)

        val err = validateBase64(str)
        if (err != null)
            throw ValidationException("Not a valid base-64 encoded value: $err")

        return str
    }

    private val DATE_PATTERN: Pattern = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
    fun validateDate(value: Value): String {
        val str = validateString(value)

        if (DATE_PATTERN.matcher(str).matches())
            return str

        throw ValidationException("Expected a valid date string in format \"YYYY-MM-DD\"")
    }

    private val TIME_PATTERN: Pattern = Pattern.compile("^[0-9]{2}:[0-9]{2}:[0-9]{2}([.][0-9]+)?$")
    fun validateTime(value: Value): String {
        val str = validateString(value)

        if (TIME_PATTERN.matcher(str).matches())
            return str

        throw ValidationException("Expected a valid time string in format \"HH:MM:SS[.mmm]\"")
    }

    private val DATETIME_PATTERN: Pattern = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}$")
    fun validateDateTime(value: Value): String {
        val str = validateString(value)

        if (DATETIME_PATTERN.matcher(str).matches())
            return str

        throw ValidationException("Expected a valid datetime string in format \"YYYY-MM-DDThh:mm:ss\"")
    }

    fun validateInstant(value: Value): String {
        val str = validateString(value)

        try {
            Instant.parse(str)
            return str
        } catch (e: Exception ) {
            // throw below
        }

        throw ValidationException("Expected a valid Instant string in format \"YYYY-MM-DDThh:mm:ssZ\"")
    }
}
