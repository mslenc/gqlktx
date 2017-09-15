package com.xs0.gqlktx.parser

import com.xs0.gqlktx.ParseException
import com.xs0.gqlktx.parser.Token.Type.*

import com.xs0.gqlktx.parser.Token.Type.EOF

class GraphQLTokenizer(private val chars: CharStream) {
    private var peek: Token<*>? = null

    operator fun <T> next(): Token<T> {
        try {
            return peek()
        } finally {
            peek = null
        }
    }

    fun <T> peek(): Token<T> {
        if (peek == null) {
            peek = findNextToken()
        }
        return peek as Token<T>
    }

    private fun findNextToken(): Token<*> {
        var c: Int
        var row: Int
        var col: Int
        while (true) { // skip ignored stuff
            if (chars.end())
                return Token(chars.row(), chars.col(), EOF, "", Unit)

            row = chars.row()
            col = chars.col()
            c = chars.get()
            if (GChar.isIgnored(c))
                continue

            if (GChar.isStartLineComment(c)) {
                while (!chars.end()) {
                    c = chars.get()
                    if (GChar.isLineTerminator(c))
                    // we'd properly have to peek, but we also know we'll ignore it anyway
                        break
                }
                continue
            }

            break
        }

        when (ascii(c)) {
            '(' -> return Token(row, col, LPAREN, "(", Unit)
            ')' -> return Token(row, col, RPAREN, ")", Unit)
            '{' -> return Token(row, col, LCURLY, "{", Unit)
            '}' -> return Token(row, col, RCURLY, "}", Unit)
            '|' -> return Token(row, col, PIPE, "|", Unit)
            ':' -> return Token(row, col, COLON, ":", Unit)
            '@' -> return Token(row, col, AT, "@", Unit)
            '!' -> return Token(row, col, EXCL, "!", Unit)
            '$' -> return Token(row, col, DOLLAR, "$", Unit)
            '=' -> return Token(row, col, EQ, "=", Unit)
            '[' -> return Token(row, col, LBRACK, "[", Unit)
            ']' -> return Token(row, col, RBRACK, "]", Unit)
            '.' -> return processSpread(row, col)
            '"' -> return processString(row, col)

            '-', '+' -> return processNumber(row, col, c, 0)
        }

        if (GChar.isDigit(c)) {
            return processNumber(row, col, 0, c)
        }

        if (GChar.isNameStart(c)) {
            return processName(row, col, c)
        }

        throw ParseException("Illegal character '" + c.toChar() + "' found", row, col)
    }

    private fun processName(row: Int, col: Int, firstChar: Int): Token<String> {
        val sb = StringBuilder()
        sb.appendCodePoint(firstChar)

        while (!chars.end()) {
            val c = chars.consume { c: Int -> GChar.isNamePart(c) }
            if (c > 0) {
                sb.appendCodePoint(c)
            } else {
                break
            }
        }

        val name = sb.toString()

        return Token(row, col, NAME, name, name)
    }

    @Throws(ParseException::class)
    private fun processNumber(row: Int, col: Int, signChar: Int, firstDigit: Int): Token<Number> {
        var firstDigit = firstDigit
        val raw = StringBuilder()
        val `val` = StringBuilder()

        if (signChar > 0)
            raw.appendCodePoint(signChar)
        if (signChar == '-'.toInt())
            `val`.appendCodePoint(signChar)

        var hadDigits = false
        var hadZeroFirst = false
        var isFloat = false

        while (firstDigit > 0 || !chars.end()) {
            val d = if (firstDigit > 0) firstDigit else chars.consume { c: Int -> GChar.isDigit(c) }
            firstDigit = 0
            if (d > 0) {
                raw.appendCodePoint(d)
                `val`.appendCodePoint(d)

                if (hadZeroFirst)
                    throw ParseException("Numbers can't start with 0", row, col)

                if (d == '0'.toInt() && !hadDigits)
                    hadZeroFirst = true

                hadDigits = true
            } else {
                break
            }
        }

        if (!hadDigits)
            throw ParseException("Expected a number after " + signChar.toChar(), row, col)

        if (chars.consume { c: Int -> GChar.isDot(c) } > 0) {
            raw.append('.')
            `val`.append('.')
            isFloat = true
            hadDigits = false
            while (!chars.end()) {
                val d = chars.consume { c: Int -> GChar.isDigit(c) }
                if (d > 0) {
                    raw.appendCodePoint(d)
                    `val`.appendCodePoint(d)
                    hadDigits = true
                } else {
                    break
                }
            }
        }

        if (!hadDigits)
            throw ParseException("Expected a number after .", row, col)

        var e = chars.consume { c: Int -> GChar.isExponentStart(c) }
        if (e > 0) {
            raw.appendCodePoint(e)
            `val`.appendCodePoint(e)
            isFloat = true
            hadDigits = false

            val pm = chars.consume { c: Int -> GChar.isPlusOrMinus(c) }
            if (pm > 0) {
                raw.appendCodePoint(pm)
                `val`.appendCodePoint(pm)
                e = pm // for error message below
            }

            while (!chars.end()) {
                val d = chars.consume { c: Int -> GChar.isDigit(c) }
                if (d > 0) {
                    raw.appendCodePoint(d)
                    `val`.appendCodePoint(d)
                    hadDigits = true
                } else {
                    break
                }
            }
        }

        if (!hadDigits)
            throw ParseException("Expected a number after " + e.toChar(), row, col)

        try {
            if (isFloat) {
                val d = java.lang.Double.valueOf(`val`.toString())
                if (d.isInfinite())
                    throw ParseException("Infinity not allowed", row, col)
                if (d.isNaN())
                    throw ParseException("NaN not allowed", row, col)

                return Token(row, col, FLOAT, raw.toString(), d)
            } else {
                val l = java.lang.Long.parseLong(`val`.toString())
                return if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                    Token(row, col, INTEGER, raw.toString(), l.toInt())
                } else {
                    Token(row, col, LONG, raw.toString(), l)
                }
            }
        } catch (ex: NumberFormatException) {
            throw ParseException("Failed to parse number \"" + raw + "\": " + ex.message, row, col)
        }

    }

    @Throws(ParseException::class)
    private fun processSpread(row: Int, col: Int): Token<Unit> {
        val a = chars.consume { c: Int -> GChar.isDot(c) }
        val b = chars.consume { c: Int -> GChar.isDot(c) }

        if (a < 0 || b < 0)
            throw ParseException("Incomplete spread operator ...", row, col)

        return Token(row, col, SPREAD, "...", Unit)
    }

    @Throws(ParseException::class)
    private fun processString(row: Int, col: Int): Token<String> {
        val raw = StringBuilder().append('"')
        val value = StringBuilder()

        nextChar@
        while (true) {
            val cRow = chars.row()
            val cCol = chars.col()

            if (chars.end())
                throw ParseException("Unterminated string at EOF", cRow, cCol)

            val c = chars.get()
            if (GChar.isLineTerminator(c))
                throw ParseException("Line terminators not allowed in strings", cRow, cCol)

            raw.appendCodePoint(c)

            if (c == '"'.toInt()) {
                return Token(row, col, STRING, raw.toString(), value.toString())
            } else if (c == '\\'.toInt()) {
                val esc = chars.consume { GChar.isValidEscapeFirstChar(it) }
                if (esc < 0) {
                    throw ParseException("Invalid escape sequence", cRow, cCol)
                } else {
                    raw.appendCodePoint(esc)
                    when (ascii(esc)) {
                        '\\' -> {
                            value.append('\\')
                            continue@nextChar
                        }
                        '"' -> {
                            value.append('"')
                            continue@nextChar
                        }
                        '/' -> {
                            value.append('/')
                            continue@nextChar
                        }
                        'b' -> {
                            value.append('\b')
                            continue@nextChar
                        }
                        'f' -> {
                            value.append('\u000c')
                            continue@nextChar
                        }
                        'n' -> {
                            value.append('\n')
                            continue@nextChar
                        }
                        'r' -> {
                            value.append('\r')
                            continue@nextChar
                        }
                        't' -> {
                            value.append('\t')
                            continue@nextChar
                        }
                        'u' -> {
                            // below
                        }
                        else -> throw IllegalStateException("Mismatch between isValidEscapeFirstChar and this switch")
                    }

                    var uni = 0
                    for (i in 0..3) {
                        val hex = chars.consume { GChar.isHex(it) }
                        if (hex < 0)
                            throw ParseException("Invalid unicode escape sequence", cRow, cCol)
                        uni = uni shl 4 or Character.getNumericValue(hex)
                    }
                    value.appendCodePoint(uni)
                }
            } else {
                value.appendCodePoint(c)
            }
        }
    }
}
