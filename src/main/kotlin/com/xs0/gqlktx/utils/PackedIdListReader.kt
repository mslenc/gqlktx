package com.xs0.gqlktx.utils

import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.UUID

import com.xs0.gqlktx.utils.PackedIdListWriter.Companion.TB_INT_ZERO
import com.xs0.gqlktx.utils.PackedIdListWriter.Companion.TB_INT_VAR
import com.xs0.gqlktx.utils.PackedIdListWriter.Companion.TB_INT_SIGNED_VAR
import com.xs0.gqlktx.utils.PackedIdListWriter.Companion.TB_INT_FIXED
import com.xs0.gqlktx.utils.PackedIdListWriter.Companion.TB_LONG_ZERO
import com.xs0.gqlktx.utils.PackedIdListWriter.Companion.TB_LONG_VAR
import com.xs0.gqlktx.utils.PackedIdListWriter.Companion.TB_LONG_SIGNED_VAR
import com.xs0.gqlktx.utils.PackedIdListWriter.Companion.TB_LONG_FIXED
import com.xs0.gqlktx.utils.PackedIdListWriter.Companion.TB_BOOLEAN_TRUE
import com.xs0.gqlktx.utils.PackedIdListWriter.Companion.TB_BOOLEAN_FALSE
import com.xs0.gqlktx.utils.PackedIdListWriter.Companion.TB_FLOAT
import com.xs0.gqlktx.utils.PackedIdListWriter.Companion.TB_DOUBLE
import com.xs0.gqlktx.utils.PackedIdListWriter.Companion.TB_BYTE
import com.xs0.gqlktx.utils.PackedIdListWriter.Companion.TB_SHORT
import com.xs0.gqlktx.utils.PackedIdListWriter.Companion.TB_CHAR
import com.xs0.gqlktx.utils.PackedIdListWriter.Companion.TB_UUID
import com.xs0.gqlktx.utils.PackedIdListWriter.Companion.TB_STRING_VARLEN
import com.xs0.gqlktx.utils.PackedIdListWriter.Companion.TB_STRING_LEN0
import java.lang.Long.reverseBytes
import java.nio.charset.StandardCharsets.UTF_8

class PackedIdListReader(private val input: InputStream) {
    private var eof: Boolean = false

    fun readNext(): Any? {
        if (eof)
            return null

        val tag = input.read()
        if (tag < 0) {
            eof = true
            return null
        }

        when (tag) {
            TB_INT_ZERO -> return 0

            TB_INT_VAR // varint encoding
            -> return readRawVarint32()

            TB_INT_SIGNED_VAR // varint encoding with signed transformation
            -> {
                val sval = readRawVarint32()
                return sval.ushr(1) xor -(sval and 1)
            }

            TB_INT_FIXED // 4-byte encoding, LSB first
            -> return readBytesAsInt(4)

            TB_LONG_ZERO -> return 0L

            TB_LONG_VAR // varint encoding
            -> return readRawVarint64()

            TB_LONG_SIGNED_VAR // varint encoding with signed transformation
            -> {
                val lval = readRawVarint64()
                return lval.ushr(1) xor -(lval and 1)
            }

            TB_LONG_FIXED // 8-byte encoding, LSB first
            -> return readBytesAsLong()

            TB_BOOLEAN_TRUE -> return true
            TB_BOOLEAN_FALSE -> return false

            TB_FLOAT  // 4 bytes, LSB first
            -> return java.lang.Float.intBitsToFloat(readBytesAsInt(4))
            TB_DOUBLE  // 8 bytes, LSB first
            -> return java.lang.Double.longBitsToDouble(readBytesAsLong())

            TB_BYTE -> return readBytesAsInt(1).toByte()
            TB_SHORT -> return readBytesAsInt(2).toShort()
            TB_CHAR -> return readBytesAsInt(2).toChar()
            TB_UUID -> {
                val msb = reverseBytes(readBytesAsLong())
                val lsb = reverseBytes(readBytesAsLong())
                return UUID(msb, lsb)
            }

            TB_STRING_VARLEN // first, varint of (length - 64), then length bytes (UTF8)
            -> {
                val len = readRawVarint32()
                if (len < 0 || len > Integer.MAX_VALUE - 64)
                    throw IOException("Invalid string length")
                return readUTF8(len + 64)
            }

            else -> {
                if (tag >= TB_STRING_LEN0)
                    return readUTF8(tag - TB_STRING_LEN0)

                throw IOException("Unknown tag encountered")
            }
        }
    }

    fun readRest(): Array<Any> {
        val list = ArrayList<Any>()
        while (true) {
            val value = readNext()
            if (value != null) {
                list.add(value)
            } else {
                return list.toTypedArray()
            }
        }
    }

    internal fun readUTF8(len: Int): String {
        val buff = ByteArray(len)

        var pos = 0
        while (pos < len) {
            val read = input.read(buff, pos, len - pos)
            if (read <= 0)
                throw EOFException("Incomplete string")
            pos += read
        }

        return String(buff, UTF_8)
    }

    private fun readBytesAsInt(len: Int): Int {
        var len = len
        var result = 0
        var shift = 0

        while (len-- > 0) {
            val b = input.read()
            if (b < 0)
                throw EOFException("Unexpected end of data")
            result = result or (b shl shift)
            shift += 8
        }

        return result
    }

    private fun readBytesAsLong(): Long {
        var result: Long = 0
        var shift = 0

        for (len in 8 downTo 1) {
            val b = input.read().toLong()
            if (b < 0)
                throw EOFException("Unexpected end of data")
            result = result or (b shl shift)
            shift += 8
        }

        return result
    }

    private fun readRawVarint64(): Long {
        var result: Long = 0
        var shift = 0
        while (shift < 64) {
            val b = input.read()
            if (b < 0)
                throw EOFException("Unexpected end of data")

            result = result or ((b and 0x7F).toLong() shl shift)
            if (b < 0x80) {
                return result
            }
            shift += 7
        }

        throw IOException("Invalid varint")
    }

    private fun readRawVarint32(): Int {
        val l = readRawVarint64()
        if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE)
            return l.toInt()

        throw IOException("Invalid varint - value exceeds 32 bits")
    }
}
