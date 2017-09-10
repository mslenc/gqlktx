package com.xs0.gqlktx.exec;

import io.vertx.core.json.JsonArray;

public class FieldPath {
    private final FieldPath parent;
    private final String fieldName;
    private final int listIndex;

    private FieldPath(FieldPath parent, String fieldName, int listIndex) {
        this.parent = parent;
        this.fieldName = fieldName;
        this.listIndex = listIndex;
    }

    public static FieldPath root() {
        return new FieldPath(null, null, 0);
    }

    public FieldPath subField(String fieldName) {
        return new FieldPath(this, fieldName, 0);
    }

    public FieldPath listElement(int listIndex) {
        return new FieldPath(this, null, listIndex);
    }

    public JsonArray toArray() {
        JsonArray result = new JsonArray();
        toArray(result);
        return result;
    }

    private void toArray(JsonArray result) {
        if (parent != null) {
            parent.toArray(result);
            if (fieldName != null) {
                result.add(fieldName);
            } else {
                result.add(listIndex);
            }
        }
    }

    @Override
    public String toString() {
        return toArray().toString();
    }

    public boolean isRoot() {
        return parent == null;
    }
}
