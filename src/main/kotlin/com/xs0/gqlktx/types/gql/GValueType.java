package com.xs0.gqlktx.types.gql;

public abstract class GValueType extends GBaseType {
    protected GValueType(String name) {
        super(name);
    }

    @Override
    public boolean validAsArgumentType() {
        return true;
    }

    @Override
    public boolean validAsQueryFieldType() {
        return true;
    }
}
