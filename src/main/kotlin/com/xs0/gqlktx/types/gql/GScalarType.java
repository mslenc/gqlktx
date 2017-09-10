package com.xs0.gqlktx.types.gql;

import com.xs0.gqlktx.GqlValueValidator;
import com.xs0.gqlktx.QueryException;
import com.xs0.gqlktx.schema.builder.TypeKind;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class GScalarType extends GValueType {
    private final GqlValueValidator varValueValidator;

    public GScalarType(String name, GqlValueValidator varValueValidator) {
        super(name);
        this.varValueValidator = varValueValidator;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.SCALAR;
    }

    @Override
    public void coerceValue(JsonObject raw, String key, JsonObject out) throws QueryException {
        if (raw.containsKey(key))
            out.put(key, coerce(raw.getValue(key)));
    }

    @Override
    public void coerceValue(JsonArray raw, int index, JsonArray out) throws QueryException {
        out.add(coerce(raw.getValue(index)));
    }

    private Object coerce(Object value) throws QueryException {
        if (value != null) {
            Object coerced;
            try {
                coerced = varValueValidator.validateAndNormalize(value);
            } catch (Exception e) {
                throw new QueryException("Invalid value " + value + " for type " + getGqlTypeString());
            }

            return coerced;
        } else {
            return null;
        }
    }

    @Override
    public void toString(StringBuilder sb) {
        sb.append("scalar ").append(getName()).append("\n");
    }
}
