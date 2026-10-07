package com.xs0.gqlktx.query.parser

import com.xs0.gqlktx.parser.CharStream
import org.junit.Test

import org.junit.Assert.*

class CharStreamTest {
    @Test
    fun testBasics() {
        val cs = CharStream("abc\ndef")

        assertFalse(cs.end())
        assertEquals(1, cs.row())
        assertEquals(1, cs.col())
        assertEquals('a'.code, cs.peek())
        assertEquals(-1, cs.consume { Character.isDigit(it) })
        assertEquals('a'.code, cs.get())

        assertFalse(cs.end())
        assertEquals(1, cs.row())
        assertEquals(2, cs.col())
        assertEquals('b'.code, cs.peek())
        assertEquals(-1, cs.consume { Character.isWhitespace(it) })
        assertEquals('b'.code, cs.consume { Character.isAlphabetic(it) })

        assertFalse(cs.end())
        assertEquals(1, cs.row())
        assertEquals(3, cs.col())
        assertEquals('c'.code, cs.peek())
        assertEquals('c'.code, cs.consume { it == 'c'.code })

        assertFalse(cs.end())
        assertEquals(1, cs.row())
        assertEquals(4, cs.col())
        assertEquals('\n'.code, cs.peek())
        assertEquals('\n'.code, cs.get())

        assertFalse(cs.end())
        assertEquals(2, cs.row())
        assertEquals(1, cs.col())
        assertEquals('d'.code, cs.peek())
        assertEquals(-1, cs.consume { Character.isDigit(it) })
        assertEquals('d'.code, cs.get())

        assertFalse(cs.end())
        assertEquals(2, cs.row())
        assertEquals(2, cs.col())
        assertEquals('e'.code, cs.peek())
        assertEquals(-1, cs.consume { Character.isWhitespace(it) })
        assertEquals('e'.code, cs.consume { Character.isAlphabetic(it) })

        assertFalse(cs.end())
        assertEquals(2, cs.row())
        assertEquals(3, cs.col())
        assertEquals('f'.code, cs.peek())
        assertEquals('f'.code, cs.consume { it == 'f'.code })

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
            assertEquals(row, cs.row())
            assertEquals(col, cs.col())
            assertEquals(c.code, cs.peek())
            assertEquals(c.code, cs.get())
            if (c == '\n') {
                row++
                col = 1
            } else {
                col++
            }
        }

        assertEquals(row, cs.row())
        assertEquals(col, cs.col())
        assertTrue(cs.end())
    }

}