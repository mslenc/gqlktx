package com.xs0.gqlktx.codegen

import java.io.StringWriter
import java.io.Writer
import kotlin.reflect.KClass

class CodeWriter(private val w: Writer, private val packageName: String): Writer() {
    private val imports = HashSet<String>()
    private val lineBuf = StringBuilder()
    private var emptyLineEmitted = true
    private var braceDepth = 0

    fun start(): CodeWriter {
        appendLine("package $packageName")
        appendLine()
        return this
    }

    fun addImport(klass: KClass<*>) {
        addImport(klass.packageName() ?: throw IllegalStateException("Missing package name for $klass"), klass.simpleName ?: throw IllegalStateException("Missing class name for $klass"))
    }

    fun addImport(packageName: String, importName: String) {
        if (packageName != this.packageName) {
            val fullType = "${ packageName }.${ importName }"
            if (imports.add(fullType)) {
                write("import $fullType\n")
            }
        }
    }

    private fun lineIsAllWhiteSpace(): Boolean {
        for (i in lineBuf.indices) {
            if (!Character.isWhitespace(lineBuf[i])) {
                return false
            }
        }
        return true
    }

    private fun emitBuf() {
        val allWS = lineIsAllWhiteSpace()

        if (!allWS || (braceDepth == 0 && !emptyLineEmitted)) {
            w.append(lineBuf)
            emptyLineEmitted = allWS
        }

        lineBuf.clear()
    }

    override fun write(c: Int) {
        lineBuf.append(c.toChar())

        when (c.toChar()) {
            '{' -> braceDepth++
            '}' -> braceDepth--
            '\n' -> emitBuf()
        }
    }

    override fun write(cbuf: CharArray) {
        for (c in cbuf)
            write(c.code)
    }

    override fun write(cbuf: CharArray, off: Int, len: Int) {
        for (i in 0 until len)
            write(cbuf[off + i].code)
    }

    override fun write(str: String) {
        for (i in str.indices)
            write(str[i].code)
    }

    override fun write(str: String, off: Int, len: Int) {
        for (i in 0 until len)
            write(str[off + i].code)
    }

    override fun append(csq: CharSequence): Writer {
        for (i in csq.indices)
            write(csq[i].code)

        return this
    }

    override fun append(csq: CharSequence, start: Int, end: Int): Writer {
        for (i in start until end)
            write(csq[i].code)

        return this
    }

    override fun append(c: Char): Writer {
        write(c.code)
        return this
    }

    override fun close() {
        emitBuf()
        w.close()
    }

    override fun flush() {
        emitBuf()
        w.flush()
    }
}