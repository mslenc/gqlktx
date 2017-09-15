package com.xs0.gqlktx.utils

import org.junit.Test

import java.util.ArrayList
import java.util.Base64
import java.util.Random
import java.util.UUID

import org.junit.Assert.*
import kotlin.reflect.KClass

class NodeIdTest {
    @Test
    fun testSimple() {
        val id = NodeId.create("type").add(UUID.randomUUID()).build()

        assertEquals("type", id.typeId)
        assertEquals(30, id.toPublicId().length.toLong()) // (1 + 4 + 1 + 16 == 22) bytes = 176 bits = (ceil(176/6)==30) encoded bytes

        val decoded = NodeId.fromPublicID(id.toPublicId())

        assertEquals("type", decoded.typeId)
        assertEquals(id.getPart(0, UUID::class), decoded.getPart(0, UUID::class))
    }

    @Test
    fun testAllTypes() {
        for (a in 0..99)
            doTestAllTypes()
    }

    fun doTestAllTypes() {
        val builder = NodeId.create("asdf")
        val expected = ArrayList<Any>()
        val types = ArrayList<KClass<*>>()

        val rnd = Random()

        for (b in booleanArrayOf(true, false)) {
            expected.add(b)
            builder.add(b)
            types.add(Boolean::class)
        }

        for (b in byteArrayOf(java.lang.Byte.MIN_VALUE, java.lang.Byte.MAX_VALUE, 0, -1, 1)) {
            expected.add(b)
            builder.add(b)
            types.add(Byte::class)
        }

        for (a in 0..2) {
            val b = rnd.nextInt().toByte()
            expected.add(b)
            builder.add(b)
            types.add(Byte::class)
        }

        for (s in shortArrayOf(java.lang.Short.MIN_VALUE, java.lang.Short.MAX_VALUE, 0, -1, 1)) {
            expected.add(s)
            builder.add(s)
            types.add(Short::class)
        }

        for (a in 0..2) {
            val s = rnd.nextInt().toShort()
            expected.add(s)
            builder.add(s)
            types.add(Short::class)
        }

        for (c in charArrayOf('\u0000', '\u0020', '\uffff')) {
            expected.add(c)
            builder.add(c)
            types.add(Char::class)
        }

        for (a in 0..2) {
            val c = rnd.nextInt().toChar()
            expected.add(c)
            builder.add(c)
            types.add(Char::class)
        }

        for (i in intArrayOf(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 1, -1)) {
            expected.add(i)
            builder.add(i)
            types.add(Int::class)
        }

        for (a in 0..2) {
            val i = rnd.nextInt()
            expected.add(i)
            builder.add(i)
            types.add(Int::class)
        }

        for (l in longArrayOf(java.lang.Long.MIN_VALUE, java.lang.Long.MAX_VALUE, 0, 1, -1)) {
            expected.add(l)
            builder.add(l)
            types.add(Long::class)
        }
        for (a in 0..2) {
            val l = rnd.nextLong()
            expected.add(l)
            builder.add(l)
            types.add(Long::class)
        }

        for (f in floatArrayOf(java.lang.Float.MIN_VALUE, -java.lang.Float.MIN_VALUE, java.lang.Float.MAX_VALUE, -java.lang.Float.MAX_VALUE, 0f, 1.0f, -1.0f, java.lang.Float.NaN, java.lang.Float.POSITIVE_INFINITY, java.lang.Float.NEGATIVE_INFINITY)) {
            expected.add(f)
            builder.add(f)
            types.add(Float::class)
        }
        for (a in 0..2) {
            val f = java.lang.Float.intBitsToFloat(rnd.nextInt())
            expected.add(f)
            builder.add(f)
            types.add(Float::class)
        }

        for (f in doubleArrayOf(java.lang.Double.MIN_VALUE, -java.lang.Double.MIN_VALUE, java.lang.Double.MAX_VALUE, -java.lang.Double.MAX_VALUE, 0.0, 1.0, -1.0, java.lang.Double.NaN, java.lang.Double.POSITIVE_INFINITY, java.lang.Double.NEGATIVE_INFINITY)) {
            expected.add(f)
            builder.add(f)
            types.add(Double::class)
        }
        for (a in 0..2) {
            val d = java.lang.Double.longBitsToDouble(rnd.nextLong())
            expected.add(d)
            builder.add(d)
            types.add(Double::class)
        }

        for (a in 0..4) {
            val uuid = UUID.randomUUID()
            expected.add(uuid)
            builder.add(uuid)
            types.add(UUID::class)
        }

        for (s in arrayOf("", "abc", "और घर के गार्डन", "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789")) {
            expected.add(s)
            builder.add(s)
            types.add(String::class)
        }

        val id = builder.build()
        val expectedPartTypes = types.toTypedArray()


        assertEquals("asdf", id.typeId)
        assertTrue(id.matches("asdf", *expectedPartTypes))
        for (i in expected.indices) {
            assertEquals(expected[i], id.getPart(i, expected[i]::class))
        }

        val encoded = id.toPublicId()
        Base64.getDecoder().decode(encoded) // check it's valid base64

        val decoded = NodeId.fromPublicID(encoded)

        assertEquals("asdf", decoded.typeId)
        assertTrue(id.matches("asdf", *expectedPartTypes))
        for (i in expected.indices) {
            assertEquals(expected[i], decoded.getPart(i, expected[i]::class))
        }
    }

    @Test
    fun testSpeed() {
        val start = System.nanoTime()
        val reps = 100000

        // FYI, in VirtualBox on Ryzen 1700 with 90% CPU throttle:
        // 10000000 encodings/decodings in 10.691300658 seconds
        // 1069 ns per op
        // seems good enough..

        for (a in 0..reps - 1) {
            val id = NodeId.create("item").add(1486723789562L).add("SKU0000001").build()
            val encoded = id.toPublicId()
            val decoded = NodeId.fromPublicID(encoded)

            assertTrue(id.matches("item", Long::class, String::class))
            decoded.getPart(0, Long::class)
            decoded.getPart(1, String::class)
        }

        val end = System.nanoTime()

        val totalTime = end - start
        val perIteration = totalTime / reps

        println(reps.toString() + " encodings/decodings in " + totalTime / 1000000000.0 + " seconds")
        println(perIteration.toString() + " ns per op")
    }
}