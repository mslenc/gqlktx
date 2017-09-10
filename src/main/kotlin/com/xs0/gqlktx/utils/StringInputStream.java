package com.xs0.gqlktx.utils;

import java.io.InputStream;

public class StringInputStream extends InputStream {
    private final String s;
    private int pos;
    private int len;

    public StringInputStream(String s) {
        this.s = s;
        this.pos = 0;
        this.len = s.length();
    }

    @Override
    public int read() {
        if (pos >= len) {
            return -1;
        } else {
            return s.charAt(pos++) & 255;
        }
    }
}