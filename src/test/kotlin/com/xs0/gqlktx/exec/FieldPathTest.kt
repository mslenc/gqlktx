package com.xs0.gqlktx.exec

import org.junit.Test

import org.junit.Assert.*

class FieldPathTest {
    @Test
    fun testRoot() {
        val res = FieldPath.root().toArray()
        assertEquals(0, res.size)
    }

    @Test
    fun testFields() {
        val res = FieldPath.root().subField("abc").subField("cde").toArray()
        assertEquals(2, res.size)
        assertEquals("abc", res[0])
        assertEquals("cde", res[1])
    }

    @Test
    fun testIndexes() {
        val res = FieldPath.root().listElement(2).listElement(9).listElement(4).toArray()
        assertEquals(3, res.size)
        assertEquals(2, res[0])
        assertEquals(9, res[1])
        assertEquals(4, res[2])
    }

    @Test
    fun testMixed() {
        val res = FieldPath.root().subField("hero").subField("heroFriends").listElement(1).subField("name").toArray()
        assertEquals(4, res.size)
        assertEquals("hero", res[0])
        assertEquals("heroFriends", res[1])
        assertEquals(1, res[2])
        assertEquals("name", res[3])
    }
}