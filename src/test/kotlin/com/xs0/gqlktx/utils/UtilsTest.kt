package com.xs0.gqlktx.utils

import org.junit.Test

import java.util.Base64

import org.junit.Assert.*

class UtilsTest {
    @Test
    fun testBase64ULong() {
        val explicitTests = longArrayOf(0, 1, 255, 256, 65535, 65536, 16777215, 16777216, -1, -255, -256, -65535, -65536, -16777215, -16777216, java.lang.Long.MIN_VALUE, java.lang.Long.MAX_VALUE, Integer.MIN_VALUE.toLong(), Integer.MIN_VALUE - 1L, Integer.MAX_VALUE.toLong(), Integer.MAX_VALUE + 1L, java.lang.Short.MIN_VALUE.toLong(), java.lang.Short.MIN_VALUE - 1L, java.lang.Short.MAX_VALUE.toLong(), java.lang.Short.MAX_VALUE + 1L, java.lang.Byte.MIN_VALUE.toLong(), java.lang.Byte.MIN_VALUE - 1L, java.lang.Byte.MAX_VALUE.toLong(), java.lang.Byte.MAX_VALUE + 1L, 0x02061979)

        for (l in explicitTests)
            checkBase64ULong(l)

        var bit1: Long = 1
        while (bit1 != 0L) {
            var bit2: Long = 1
            while (bit2 != 0L) {
                checkBase64ULong(bit1 or bit2)
                bit2 = bit2 shl 1
            }
            bit1 = bit1 shl 1
        }
    }

    private fun checkBase64ULong(l: Long) {
        val encoded = base64EncodeULong(l)

        assertNotNull(encoded)
        assertTrue("" + l + ": " + encoded, encoded.length >= 1)
        assertTrue("" + l + ": " + encoded, encoded.length <= 11)

        Base64.getUrlDecoder().decode(encoded) // throws IllegalArgEx if not base64

        val d = base64DecodeULong(encoded)

        assertEquals(l, d)
    }
}