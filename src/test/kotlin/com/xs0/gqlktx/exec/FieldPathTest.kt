package com.xs0.gqlktx.exec

import org.junit.Test

import org.junit.Assert.*

class FieldPathTest {
    @Test
    fun testRoot() {
        val res = FieldPath.root().toArray()
        assertEquals(0, res.size().toLong())
    }

    @Test
    fun testFields() {
        val res = FieldPath.root().subField("abc").subField("cde").toArray()
        assertEquals(2, res.size().toLong())
        assertEquals("abc", res.getString(0))
        assertEquals("cde", res.getString(1))
    }

    @Test
    fun testIndexes() {
        val res = FieldPath.root().listElement(2).listElement(9).listElement(4).toArray()
        assertEquals(3, res.size().toLong())
        assertEquals(2, res.getInteger(0)!!.toInt().toLong())
        assertEquals(9, res.getInteger(1)!!.toInt().toLong())
        assertEquals(4, res.getInteger(2)!!.toInt().toLong())
    }

    @Test
    fun testMixed() {
        val res = FieldPath.root().subField("hero").subField("heroFriends").listElement(1).subField("name").toArray()
        assertEquals(4, res.size().toLong())
        assertEquals("hero", res.getString(0))
        assertEquals("heroFriends", res.getString(1))
        assertEquals(1, res.getInteger(2)!!.toInt().toLong())
        assertEquals("name", res.getString(3))
    }
}