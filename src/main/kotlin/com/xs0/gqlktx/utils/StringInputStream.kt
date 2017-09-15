package com.xs0.gqlktx.utils

import java.io.InputStream

class StringInputStream(private val s: String) : InputStream() {
    private var pos: Int = 0
    private val len: Int = s.length

    override fun read(): Int {
        return if (pos >= len) {
            -1
        } else {
            s[pos++].toInt() and 255
        }
    }
}