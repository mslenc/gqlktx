package com.xs0.gqlktx;

public class SchemaException extends RuntimeException {
    public SchemaException(String msg) {
        super(msg);
    }

    public SchemaException(Throwable cause) {
        super(cause);
    }
}
