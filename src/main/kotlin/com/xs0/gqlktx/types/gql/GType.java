package com.xs0.gqlktx.types.gql;

import com.xs0.gqlktx.QueryException;
import com.xs0.gqlktx.schema.builder.TypeKind;
import com.xs0.gqlktx.schema.intro.GqlIntroType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public abstract class GType {
    private final GqlIntroType introspector;

    protected GType() {
        this.introspector = new GqlIntroType(this);
    }

    public GqlIntroType introspector() {
        return introspector;
    }

    public abstract TypeKind getKind();
    public abstract String getGqlTypeString();

    public abstract boolean validAsArgumentType();
    public abstract boolean validAsQueryFieldType();

    public abstract GBaseType getBaseType();

    private GNotNullType myNotNullType;
    public GNotNullType notNull() {
        if (myNotNullType == null)
            myNotNullType = new GNotNullType(this);

        return myNotNullType;
    }

    private GListType myListType;
    public GListType listOf() {
        if (myListType == null)
            myListType = new GListType(this);

        return myListType;
    }

    public abstract void coerceValue(JsonObject raw, String key, JsonObject out) throws QueryException;
    public abstract void coerceValue(JsonArray raw, int index, JsonArray out) throws QueryException;
}
