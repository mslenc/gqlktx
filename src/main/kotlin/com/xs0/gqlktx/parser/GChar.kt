package com.xs0.gqlktx.parser

internal inline fun ascii(c: Int): Char {
    return if (c in 0..127) c.toChar() else 0.toChar()
}

internal object GChar {


    @JvmStatic
    fun isDot(c: Int): Boolean {
        return ascii(c) == '.'
    }

    @JvmStatic
    fun isValidEscapeFirstChar(c: Int): Boolean {
        return when (ascii(c)) {
            '\\', '"', '/', 'b', 'f', 'n', 'r', 't', 'u' -> true
            else -> false
        }
    }

    @JvmStatic
    fun isStartLineComment(c: Int): Boolean {
        return ascii(c) == '#'
    }

    @JvmStatic
    fun isDigit(c: Int): Boolean {
        return ascii(c) in '0'..'9'
    }

    @JvmStatic
    fun isHex(c: Int): Boolean {
        val cc = ascii(c)
        return (cc in '0'..'9') ||
               (cc in 'a'..'f') ||
               (cc in 'A'..'F')
    }

    @JvmStatic
    fun isIgnored(c: Int): Boolean {
        return isComma(c) || isWhiteSpace(c) || isLineTerminator(c)
    }

    @JvmStatic
    fun isComma(c: Int): Boolean {
        return ascii(c) == ','
    }

    @JvmStatic
    fun isWhiteSpace(c: Int): Boolean {
        return when (ascii(c)) {
            ' ', '\t', '\u000c', '\u000b', '\u00a0' -> true
            else -> false
        }
    }

    @JvmStatic
    fun isLineTerminator(c: Int): Boolean {
        return when (ascii(c)) {
            '\n', '\r' -> true
            else -> false
        }
    }

    @JvmStatic
    fun isExponentStart(c: Int): Boolean {
        return c == 'e'.toInt() || c == 'E'.toInt()
    }

    @JvmStatic
    fun isPlusOrMinus(c: Int): Boolean {
        return c == '+'.toInt() || c == '-'.toInt()
    }

    @JvmStatic
    fun isNameStart(c: Int): Boolean {
        val cc = ascii(c)
        return cc in 'a'..'z' ||
               cc in 'A'..'Z' ||
               cc == '_'
    }

    @JvmStatic
    fun isNamePart(c: Int): Boolean {
        val cc = ascii(c)
        return cc in 'a'..'z' ||
               cc in 'A'..'Z' ||
               cc in '0'..'9' ||
               cc == '_'
    }
}
