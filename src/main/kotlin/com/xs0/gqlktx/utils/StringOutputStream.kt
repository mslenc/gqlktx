package com.xs0.gqlktx.utils

import java.io.OutputStream

class StringOutputStream : OutputStream {
    val stringBuilder: StringBuilder

    constructor() {
        stringBuilder = StringBuilder()
    }

    constructor(sb: StringBuilder) {
        this.stringBuilder = sb
    }

    override fun toString(): String {
        return stringBuilder.toString()
    }

    override fun write(b: Int) {
        stringBuilder.append((b and 255).toChar())
    }

    override fun write(bytes: ByteArray) {
        for (b in bytes)
            stringBuilder.append((b.toInt() and 255).toChar())
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        var offset = off
        for (i in 1..len)
            stringBuilder.append((b[offset++].toInt() and 255).toChar())
    }
}
