package com.xs0.gqlktx.utils

import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID

import java.lang.Long.reverseBytes
import java.math.BigDecimal
import java.math.BigInteger

class PackedIdListWriter(private val out: OutputStream) {
    private fun writeVarint(value: Int) {
        var value = value
        while (true) {
            if (value and 0x7F.inv() == 0) {
                out.write(value)
                break
            } else {
                out.write(value and 0x7F or 0x80)
                value = value ushr 7
            }
        }
    }

    private fun writeVarint(value: Long) {
        var value = value
        while (true) {
            if (value and 0x7F.inv().toLong() == 0L) {
                out.write(value.toInt())
                break
            } else {
                out.write(value.toInt() and 0x7F or 0x80)
                value = value ushr 7
            }
        }
    }

    private fun writeFixedInt(value: Int) {
        out.write(value)
        out.write(value shr 8)
        out.write(value shr 16)
        out.write(value shr 24)
    }

    private fun writeFixedLong(value: Long) {
        writeFixedInt(value.toInt())
        writeFixedInt(value.ushr(32).toInt())
    }

    fun writeInt(value: Int) {
        if (value < 0xfff00000.toInt() || value >= 0x200000) {
            out.write(TB_INT_FIXED)
            writeFixedInt(value)
            return
        }

        when {
            value > 0 -> {
                out.write(TB_INT_VAR)
                writeVarint(value)
            }
            value < 0 -> {
                out.write(TB_INT_SIGNED_VAR)
                writeVarint(value shl 1 xor (value shr 31))
            }
            else -> out.write(TB_INT_ZERO)
        }
    }

    fun writeLong(value: Long) {
        if (value < MIN_VARINT_LONG || value >= MAX_VARINT_LONG) {
            out.write(TB_LONG_FIXED)
            writeFixedLong(value)
            return
        }

        when {
            value > 0 -> {
                out.write(TB_LONG_VAR)
                writeVarint(value)
            }
            value < 0 -> {
                out.write(TB_LONG_SIGNED_VAR)
                writeVarint(value shl 1 xor (value shr 63))
            }
            else -> out.write(TB_LONG_ZERO)
        }
    }

    fun writeFloat(value: Float) {
        out.write(TB_FLOAT)
        writeFixedInt(java.lang.Float.floatToRawIntBits(value))
    }

    fun writeDouble(value: Double) {
        out.write(TB_DOUBLE)
        writeFixedLong(java.lang.Double.doubleToRawLongBits(value))
    }

    fun writeBoolean(value: Boolean) {
        out.write(if (value) TB_BOOLEAN_TRUE else TB_BOOLEAN_FALSE)
    }

    fun writeByte(value: Byte) {
        out.write(TB_BYTE)
        out.write(value.toInt())
    }

    fun writeChar(value: Char) {
        out.write(TB_CHAR)
        out.write(value.toInt())
        out.write(value.toInt() shr 8)
    }

    fun writeShort(value: Short) {
        out.write(TB_SHORT)
        out.write(value.toInt())
        out.write(value.toInt() shr 8)
    }

    fun writeString(value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        if (bytes.size <= 63) {
            val tag = TB_STRING_LEN0 + bytes.size
            out.write(tag)
            out.write(bytes)
        } else {
            out.write(TB_STRING_VARLEN)
            writeVarint(bytes.size - 64)
            out.write(bytes)
        }
    }

    fun writeUUID(value: UUID) {
        // writeFixedLong is LSB-first, so we flip the bytes (rather than making another method)
        out.write(TB_UUID)
        writeFixedLong(reverseBytes(value.mostSignificantBits))
        writeFixedLong(reverseBytes(value.leastSignificantBits))
    }

    fun writeBigInteger(value: BigInteger) {
        val bytes = value.toByteArray()
        out.write(TB_BIGINTEGER)
        writeVarint(bytes.size)
        out.write(bytes)
    }

    fun writeBigDecimal(value: BigDecimal) {
        val bytes = value.unscaledValue().toByteArray()
        out.write(TB_BIGDECIMAL)
        writeVarint(value.scale())
        writeVarint(bytes.size)
        out.write(bytes)
    }

    companion object {

        const val TB_INT_ZERO = 0 // 0, no extra bytes
        const val TB_INT_VAR = 1 // varint encoding
        const val TB_INT_SIGNED_VAR = 2 // varint encoding with signed transformation
        const val TB_INT_FIXED = 3 // 4-byte encoding, LSB first

        const val TB_LONG_ZERO = 4 // 0L, no extra bytes
        const val TB_LONG_VAR = 5 // varint encoding
        const val TB_LONG_SIGNED_VAR = 6 // varint encoding with signed transformation
        const val TB_LONG_FIXED = 7 // 8-byte encoding, LSB first

        const val TB_BOOLEAN_TRUE = 8 // no extra bytes
        const val TB_BOOLEAN_FALSE = 9 // no extra bytes

        const val TB_FLOAT = 10 // 4 bytes, LSB first
        const val TB_DOUBLE = 11 // 8 bytes, LSB first

        const val TB_BYTE = 12 // 1 byte
        const val TB_SHORT = 13 // 2 bytes, LSB first
        const val TB_CHAR = 14 // 2 bytes, LSB first

        const val TB_UUID = 64 // 16 bytes, big endian
        const val TB_BIGINTEGER = 65 // varint length, then length bytes
        const val TB_BIGDECIMAL = 66 // varint scale, varint length, then length bytes

        const val TB_STRING_VARLEN = 191 // first, varint of (length - 64), then length bytes (UTF8)
        const val TB_STRING_LEN0 = 192 // this and next 63; (implicit length + that many bytes)

        const val MIN_VARINT_LONG: Long = 0xffffL shl 48
        const val MAX_VARINT_LONG: Long = 0x0002L shl 48
    }
}
