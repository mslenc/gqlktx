package com.xs0.gqlktx.codegen

import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.dom.*
import com.xs0.gqlktx.schema.builder.ResolvedName
import com.xs0.gqlktx.utils.NodeId
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Base64
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.floor

private fun err(msg: String): Nothing {
    throw ValidationException(msg)
}

object BaselineInputParser {
    fun parseBoolean(value: ValueOrVar, variables: Map<String, ValueOrNull>): Boolean? {
        return when (value) {
            is ValueBool -> value.value
            is ValueNull -> null
            is Variable -> parseBoolean(variables[value.name] ?: return null, variables)
            else -> err("Expected a boolean, but encountered something else.")
        }
    }

    fun parseBooleanNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): Boolean {
        return when (value) {
            is ValueBool -> value.value
            is ValueNull -> err("Expected a boolean, but found null instead.")
            is Variable -> parseBooleanNotNull(variables[value.name]?: err("Missing non-nullable variable ${ value.name }"), variables)
            else -> err("Expected a boolean, but encountered something else.")
        }
    }

    fun parseLong(value: ValueOrVar, variables: Map<String, ValueOrNull>): Long? {
        return when (value) {
            is ValueNumber -> {
                value.value.toLongOrNull()?.let { return it }

                val dval = value.value.toDoubleOrNull() ?: err("Expected a long value")

                if (dval < Long.MIN_VALUE || dval > Long.MAX_VALUE)
                    err("Long value out of range.")

                if (ceil(dval) != floor(dval))
                    err("Long value can't have fractional digits.")

                return dval.toLong()
            }
            is ValueNull -> null
            is Variable -> parseLong(variables[value.name] ?: return null, variables)
            else -> err("Expected a long value, but encountered something else.")
        }
    }

    fun parseLongNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): Long {
        return when (value) {
            is ValueNumber -> {
                value.value.toLongOrNull()?.let { return it }

                val dval = value.value.toDoubleOrNull() ?: err("Expected a long value")

                if (dval < Long.MIN_VALUE || dval > Long.MAX_VALUE)
                    err("Long value out of range.")

                if (ceil(dval) != floor(dval))
                    err("Long value can't have fractional digits.")

                return dval.toLong()
            }
            is ValueNull -> err("Expected a long value, but found null instead.")
            is Variable -> parseLongNotNull(variables[value.name] ?: err("Missing non-nullable variable ${ value.name }."), variables)
            else -> err("Expected a long value, but encountered something else.")
        }
    }

    fun parseInt(value: ValueOrVar, variables: Map<String, ValueOrNull>): Int? {
        return when (value) {
            is ValueNumber -> {
                value.value.toIntOrNull()?.let { return it }

                val dval = value.value.toDoubleOrNull() ?: err("Expected an integer value.")

                if (dval == dval.toInt().toDouble()) // 32-bit ints fit exactly in double
                    err("Expected an integer value, but it has a fractional part and/or is out of range.")

                dval.toInt()
            }
            is ValueNull -> null
            is Variable -> parseInt(variables[value.name] ?: return null, variables)
            else -> err("Expected an integer value, but encountered something else.")
        }
    }

    fun parseIntNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): Int {
        return when (value) {
            is ValueNumber -> {
                value.value.toIntOrNull()?.let { return it }

                val dval = value.value.toDoubleOrNull() ?: err("Expected an integer value.")

                if (dval == dval.toInt().toDouble()) // 32-bit ints fit exactly in double
                    err("Expected an integer value, but it has a fractional part and/or is out of range.")

                dval.toInt()
            }
            is ValueNull -> err("Expected an integer, but found null instead.")
            is Variable -> parseIntNotNull(variables[value.name] ?: err("Missing non-nullable variable ${ value.name }."), variables)
            else -> err("Expected an integer value, but encountered something else.")
        }
    }

    fun parseShort(value: ValueOrVar, variables: Map<String, ValueOrNull>): Short? {
        val intVal = parseInt(value, variables) ?: return null

        if (intVal !in Short.MIN_VALUE..Short.MAX_VALUE)
            err("Value out of range for short.")

        return intVal.toShort()
    }

    fun parseShortNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): Short {
        val intVal = parseIntNotNull(value, variables)

        if (intVal !in Short.MIN_VALUE..Short.MAX_VALUE)
            err("Value out of range for short.")

        return intVal.toShort()
    }

    fun parseByte(value: ValueOrVar, variables: Map<String, ValueOrNull>): Byte? {
        val intVal = parseInt(value, variables) ?: return null

        if (intVal !in Byte.MIN_VALUE..Byte.MAX_VALUE)
            err("Value out of range for byte.")

        return intVal.toByte()
    }

    fun parseByteNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): Byte {
        val intVal = parseIntNotNull(value, variables)

        if (intVal !in Byte.MIN_VALUE..Byte.MAX_VALUE)
            err("Value out of range for byte.")

        return intVal.toByte()
    }

    fun parseBooleanArray(value: ValueOrVar, variables: Map<String, ValueOrNull>): BooleanArray? {
        return when (value) {
            is ValueList -> {
                BooleanArray(value.elements.size) { i ->
                    parseBooleanNotNull(value.elements[i], variables)
                }
            }
            is ValueNull -> null
            is Variable -> parseBooleanArray(variables[value.name] ?: return null, variables)
            else -> err("Expected a list of booleans, but got something else.")
        }
    }

    fun parseBooleanArrayNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): BooleanArray {
        return when (value) {
            is ValueList -> {
                BooleanArray(value.elements.size) { i ->
                    parseBooleanNotNull(value.elements[i], variables)
                }
            }
            is ValueNull -> err("Expected a list of booleans, but found null instead.")
            is Variable -> parseBooleanArrayNotNull(variables[value.name] ?: err("Missing non-nullable variable ${ value.name }.") , variables)
            else -> err("Expected a list of booleans, but got something else.")
        }
    }

    fun parseString(value: ValueOrVar, variables: Map<String, ValueOrNull>): String? {
        return when (value) {
            is ValueString -> value.value
            is ValueNull -> null
            is Variable -> parseString(variables[value.name] ?: return null, variables)
            else -> err("Expected a string, but got something else.")
        }
    }

    fun parseStringNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): String {
        return when (value) {
            is ValueString -> value.value
            is ValueNull -> err("Expected a string, but found null instead.")
            is Variable -> parseStringNotNull(variables[value.name] ?: err("Missing non-nullable variable ${ value.name }."), variables)
            else -> err("Expected a string, but got something else.")
        }
    }

    fun parseDouble(value: ValueOrVar, variables: Map<String, ValueOrNull>): Double? {
        return when (value) {
            is ValueNumber -> {
                val d = value.value.toDoubleOrNull() ?: err("Expected a double value.")

                if (d.isNaN())
                    err("NaN encountered.")
                if (d.isInfinite())
                    err("Infinity encountered.")

                d
            }
            is ValueNull -> null
            is Variable -> parseDouble(variables[value.name] ?: return null, variables)
            else -> err("Expected a double, but got something else.")
        }
    }

    fun parseDoubleNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): Double {
        return when (value) {
            is ValueNumber -> {
                val d = value.value.toDoubleOrNull() ?: err("Expected a double value.")

                if (d.isNaN())
                    err("NaN encountered.")
                if (d.isInfinite())
                    err("Infinity encountered.")

                d
            }
            is ValueNull -> err("Expected a double, but found null instead.")
            is Variable -> parseDoubleNotNull(variables[value.name] ?: err("Missing non-nullable variable ${ value.name }."), variables)
            else -> err("Expected a double, but got something else.")
        }
    }

    fun parseFloat(value: ValueOrVar, variables: Map<String, ValueOrNull>): Float? {
        val d = parseDouble(value, variables) ?: return null
        val f = d.toFloat()
        if (f.isInfinite())
            err("Float value out of range.")
        return f
    }

    fun parseFloatNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): Float {
        val d = parseDoubleNotNull(value, variables)
        val f = d.toFloat()
        if (f.isInfinite())
            err("Float value out of range.")
        return f
    }

    fun parseBigDecimal(value: ValueOrVar, variables: Map<String, ValueOrNull>): BigDecimal? {
        return when (value) {
            is ValueNumber -> value.value.toBigDecimalOrNull() ?: err("Invalid number - couldn't convert to BigDecimal.")
            is ValueNull -> null
            is Variable -> parseBigDecimal(variables[value.name] ?: return null, variables)
            else -> err("Expected a BigDecimal, but got something else.")
        }
    }

    fun parseBigDecimalNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): BigDecimal {
        return when (value) {
            is ValueNumber -> value.value.toBigDecimalOrNull() ?: err("Invalid number - couldn't convert to BigDecimal.")
            is ValueNull -> err("Expected a BigDecimal, but found null instead.")
            is Variable -> parseBigDecimalNotNull(variables[value.name] ?: err("Missing non-nullable variable ${ value.name }."), variables)
            else -> err("Expected a BigDecimal, but got something else.")
        }
    }

    fun parseLocalDate(value: ValueOrVar, variables: Map<String, ValueOrNull>): LocalDate? {
        val string = when (value) {
            is ValueString -> value.value
            is ValueNull -> return null
            is Variable -> return parseLocalDate(variables[value.name] ?: return null, variables)
            else -> err("Expected a Date, but got something else.")
        }

        try {
            return LocalDate.parse(string)
        } catch (e: Exception) {
            err(e.message ?: "Couldn't parse as LocalDate")
        }
    }

    fun parseLocalDateNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): LocalDate {
        val string = when (value) {
            is ValueString -> value.value
            is ValueNull -> err("Expected a Date, but found null instead.")
            is Variable -> return parseLocalDateNotNull(variables[value.name] ?: err("Missing non-nullable variable ${ value.name }."), variables)
            else -> err("Expected a Date, but got something else.")
        }

        try {
            return LocalDate.parse(string)
        } catch (e: Exception) {
            err(e.message ?: "Couldn't parse as LocalDate")
        }
    }

    fun parseLocalDateTime(value: ValueOrVar, variables: Map<String, ValueOrNull>): LocalDateTime? {
        val string = when (value) {
            is ValueString -> value.value
            is ValueNull -> return null
            is Variable -> return parseLocalDateTime(variables[value.name] ?: return null, variables)
            else -> err("Expected a Date, but got something else.")
        }

        try {
            return LocalDateTime.parse(string)
        } catch (e: Exception) {
            err(e.message ?: "Couldn't parse as LocalDate")
        }
    }

    fun parseLocalDateTimeNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): LocalDateTime {
        val string = when (value) {
            is ValueString -> value.value
            is ValueNull -> err("Expected a DateTime, but found null instead.")
            is Variable -> return parseLocalDateTimeNotNull(variables[value.name] ?: err("Missing non-nullable variable ${ value.name }."), variables)
            else -> err("Expected a DateTime, but got something else.")
        }

        try {
            return LocalDateTime.parse(string)
        } catch (e: Exception) {
            err(e.message ?: "Couldn't parse as LocalDateTime")
        }
    }

    fun parseInstant(value: ValueOrVar, variables: Map<String, ValueOrNull>): Instant? {
        val string = when (value) {
            is ValueString -> value.value
            is ValueNull -> return null
            is Variable -> return parseInstant(variables[value.name] ?: return null, variables)
            else -> err("Expected a Instant, but got something else.")
        }

        try {
            return Instant.parse(string)
        } catch (e: Exception) {
            err(e.message ?: "Couldn't parse as Instant")
        }
    }

    fun parseInstantNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): Instant {
        val string = when (value) {
            is ValueString -> value.value
            is ValueNull -> err("Expected an Instant, but found null instead.")
            is Variable -> return parseInstantNotNull(variables[value.name] ?: err("Missing non-nullable variable ${ value.name }."), variables)
            else -> err("Expected an Instant, but got something else.")
        }

        try {
            return Instant.parse(string)
        } catch (e: Exception) {
            err(e.message ?: "Couldn't parse as Instant")
        }
    }

    fun parseLocalTime(value: ValueOrVar, variables: Map<String, ValueOrNull>): LocalTime? {
        val string = when (value) {
            is ValueString -> value.value
            is ValueNull -> return null
            is Variable -> return parseLocalTime(variables[value.name] ?: return null, variables)
            else -> err("Expected a Time, but got something else.")
        }

        try {
            return LocalTime.parse(string)
        } catch (e: Exception) {
            err(e.message ?: "Couldn't parse as LocalDate")
        }
    }

    fun parseLocalTimeNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): LocalTime {
        val string = when (value) {
            is ValueString -> value.value
            is ValueNull -> err("Expected a Time, but found null instead.")
            is Variable -> return parseLocalTimeNotNull(variables[value.name] ?: err("Missing non-nullable variable ${ value.name }."), variables)
            else -> err("Expected a Time, but got something else.")
        }

        try {
            return LocalTime.parse(string)
        } catch (e: Exception) {
            err(e.message ?: "Couldn't parse as LocalTime")
        }
    }

    fun parseByteArray(value: ValueOrVar, variables: Map<String, ValueOrNull>): ByteArray? {
        return when (value) {
            is ValueString -> {
                try {
                    return Base64.getUrlDecoder().decode(value.value)
                } catch (e: IllegalArgumentException) {
                    err("Couldn't base64-decode value: " + e.message)
                }
            }
            is ValueNull -> null
            is Variable -> parseByteArray(variables[value.name] ?: return null, variables)
            else -> err("Expected a base-64 encoded byte array, but got something else.")
        }
    }

    fun parseByteArrayNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): ByteArray {
        return when (value) {
            is ValueString -> {
                try {
                    return Base64.getUrlDecoder().decode(value.value)
                } catch (e: IllegalArgumentException) {
                    err("Couldn't base64-decode value: " + e.message)
                }
            }
            is ValueNull -> err("Expected a base-64 encoded byte array, but found null instead.")
            is Variable -> parseByteArrayNotNull(variables[value.name] ?: err("Missing non-nullable variable ${ value.name }."), variables)
            else -> err("Expected a base-64 encoded byte array, but got something else.")
        }
    }

    fun parseCharArray(value: ValueOrVar, variables: Map<String, ValueOrNull>): CharArray? {
        return when (value) {
            is ValueString -> value.value.toCharArray()
            is ValueNull -> null
            is Variable -> parseCharArray(variables[value.name] ?: return null, variables)
            else -> err("Expected a char array (string), but got something else.")
        }
    }

    fun parseCharArrayNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): CharArray {
        return when (value) {
            is ValueString -> value.value.toCharArray()
            is ValueNull -> err("Expected a char array (string), but found null instead.")
            is Variable -> parseCharArrayNotNull(variables[value.name] ?: err("Missing non-nullable variable ${ value.name }."), variables)
            else -> err("Expected a char array (string), but got something else.")
        }
    }

    fun parseChar(value: ValueOrVar, variables: Map<String, ValueOrNull>): Char? {
        return when (value) {
            is ValueString -> {
                val str = value.value
                if (str.length > 1)
                    err("Expected a character, but received a multi-char string")
                if (str.isEmpty())
                    err("Expected a character, but received an empty string")
                str[0]
            }
            is ValueNull -> null
            is Variable -> parseChar(variables[value.name] ?: return null, variables)
            else -> err("Expected a char (string), but got something else.")
        }
    }

    fun parseCharNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): Char {
        return when (value) {
            is ValueString -> {
                val str = value.value
                if (str.length > 1)
                    err("Expected a character, but received a multi-char string")
                if (str.isEmpty())
                    err("Expected a character, but received an empty string")
                str[0]
            }
            is ValueNull -> err("Expected a char (string), but found null instead.")
            is Variable -> parseCharNotNull(variables[value.name] ?: err("Missing non-nullable variable ${ value.name }."), variables)
            else -> err("Expected a char (string), but got something else.")
        }
    }

    fun parseUUID(value: ValueOrVar, variables: Map<String, ValueOrNull>): UUID? {
        val string = when (value) {
            is ValueString -> value.value
            is ValueNull -> return null
            is Variable -> return parseUUID(variables[value.name] ?: return null, variables)
            else -> err("Expected a UUID, but got something else.")
        }

        try {
            return UUID.fromString(string)
        } catch (e: Exception) {
            err(e.message ?: "Couldn't parse as UUID")
        }
    }

    fun parseUUIDNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): UUID {
        val string = when (value) {
            is ValueString -> value.value
            is ValueNull -> err("Expected an UUID, but found null instead.")
            is Variable -> return parseUUIDNotNull(variables[value.name] ?: err("Missing non-nullable variable ${ value.name }."), variables)
            else -> err("Expected an UUID, but got something else.")
        }

        try {
            return UUID.fromString(string)
        } catch (e: Exception) {
            err(e.message ?: "Couldn't parse as UUID")
        }
    }

    fun parseNodeId(value: ValueOrVar, variables: Map<String, ValueOrNull>): NodeId? {
        val string = when (value) {
            is ValueString -> value.value
            is ValueNull -> return null
            is Variable -> return parseNodeId(variables[value.name] ?: return null, variables)
            else -> err("Expected an Id, but got something else.")
        }

        try {
            return NodeId.fromPublicID(string)
        } catch (e: Exception) {
            err(e.message ?: "Couldn't parse as NodeId")
        }
    }

    fun parseNodeIdNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): NodeId {
        val string = when (value) {
            is ValueString -> value.value
            is ValueNull -> err("Expected an Id, but found null instead.")
            is Variable -> return parseNodeIdNotNull(variables[value.name] ?: err("Missing non-nullable variable ${ value.name }."), variables)
            else -> err("Expected an Id, but got something else.")
        }

        try {
            return NodeId.fromPublicID(string)
        } catch (e: Exception) {
            err(e.message ?: "Couldn't parse as NodeId")
        }
    }

    fun parseIntArray(value: ValueOrVar, variables: Map<String, ValueOrNull>): IntArray? {
        return when (value) {
            is ValueList -> {
                IntArray(value.elements.size) { i ->
                    parseIntNotNull(value.elements[i], variables)
                }
            }
            is ValueNull -> null
            is Variable -> parseIntArray(variables[value.name] ?: return null, variables)
            else -> err("Expected a list of ints, but got something else.")
        }
    }

    fun parseIntArrayNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): IntArray {
        return when (value) {
            is ValueList -> {
                IntArray(value.elements.size) { i ->
                    parseIntNotNull(value.elements[i], variables)
                }
            }
            is ValueNull -> err("Expected a list of ints, but found null instead.")
            is Variable -> parseIntArrayNotNull(variables[value.name] ?: err("Missing non-nullable variable ${ value.name }.") , variables)
            else -> err("Expected a list of ints, but got something else.")
        }
    }

    fun parseDoubleArray(value: ValueOrVar, variables: Map<String, ValueOrNull>): DoubleArray? {
        return when (value) {
            is ValueList -> {
                DoubleArray(value.elements.size) { i ->
                    parseDoubleNotNull(value.elements[i], variables)
                }
            }
            is ValueNull -> null
            is Variable -> parseDoubleArray(variables[value.name] ?: return null, variables)
            else -> err("Expected a list of Doubles, but got something else.")
        }
    }

    fun parseDoubleArrayNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): DoubleArray {
        return when (value) {
            is ValueList -> {
                DoubleArray(value.elements.size) { i ->
                    parseDoubleNotNull(value.elements[i], variables)
                }
            }
            is ValueNull -> err("Expected a list of Doubles, but found null instead.")
            is Variable -> parseDoubleArrayNotNull(variables[value.name] ?: err("Missing non-nullable variable ${ value.name }.") , variables)
            else -> err("Expected a list of Doubles, but got something else.")
        }
    }

    fun parseShortArray(value: ValueOrVar, variables: Map<String, ValueOrNull>): ShortArray? {
        return when (value) {
            is ValueList -> {
                ShortArray(value.elements.size) { i ->
                    parseShortNotNull(value.elements[i], variables)
                }
            }
            is ValueNull -> null
            is Variable -> parseShortArray(variables[value.name] ?: return null, variables)
            else -> err("Expected a list of Shorts, but got something else.")
        }
    }

    fun parseShortArrayNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): ShortArray {
        return when (value) {
            is ValueList -> {
                ShortArray(value.elements.size) { i ->
                    parseShortNotNull(value.elements[i], variables)
                }
            }
            is ValueNull -> err("Expected a list of Shorts, but found null instead.")
            is Variable -> parseShortArrayNotNull(variables[value.name] ?: err("Missing non-nullable variable ${ value.name }.") , variables)
            else -> err("Expected a list of Shorts, but got something else.")
        }
    }

    fun parseLongArray(value: ValueOrVar, variables: Map<String, ValueOrNull>): LongArray? {
        return when (value) {
            is ValueList -> {
                LongArray(value.elements.size) { i ->
                    parseLongNotNull(value.elements[i], variables)
                }
            }
            is ValueNull -> null
            is Variable -> parseLongArray(variables[value.name] ?: return null, variables)
            else -> err("Expected a list of Longs, but got something else.")
        }
    }

    fun parseLongArrayNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): LongArray {
        return when (value) {
            is ValueList -> {
                LongArray(value.elements.size) { i ->
                    parseLongNotNull(value.elements[i], variables)
                }
            }
            is ValueNull -> err("Expected a list of Longs, but found null instead.")
            is Variable -> parseLongArrayNotNull(variables[value.name] ?: err("Missing non-nullable variable ${ value.name }.") , variables)
            else -> err("Expected a list of Longs, but got something else.")
        }
    }

    fun parseFloatArray(value: ValueOrVar, variables: Map<String, ValueOrNull>): FloatArray? {
        return when (value) {
            is ValueList -> {
                FloatArray(value.elements.size) { i ->
                    parseFloatNotNull(value.elements[i], variables)
                }
            }
            is ValueNull -> null
            is Variable -> parseFloatArray(variables[value.name] ?: return null, variables)
            else -> err("Expected a list of Floats, but got something else.")
        }
    }

    fun parseFloatArrayNotNull(value: ValueOrVar, variables: Map<String, ValueOrNull>): FloatArray {
        return when (value) {
            is ValueList -> {
                FloatArray(value.elements.size) { i ->
                    parseFloatNotNull(value.elements[i], variables)
                }
            }
            is ValueNull -> err("Expected a list of Floats, but found null instead.")
            is Variable -> parseFloatArrayNotNull(variables[value.name] ?: err("Missing non-nullable variable ${ value.name }.") , variables)
            else -> err("Expected a list of Floats, but got something else.")
        }
    }

    val importSet = setOf("com.xs0.gqlktx.codegen" to "BaselineInputParser")

    fun codeGenInfo(name: ResolvedName, gen: CodeGen<*, *>, extraImports: Set<Pair<String, String>> = emptySet()): InputParseCodeGenInfo {
        val nullable = name.codeGenType.endsWith("?")

        val funName = when {
            nullable -> name.codeGenFunName
            else -> name.codeGenFunName + "NotNull"
        }

        val allImports = importSet + extraImports + name.imports

        return InputParseCodeGenInfo(
            kind = InputParseKind.BASELINE,
            funName = funName,
            funReturnType = name.codeGenType,
            funCreateType = name.codeGenTypeNN,
            outPackageName = gen.statePackage,
            exprTemplate = "BaselineInputParser.parse$funName(VALUE, variables)",
            importsForGen = emptySet(), // we don't generate
            importsForUse = allImports,
            nullable = nullable
        )
    }
}