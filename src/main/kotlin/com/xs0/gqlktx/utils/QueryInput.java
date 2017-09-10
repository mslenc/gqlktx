package com.xs0.gqlktx.utils;

import io.vertx.core.json.JsonObject;

public class QueryInput {
    private final String query;
    private final JsonObject variables;
    private final String opName;
    private final boolean allowMutations;

    public QueryInput(String query, JsonObject variables, String opName, boolean allowMutations) {
        this.query = query;
        this.variables = variables;
        this.opName = opName;
        this.allowMutations = allowMutations;
    }

    public String getQuery() {
        return query;
    }

    public JsonObject getVariables() {
        return variables;
    }

    public String getOpName() {
        return opName;
    }

    public boolean getAllowMutations() {
        return allowMutations;
    }
}
