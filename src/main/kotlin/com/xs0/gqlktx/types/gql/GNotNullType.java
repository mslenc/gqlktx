package com.xs0.gqlktx.types.gql;

import com.xs0.gqlktx.QueryException;
import com.xs0.gqlktx.schema.builder.TypeKind;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class GNotNullType extends GWrappingType {
    public GNotNullType(GType innerType) {
        super(innerType);

        if (innerType instanceof GNotNullType)
            throw new IllegalArgumentException("Not null already asserted");
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.NON_NULL;
    }

    @Override
    public String getGqlTypeString() {
        return getWrappedType().getGqlTypeString() + "!";
    }

    @Override
    public void coerceValue(JsonObject raw, String key, JsonObject out) throws QueryException {
        getWrappedType().coerceValue(raw, key, out);
        if (out.getValue(key) == null)
            throw new QueryException("Non-null type " + getGqlTypeString() + " can't have null as a value");
    }

    @Override
    public void coerceValue(JsonArray raw, int index, JsonArray out) throws QueryException {
        getWrappedType().coerceValue(raw, index, out);
        if (out.getValue(index) == null)
            throw new QueryException("Non-null type " + getGqlTypeString() + " can't have null as a value");
    }
}
