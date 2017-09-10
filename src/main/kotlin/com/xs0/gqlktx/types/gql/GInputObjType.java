package com.xs0.gqlktx.types.gql;

import com.xs0.gqlktx.QueryException;
import com.xs0.gqlktx.schema.builder.TypeKind;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashSet;
import java.util.Map;

public class GInputObjType extends GFieldedType {
    public GInputObjType(String name, Map<String, GField> fields) {
        super(name, fields);
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.INPUT_OBJECT;
    }

    @Override
    public boolean validAsArgumentType() {
        return true;
    }

    @Override
    public void coerceValue(JsonArray raw, int index, JsonArray out) throws QueryException {
        JsonObject rawObj;
        try {
            rawObj = raw.getJsonObject(index);
        } catch (ClassCastException e) {
            throw new QueryException("Expected a JSON object value for type " + getGqlTypeString());
        }

        if (rawObj == null) {
            out.addNull();
        } else {
            out.add(makeCoercedObj(rawObj));
        }
    }

    @Override
    public void coerceValue(JsonObject raw, String key, JsonObject out) throws QueryException {
        JsonObject rawObj;
        try {
            rawObj = raw.getJsonObject(key);
        } catch (ClassCastException e) {
            throw new QueryException("Expected a JSON object value for type " + getGqlTypeString());
        }

        if (rawObj == null) {
            out.putNull(key);
        } else {
            out.put(key, makeCoercedObj(rawObj));
        }
    }

    private JsonObject makeCoercedObj(JsonObject rawObj) throws QueryException {
        if (!getFields().keySet().containsAll(rawObj.fieldNames())) {
            HashSet<String> names = new HashSet<>(rawObj.fieldNames());
            names.removeAll(fields.keySet());

            throw new QueryException("Unknown field(s) in value of type " + getGqlTypeString() + ": " + names);
        }

        JsonObject coerced = new JsonObject();
        for (Map.Entry<String, GField> entry : fields.entrySet()) {
            entry.getValue().getType().coerceValue(rawObj, entry.getKey(), coerced);
        }
        return coerced;
    }

    @Override
    public void toString(StringBuilder sb) {
        sb.append("input ").append(getName()).append(" {\n");
        dumpFieldsToString(sb);
        sb.append("}\n");
    }
}
