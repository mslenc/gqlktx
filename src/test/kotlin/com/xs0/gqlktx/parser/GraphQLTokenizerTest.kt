package com.xs0.gqlktx.parser

import com.xs0.gqlktx.ParseException
import org.junit.Test

import org.junit.Assert.*
import com.xs0.gqlktx.parser.Token.Type.*

class GraphQLTokenizerTest {
    @Throws(ParseException::class)
    internal fun testSingle(txt: String, type: Token.Type, value: Any?, row: Int, col: Int) {
        val tokenizer = GraphQLTokenizer(CharStream(txt))

        val token = tokenizer.peek<Any>()
        assertNotNull(token)
        assertEquals(type, token.type)
        assertEquals(value, token.value)
        assertEquals(row.toLong(), token.row.toLong())
        assertEquals(col.toLong(), token.column.toLong())

        assertSame(token, tokenizer.next<Any>())

        val eof = tokenizer.next<Any>()
        assertNotNull(eof)
        assertEquals(EOF, eof.type)
        assertEquals(Unit, eof.value)
    }

    @Throws(ParseException::class)
    internal fun testSingle(txt: String, type: Token.Type, value: Any?) {
        testSingle(txt, type, value, 1, 1)
        testSingle(" , " + txt, type, value, 1, 4)
        testSingle("  # comment\n\n     $txt  # or whatever", type, value, 3, 6)
    }

    @Throws(ParseException::class)
    internal fun testSingleInt(`val`: Int) {
        if (`val` >= 0) {
            testSingle("" + `val`, INTEGER, `val`)
            testSingle("+" + `val`, INTEGER, `val`)
            testSingle("-" + `val`, INTEGER, -`val`)
        } else {
            testSingle("" + `val`, INTEGER, `val`)
        }
    }

    @Test
    @Throws(ParseException::class)
    fun testSingle() {
        testSingleInt(0)
        testSingleInt(123)
        testSingleInt(1023)
        testSingleInt(1203)
        testSingleInt(9990)
        testSingleInt(Integer.MAX_VALUE)
        testSingleInt(Integer.MIN_VALUE)

        for (decimals in arrayOf("", ".0", ".1", ".987654")) {
            for (e in arrayOf("", "e", "e+", "e-", "E", "E+", "E-")) {
                if ((decimals + e).isEmpty())
                    continue

                for (exp in if (e.isEmpty()) arrayOf("") else arrayOf("0", "6", "85")) {
                    val txt = (Math.random() * 1000000).toInt().toString() + decimals + e + exp
                    val `val` = java.lang.Double.parseDouble(txt)
                    testSingle(txt, FLOAT, `val`)
                }
            }
        }

        testSingle("\"\"", STRING, "")
        testSingle("\"a b c\"", STRING, "a b c")
        testSingle("\" 1\\n2\\r3\\t4\\b5\\f\\\"g\\\\h\\/ \"", STRING, " 1\n2\r3\t4\b5\u000c\"g\\h/ ")

        val names = arrayOf("Janez", "__typename", "_whatIsThis0031251", "fragment", "query", "mutation", "subscription", "schema", "scalar", "type", "interface", "implements", "enum", "union", "input", "extend", "directive", "on", "null", "true", "false")

        for (name in names)
            testSingle(name, NAME, name)

        testSingle("(", LPAREN, Unit)
        testSingle(")", RPAREN, Unit)
        testSingle("{", LCURLY, Unit)
        testSingle("}", RCURLY, Unit)
        testSingle("[", LBRACK, Unit)
        testSingle("]", RBRACK, Unit)
        testSingle("|", PIPE, Unit)
        testSingle(":", COLON, Unit)
        testSingle("...", SPREAD, Unit)
        testSingle("@", AT, Unit)
        testSingle("!", EXCL, Unit)
        testSingle("$", DOLLAR, Unit)
        testSingle("=", EQ, Unit)
    }

}