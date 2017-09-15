package com.xs0.gqlktx.query.parser

import com.xs0.gqlktx.parser.CharStream
import org.junit.Test

import org.junit.Assert.*

class CharStreamTest {
    @Test
    fun testBasics() {
        val cs = CharStream("abc\ndef")

        assertFalse(cs.end())
        assertEquals(1, cs.row().toLong())
        assertEquals(1, cs.col().toLong())
        assertEquals('a', cs.peek().toLong())
        assertEquals(-1, cs.consume { codePoint: Int -> Character.isDigit(codePoint) }.toLong())
        assertEquals('a', cs.get().toLong())

        assertFalse(cs.end())
        assertEquals(1, cs.row().toLong())
        assertEquals(2, cs.col().toLong())
        assertEquals('b', cs.peek().toLong())
        assertEquals(-1, cs.consume { codePoint: Int -> Character.isWhitespace(codePoint) }.toLong())
        assertEquals('b', cs.consume { codePoint: Int -> Character.isAlphabetic(codePoint) }.toLong())

        assertFalse(cs.end())
        assertEquals(1, cs.row().toLong())
        assertEquals(3, cs.col().toLong())
        assertEquals('c', cs.peek().toLong())
        assertEquals('c', cs.consume { c -> c == 'c'.toInt() }.toLong())

        assertFalse(cs.end())
        assertEquals(1, cs.row().toLong())
        assertEquals(4, cs.col().toLong())
        assertEquals('\n', cs.peek().toLong())
        assertEquals('\n', cs.get().toLong())

        assertFalse(cs.end())
        assertEquals(2, cs.row().toLong())
        assertEquals(1, cs.col().toLong())
        assertEquals('d', cs.peek().toLong())
        assertEquals(-1, cs.consume { codePoint: Int -> Character.isDigit(codePoint) }.toLong())
        assertEquals('d', cs.get().toLong())

        assertFalse(cs.end())
        assertEquals(2, cs.row().toLong())
        assertEquals(2, cs.col().toLong())
        assertEquals('e', cs.peek().toLong())
        assertEquals(-1, cs.consume { codePoint: Int -> Character.isWhitespace(codePoint) }.toLong())
        assertEquals('e', cs.consume { codePoint: Int -> Character.isAlphabetic(codePoint) }.toLong())

        assertFalse(cs.end())
        assertEquals(2, cs.row().toLong())
        assertEquals(3, cs.col().toLong())
        assertEquals('f', cs.peek().toLong())
        assertEquals('f', cs.consume { c -> c == 'f'.toInt() }.toLong())

        assertTrue(cs.end())
    }

    @Test
    fun testNewLines() {
        val cs = CharStream("ab\ncd\ref\r\ngh\n\rij")
        val expected = "ab\ncd\nef\ngh\n\nij".toCharArray()

        var row = 1
        var col = 1
        for (c in expected) {
            assertFalse(cs.end())
            assertEquals(row.toLong(), cs.row().toLong())
            assertEquals(col.toLong(), cs.col().toLong())
            assertEquals(c.toLong(), cs.peek().toLong())
            assertEquals(c.toLong(), cs.get().toLong())
            if (c == '\n') {
                row++
                col = 1
            } else {
                col++
            }
        }

        assertEquals(row.toLong(), cs.row().toLong())
        assertEquals(col.toLong(), cs.col().toLong())
        assertTrue(cs.end())
    }

}