package com.xs0.gqlktx;

import com.xs0.gqlktx.exec.FieldPath;
import com.xs0.gqlktx.parser.Token;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class FieldException extends GraphQLException {
    private final FieldPath path;
    private final Token<?> location;

    public FieldException(String message, FieldPath path, Token<?> location) {
        super(message);

        this.path = path;
        this.location = location;
    }

    public FieldPath getPath() {
        return path;
    }

    public Token<?> getLocation() {
        return location;
    }

    @Override
    public JsonObject toGraphQLError() {
        JsonObject error = super.toGraphQLError();

        if (path != null)
            error.put("path", path.toArray());

        if (location != null) {
            JsonObject loc = new JsonObject();
            loc.put("line", location.getRow());
            loc.put("column", location.getColumn());
            error.put("locations", new JsonArray().add(loc));
        }

        return error;
    }
}
