package com.xs0.gqlktx.utils;

import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;

public class StringOutputStream extends OutputStream {
    private final StringBuilder sb;

    public StringOutputStream() {
        sb = new StringBuilder();
    }

    public StringOutputStream(StringBuilder sb) {
        this.sb = sb;
    }

    public StringBuilder getStringBuilder() {
        return sb;
    }

    @Override
    public String toString() {
        return sb.toString();
    }

    @Override
    public void write(int b) {
        sb.append((char)(b & 255));
    }

    @Override
    public void write(@NotNull byte[] bytes) {
        for (byte b : bytes)
            sb.append((char)(b & 255));
    }

    @Override
    public void write(@NotNull byte[] b, int off, int len) {
        while (len-->0)
            sb.append((char)(b[off++] & 255));
    }
}
