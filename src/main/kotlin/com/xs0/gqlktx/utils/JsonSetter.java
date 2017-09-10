package com.xs0.gqlktx.utils;

import java.time.Instant;
import java.util.Base64;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

public interface JsonSetter {
    static Object transform(Object value) {
        if (value == null)
            return null;
        if (value instanceof Enum)
            return ((Enum)value).name();
        if (value instanceof byte[])
            return Base64.getEncoder().encodeToString((byte[])value);
        if (value instanceof Instant)
            return ISO_INSTANT.format((Instant)value);
        return value;
    }
}
