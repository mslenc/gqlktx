package com.xs0.gqlktx.types.gql;

public abstract class GWrappingType extends GType {
    private final GType wrappedType;

    protected GWrappingType(GType wrappedType) {
        this.wrappedType = wrappedType;
    }

    public final GType getWrappedType() {
        return wrappedType;
    }

    @Override
    public boolean validAsArgumentType() {
        return wrappedType.validAsArgumentType();
    }

    @Override
    public boolean validAsQueryFieldType() {
        return wrappedType.validAsQueryFieldType();
    }

    @Override
    public GBaseType getBaseType() {
        return wrappedType.getBaseType();
    }
}
