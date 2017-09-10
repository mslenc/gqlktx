package com.xs0.gqlktx.types.gql;

public abstract class GBaseType extends GType {
    private final String name;

    protected GBaseType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getGqlTypeString() {
        return name;
    }

    @Override
    public GBaseType getBaseType() {
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    public abstract void toString(StringBuilder sb);
}
