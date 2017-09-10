package com.xs0.gqlktx.parser

class CharStream(private val s: String) {

    private var pos = 0
    private var row = 1
    private var col = 1

    init {
        this.pos = 0
    }

    fun end(): Boolean {
        return pos >= s.length
    }

    fun get(): Int {
        var res = s.codePointAt(pos)
        pos += Character.charCount(res)

        // we normalize all of CR (\r), CR+LF (\r\n) and LF (\n) to LF
        if (res == CR || res == LF) {
            if (res == CR && pos < s.length && s.codePointAt(pos) == LF) {
                pos++
            }
            row++
            col = 1
            res = LF
        } else {
            col++
        }

        return res
    }

    fun peek(): Int {
        val res = s.codePointAt(pos)
        return if (res == CR) LF else res
    }

    fun consume(predicate: (Int)->Boolean): Int {
        return if (end() || !predicate(peek())) {
            -1
        } else {
            get()
        }
    }

    fun row(): Int {
        return row
    }

    fun col(): Int {
        return col
    }

    companion object {
        private val CR = 13
        private val LF = 10
    }
}
