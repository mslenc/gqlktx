package com.xs0.gqlktx.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static java.lang.Long.reverseBytes;

public class PackedIdListWriter {
    public enum Type {
        NULL,
        INTEGER,
        LONG,
        UUID,
        STRING
    }

    static final int TB_INT_ZERO = 0; // 0, no extra bytes
    static final int TB_INT_VAR = 1; // varint encoding
    static final int TB_INT_SIGNED_VAR = 2; // varint encoding with signed transformation
    static final int TB_INT_FIXED = 3; // 4-byte encoding, LSB first

    static final int TB_LONG_ZERO = 4; // 0L, no extra bytes
    static final int TB_LONG_VAR = 5; // varint encoding
    static final int TB_LONG_SIGNED_VAR = 6; // varint encoding with signed transformation
    static final int TB_LONG_FIXED = 7; // 8-byte encoding, LSB first

    static final int TB_BOOLEAN_TRUE = 8; // no extra bytes
    static final int TB_BOOLEAN_FALSE = 9; // no extra bytes

    static final int TB_FLOAT = 10; // 4 bytes, LSB first
    static final int TB_DOUBLE = 11; // 8 bytes, LSB first

    static final int TB_BYTE = 12; // 1 byte
    static final int TB_SHORT = 13; // 2 bytes, LSB first
    static final int TB_CHAR = 14; // 2 bytes, LSB first

    static final int TB_UUID = 64; // 16 bytes, big endian

    static final int TB_STRING_VARLEN = 191; // first, varint of (length - 64), then length bytes (UTF8)
    static final int TB_STRING_LEN0 = 192; // this and next 63; (implicit length + that many bytes)

    private final OutputStream out;

    public PackedIdListWriter(OutputStream out) {
        this.out = out;
    }

    private void writeVarint(int value) throws IOException {
        while (true) {
            if ((value & ~0x7F) == 0) {
                out.write(value);
                break;
            } else {
                out.write((value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
    }

    private void writeVarint(long value) throws IOException {
        while (true) {
            if ((value & ~0x7F) == 0) {
                out.write((int) value);
                break;
            } else {
                out.write((((int)value & 0x7F) | 0x80));
                value >>>= 7;
            }
        }
    }

    private void writeFixedInt(int value) throws IOException {
        out.write(value);
        out.write(value >> 8);
        out.write(value >> 16);
        out.write(value >> 24);
    }

    private void writeFixedLong(long value) throws IOException {
        writeFixedInt((int)value);
        writeFixedInt((int)(value >>> 32));
    }

    public void writeInt(int value) throws IOException {
        if (value < 0xfff00000 || value >= 0x200000) {
            out.write(TB_INT_FIXED);
            writeFixedInt(value);
            return;
        }

        if (value > 0) {
            out.write(TB_INT_VAR);
            writeVarint(value);
        } else
        if (value < 0) {
            out.write(TB_INT_SIGNED_VAR);
            writeVarint((value << 1) ^ (value >> 31));
        } else {
            out.write(TB_INT_ZERO);
        }
    }

    public void writeLong(long value) throws IOException {
        if (value < 0xffff000000000000L || value >= 0x2000000000000L) {
            out.write(TB_LONG_FIXED);
            writeFixedLong(value);
            return;
        }

        if (value > 0) {
            out.write(TB_LONG_VAR);
            writeVarint(value);
        } else
        if (value < 0) {
            out.write(TB_LONG_SIGNED_VAR);
            writeVarint((value << 1) ^ (value >> 63));
        } else {
            out.write(TB_LONG_ZERO);
        }
    }

    public void writeFloat(float value) throws IOException {
        out.write(TB_FLOAT);
        writeFixedInt(Float.floatToRawIntBits(value));
    }

    public void writeDouble(double value) throws IOException {
        out.write(TB_DOUBLE);
        writeFixedLong(Double.doubleToRawLongBits(value));
    }

    public void writeBoolean(boolean value) throws IOException {
        out.write(value ? TB_BOOLEAN_TRUE : TB_BOOLEAN_FALSE);
    }

    public void writeByte(byte value) throws IOException {
        out.write(TB_BYTE);
        out.write(value);
    }

    public void writeChar(char value) throws IOException {
        out.write(TB_CHAR);
        out.write(value);
        out.write(value >> 8);
    }

    public void writeShort(short value) throws IOException {
        out.write(TB_SHORT);
        out.write(value);
        out.write(value >> 8);
    }

    public void writeString(String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= 63) {
            int tag = TB_STRING_LEN0 + bytes.length;
            out.write(tag);
            out.write(bytes);
        } else {
            out.write(TB_STRING_VARLEN);
            writeVarint(bytes.length - 64);
            out.write(bytes);
        }
    }

    public void writeUUID(UUID value) throws IOException {
        // writeFixedLong is LSB-first, so we flip the bytes (rather than making another method)
        out.write(TB_UUID);
        writeFixedLong(reverseBytes(value.getMostSignificantBits()));
        writeFixedLong(reverseBytes(value.getLeastSignificantBits()));
    }
}
