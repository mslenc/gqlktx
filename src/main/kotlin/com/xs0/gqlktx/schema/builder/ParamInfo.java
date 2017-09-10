package com.xs0.gqlktx.schema.builder;

import com.xs0.gqlktx.dom.Value;

import java.lang.reflect.Type;

public final class ParamInfo {
    public enum ParamKind {
        PUBLIC,
        IGNORED,
        QUERY_CTX,
        ASYNC_HANDLER
    }

    private final String name;
    private final ParamKind kind;
    private final Value defaultValue;
    private final Object parsedDefault;
    private final Object ignoredValue;
    private final Type type;

    public ParamInfo(String name, ParamKind kind, Value defaultValue, Object parsedDefault, Type type, Object ignoredValue) {
        this.name = name;
        this.kind = kind;
        this.defaultValue = defaultValue;
        this.parsedDefault = parsedDefault;
        this.type = type;
        this.ignoredValue = ignoredValue;
    }

    public static ParamInfo ignored(Object ignoredValue) {
        return new ParamInfo(null, ParamKind.IGNORED, null,null, null, ignoredValue);
    }

    public static ParamInfo queryCtx() {
        return new ParamInfo(null, ParamKind.QUERY_CTX, null, null, null, null);
    }

    public static ParamInfo asyncHandler(Type asyncResType) {
        return new ParamInfo(null, ParamKind.ASYNC_HANDLER, null, null, asyncResType, null);
    }

    public static ParamInfo publicParam(String name, Type type, Value defaultValue, Object parsedDefault) {
        if ((defaultValue == null) != (parsedDefault == null))
            throw new IllegalArgumentException("Both default value forms must be present, or neither");

        return new ParamInfo(name, ParamKind.PUBLIC, defaultValue, parsedDefault, type, null);
    }

    public static ParamInfo valueForSetter(Type type) {
        return new ParamInfo(null, ParamKind.PUBLIC, null, null, type, null);
    }

    /**
     * For PUBLIC parameters, returns the name of the parameter (as exposed via GraphQL).
     * Otherwise returns null.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the {@link ParamKind kind} of this parameter.
     */
    public ParamKind getKind() {
        return kind;
    }

    /**
     * For PUBLIC parameters, returns the default value, if it exists.
     * Otherwise returns null.
     */
    public Value getDefaultValue() {
        return defaultValue;
    }

    /**
     * For PUBLIC parameters, returns the default value, if it exists, in parsed form.
     * Otherwise returns null.
     */
    public Object getParsedDefault() {
        return parsedDefault;
    }

    /**
     * For IGNORED parameters, returns the value to use for successfully ignoring the
     * parameter (null, false or (type-correct) 0).
     * Otherwise returns null.
     */
    public Object getIgnoredValue() {
        return ignoredValue;
    }

    /**
     * For PUBLIC parameters, returns the type of the parameter.
     * For ASYNC_HANDLER parameters, returns the type of the return value (but where
     * the actual parameter type is Handler&lt;AsyncResult&lt;T&gt;&gt;)
     * Otherwise returns null.
     */
    public Type getType() {
        return type;
    }
}