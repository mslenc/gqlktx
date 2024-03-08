package com.xs0.gqlktx.codegen

import com.xs0.gqlktx.ScalarCoercion
import com.xs0.gqlktx.schema.builder.ResolvedName
import com.xs0.gqlktx.utils.NodeId
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

object BaselineExporter {
    fun exportBoolean(value: Boolean?, coercion: ScalarCoercion): Boolean? {
        return value
    }

    fun exportBooleanNotNull(value: Boolean, coercion: ScalarCoercion): Boolean {
        return value
    }

    fun exportLong(value: Long?, coercion: ScalarCoercion): Number? {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON ->
                value?.toDouble()

            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET,
            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportLongNotNull(value: Long, coercion: ScalarCoercion): Number {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON ->
                value.toDouble()

            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET,
            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportInt(value: Int?, coercion: ScalarCoercion): Number? {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON ->
                value?.toDouble()

            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET,
            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportIntNotNull(value: Int, coercion: ScalarCoercion): Number {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON ->
                value.toDouble()

            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET,
            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportShort(value: Short?, coercion: ScalarCoercion): Number? {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON ->
                value?.toDouble()

            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET ->
                value?.toInt()

            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportShortNotNull(value: Short, coercion: ScalarCoercion): Number {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON ->
                value.toDouble()

            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET ->
                value.toInt()

            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportByte(value: Byte?, coercion: ScalarCoercion): Number? {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON ->
                value?.toDouble()

            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET ->
                value?.toInt()

            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportByteNotNull(value: Byte, coercion: ScalarCoercion): Number {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON ->
                value.toDouble()

            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET ->
                value.toInt()

            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportBooleanArray(value: BooleanArray?, coercion: ScalarCoercion): List<Boolean>? {
        return value?.map { exportBooleanNotNull(it, coercion) }
    }

    fun exportBooleanArrayNotNull(value: BooleanArray, coercion: ScalarCoercion): List<Boolean> {
        return value.map { exportBooleanNotNull(it, coercion) }
    }

    fun exportString(value: String?, coercion: ScalarCoercion): String? {
        return value
    }

    fun exportStringNotNull(value: String, coercion: ScalarCoercion): String {
        return value
    }

    fun exportDouble(value: Double?, coercion: ScalarCoercion): Double? {
        return value
    }

    fun exportDoubleNotNull(value: Double, coercion: ScalarCoercion): Double {
        return value
    }

    fun exportFloat(value: Float?, coercion: ScalarCoercion): Number? {
        return when (coercion) {
            ScalarCoercion.NONE -> value
            else -> value?.toDouble()
        }
    }

    fun exportFloatNotNull(value: Float, coercion: ScalarCoercion): Number {
        return when (coercion) {
            ScalarCoercion.NONE -> value
            else -> value.toDouble()
        }
    }

    fun exportBigDecimal(value: BigDecimal?, coercion: ScalarCoercion): Number? {
        return when (coercion) {
            ScalarCoercion.STRICT_JSON -> value?.toDouble()
            else -> value
        }
    }

    fun exportBigDecimalNotNull(value: BigDecimal, coercion: ScalarCoercion): Number {
        return when (coercion) {
            ScalarCoercion.STRICT_JSON -> value.toDouble()
            else -> value
        }
    }

    fun exportLocalDate(value: LocalDate?, coercion: ScalarCoercion): Any? {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON ->
                value?.toString()

            ScalarCoercion.SPREADSHEET,
            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportLocalDateNotNull(value: LocalDate, coercion: ScalarCoercion): Any {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON ->
                value.toString()

            ScalarCoercion.SPREADSHEET,
            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportLocalDateTime(value: LocalDateTime?, coercion: ScalarCoercion): Any? {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON ->
                value?.let { DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(it) }

            ScalarCoercion.SPREADSHEET,
            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportLocalDateTimeNotNull(value: LocalDateTime, coercion: ScalarCoercion): Any {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON ->
                DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(value)

            ScalarCoercion.SPREADSHEET,
            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportInstant(value: Instant?, coercion: ScalarCoercion): Any? {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON ->
                value?.let { DateTimeFormatter.ISO_INSTANT.format(it) }

            ScalarCoercion.SPREADSHEET,
            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportInstantNotNull(value: Instant, coercion: ScalarCoercion): Any {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON ->
                DateTimeFormatter.ISO_INSTANT.format(value)

            ScalarCoercion.SPREADSHEET,
            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportLocalTime(value: LocalTime?, coercion: ScalarCoercion): Any? {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON ->
                value?.toString()

            ScalarCoercion.SPREADSHEET,
            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportLocalTimeNotNull(value: LocalTime, coercion: ScalarCoercion): Any {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON ->
                value.toString()

            ScalarCoercion.SPREADSHEET,
            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportByteArray(value: ByteArray?, coercion: ScalarCoercion): Any? {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET ->
                value?.let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }

            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportByteArrayNotNull(value: ByteArray, coercion: ScalarCoercion): Any {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET ->
                Base64.getUrlEncoder().withoutPadding().encodeToString(value)

            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportCharArray(value: CharArray?, coercion: ScalarCoercion): Any? {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET ->
                value?.let { String(it) }

            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportCharArrayNotNull(value: CharArray, coercion: ScalarCoercion): Any {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET ->
                String(value)

            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportChar(value: Char?, coercion: ScalarCoercion): Any? {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET ->
                value?.toString()

            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportCharNotNull(value: Char, coercion: ScalarCoercion): Any {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET ->
                value.toString()

            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportUUID(value: UUID?, coercion: ScalarCoercion): Any? {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET ->
                value?.toString()

            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportUUIDNotNull(value: UUID, coercion: ScalarCoercion): Any {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET ->
                value.toString()

            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportNodeId(value: NodeId?, coercion: ScalarCoercion): Any? {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET ->
                value?.toPublicId()

            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportNodeIdNotNull(value: NodeId, coercion: ScalarCoercion): Any {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET ->
                value.toPublicId()

            ScalarCoercion.NONE ->
                value
        }
    }

    fun exportIntArray(value: IntArray?, coercion: ScalarCoercion): List<Number>? {
        return value?.map { exportIntNotNull(it, coercion) }
    }

    fun exportIntArrayNotNull(value: IntArray, coercion: ScalarCoercion): List<Number> {
        return value.map { exportIntNotNull(it, coercion) }
    }

    fun exportDoubleArray(value: DoubleArray?, coercion: ScalarCoercion): List<Double>? {
        return value?.map { exportDoubleNotNull(it, coercion) }
    }

    fun exportDoubleArrayNotNull(value: DoubleArray, coercion: ScalarCoercion): List<Double> {
        return value.map { exportDoubleNotNull(it, coercion) }
    }

    fun exportShortArray(value: ShortArray?, coercion: ScalarCoercion): List<Number>? {
        return value?.map { exportShortNotNull(it, coercion) }
    }

    fun exportShortArrayNotNull(value: ShortArray, coercion: ScalarCoercion): List<Number> {
        return value.map { exportShortNotNull(it, coercion) }
    }

    fun exportLongArray(value: LongArray?, coercion: ScalarCoercion): List<Number>? {
        return value?.map { exportLongNotNull(it, coercion) }
    }

    fun exportLongArrayNotNull(value: LongArray, coercion: ScalarCoercion): List<Number> {
        return value.map { exportLongNotNull(it, coercion) }
    }

    fun exportFloatArray(value: FloatArray?, coercion: ScalarCoercion): List<Number>? {
        return value?.map { exportFloatNotNull(it, coercion) }
    }

    fun exportFloatArrayNotNull(value: FloatArray, coercion: ScalarCoercion): List<Number> {
        return value.map { exportFloatNotNull(it, coercion) }
    }

    val importSet = setOf("com.xs0.gqlktx.codegen" to "BaselineExporter")

    fun codeGenInfo(name: ResolvedName, gen: CodeGen<*, *>, returnTypeNN: String = "Any", extraImports: Set<Pair<String, String>> = emptySet()): OutputExportCodeGenInfo {
        val isNullable = name.codeGenType.endsWith("?")

        val funName = when {
            isNullable -> name.codeGenFunName
            else -> name.codeGenFunName + "NotNull"
        }

        val allImports = importSet + extraImports + name.imports

        return OutputExportCodeGenInfo(
            OutputExportKind.BASELINE,
            funName,
            funReturnType = if (isNullable) returnTypeNN + "?" else returnTypeNN,
            funReturnTypeNN = returnTypeNN,
            funIsSuspending = false,
            gen.statePackage,
            "BaselineExporter.export$funName(VALUE, coercion)",
            emptySet(), // we don't generate
            allImports,
        )
    }
}