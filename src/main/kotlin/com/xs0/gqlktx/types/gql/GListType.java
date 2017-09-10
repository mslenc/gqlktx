package com.xs0.gqlktx.types.gql;

import com.xs0.gqlktx.QueryException;
import com.xs0.gqlktx.schema.builder.TypeKind;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class GListType extends GWrappingType {
    public GListType(GType innerType) {
        super(innerType);
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.LIST;
    }

    @Override
    public String getGqlTypeString() {
        return "[" + getWrappedType().getGqlTypeString() + "]";
    }

    @Override
    public void coerceValue(JsonObject raw, String key, JsonObject out) throws QueryException {
        JsonArray rawList;
        try {
            rawList = raw.getJsonArray(key);
        } catch (ClassCastException e) {
            throw new QueryException("List type " + getGqlTypeString() + " needs a list value, but had something else");
        }

        if (rawList == null) {
            out.putNull(key);
        } else {
            JsonArray coerced = new JsonArray();
            for (int i = 0, n = rawList.size(); i < n; i++) {
                getWrappedType().coerceValue(rawList, i, coerced);
            }
            out.put(key, coerced);
        }
    }

    @Override
    public void coerceValue(JsonArray raw, int index, JsonArray out) throws QueryException {
        JsonArray rawList;
        try {
            rawList = raw.getJsonArray(index);
        } catch (ClassCastException e) {
            throw new QueryException("List type " + getGqlTypeString() + " needs a list value, but had something else");
        }

        if (rawList == null) {
            out.addNull();
        } else {
            JsonArray coerced = new JsonArray();
            for (int i = 0, n = rawList.size(); i < n; i++) {
                getWrappedType().coerceValue(rawList, i, coerced);
            }
            out.add(coerced);
        }
    }
}
