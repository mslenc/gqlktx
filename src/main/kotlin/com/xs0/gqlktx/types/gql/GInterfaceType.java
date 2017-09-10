package com.xs0.gqlktx.types.gql;

import com.xs0.gqlktx.QueryException;
import com.xs0.gqlktx.schema.builder.TypeKind;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Map;
import java.util.Set;

public class GInterfaceType extends GFieldedType {
    private Set<GObjectType> implementations;

    public GInterfaceType(String name, Map<String, GField> fields) {
        super(name, fields);
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.INTERFACE;
    }

    @Override
    public boolean validAsArgumentType() {
        return false;
    }

    public void setImplementations(Set<GObjectType> implementations) {
        this.implementations = implementations;

        for (GObjectType objectType : implementations) {
            objectType.addInterface(this);
        }
    }

    public Set<GObjectType> getImplementations() {
        return implementations;
    }

    @Override
    public void coerceValue(JsonObject raw, String key, JsonObject out) throws QueryException {
        throw new QueryException("Interface type " + getName() + " can't be used as a variable");
    }

    @Override
    public void coerceValue(JsonArray raw, int index, JsonArray out) throws QueryException {
        throw new QueryException("Interface type " + getName() + " can't be used as a variable");
    }

    @Override
    public void toString(StringBuilder sb) {
        sb.append("interface ").append(getName()).append(" {\n");
        dumpFieldsToString(sb);
        sb.append("}\n");
    }
}
