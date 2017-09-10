package com.xs0.gqlktx.utils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

import static com.xs0.gqlktx.utils.PackedIdListWriter.*;
import static java.lang.Long.reverseBytes;
import static java.nio.charset.StandardCharsets.UTF_8;

public class PackedIdListReader {
    private final InputStream input;
    private boolean eof;

    public PackedIdListReader(InputStream input) {
        this.input = input;
    }

    public Object readNext() throws IOException {
        if (eof)
            return null;

        int tag = input.read();
        if (tag < 0) {
            eof = true;
            return null;
        }

        switch (tag) {
            case TB_INT_ZERO:
                return 0;

            case TB_INT_VAR: // varint encoding
                return readRawVarint32();

            case TB_INT_SIGNED_VAR: // varint encoding with signed transformation
                int sval = readRawVarint32();
                return (sval >>> 1) ^ -(sval & 1);

            case TB_INT_FIXED: // 4-byte encoding, LSB first
                return readBytesAsInt(4);

            case TB_LONG_ZERO:
                return 0L;

            case TB_LONG_VAR: // varint encoding
                return readRawVarint64();

            case TB_LONG_SIGNED_VAR: // varint encoding with signed transformation
                long lval = readRawVarint64();
                return (lval >>> 1) ^ -(lval & 1);

            case TB_LONG_FIXED: // 8-byte encoding, LSB first
                return readBytesAsLong();

            case TB_BOOLEAN_TRUE:
                return true;
            case TB_BOOLEAN_FALSE:
                return false;

            case TB_FLOAT:  // 4 bytes, LSB first
                return Float.intBitsToFloat(readBytesAsInt(4));
            case TB_DOUBLE:  // 8 bytes, LSB first
                return Double.longBitsToDouble(readBytesAsLong());

            case TB_BYTE:
                return (byte)readBytesAsInt(1);
            case TB_SHORT:
                return (short)readBytesAsInt(2);
            case TB_CHAR:
                return (char)readBytesAsInt(2);
            case TB_UUID:
                long msb = reverseBytes(readBytesAsLong());
                long lsb = reverseBytes(readBytesAsLong());
                return new UUID(msb, lsb);

            case TB_STRING_VARLEN: // first, varint of (length - 64), then length bytes (UTF8)
                int len = readRawVarint32();
                if (len < 0 || len > Integer.MAX_VALUE - 64)
                    throw new IOException("Invalid string length");
                return readUTF8(len + 64);

            default:
                if (tag >= TB_STRING_LEN0)
                    return readUTF8(tag - TB_STRING_LEN0);

                throw new IOException("Unknown tag encountered");
        }
    }

    public Object[] readRest() throws IOException {
        ArrayList<Object> list = new ArrayList<>();
        while (true) {
            Object val = readNext();
            if (val != null) {
                list.add(val);
            } else {
                return list.toArray();
            }
        }
    }

    String readUTF8(int len) throws IOException {
        byte[] buff = new byte[len];

        int pos = 0;
        while (pos < len) {
            int read = input.read(buff, pos, len - pos);
            if (read <= 0)
                throw new EOFException("Incomplete string");
            pos += read;
        }

        return new String(buff, UTF_8);
    }

    private int readBytesAsInt(int len) throws IOException {
        int result = 0;
        int shift = 0;

        while (len-->0) {
            int b = input.read();
            if (b < 0)
                throw new EOFException("Unexpected end of data");
            result |= b << shift;
            shift += 8;
        }

        return result;
    }

    private long readBytesAsLong() throws IOException {
        long result = 0;
        int shift = 0;

        for (int len = 8; len > 0; len--) {
            long b = input.read();
            if (b < 0)
                throw new EOFException("Unexpected end of data");
            result |= b << shift;
            shift += 8;
        }

        return result;
    }

    private long readRawVarint64() throws IOException {
        long result = 0;
        for (int shift = 0; shift < 64; shift += 7) {
            int b = input.read();
            if (b < 0)
                throw new EOFException("Unexpected end of data");

            result |= (long) (b & 0x7F) << shift;
            if (b < 0x80) {
                return result;
            }
        }

        throw new IOException("Invalid varint");
    }

    private int readRawVarint32() throws IOException {
        long l = readRawVarint64();
        if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE)
            return (int)l;

        throw new IOException("Invalid varint - value exceeds 32 bits");
    }
}
