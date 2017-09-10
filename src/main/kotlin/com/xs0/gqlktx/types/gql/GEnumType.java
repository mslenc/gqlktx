package com.xs0.gqlktx.types.gql;

import com.xs0.gqlktx.QueryException;
import com.xs0.gqlktx.schema.builder.TypeKind;
import com.xs0.gqlktx.schema.intro.GqlIntroEnumValue;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GEnumType extends GValueType {
    private final Set<String> values;

    public GEnumType(String name, Set<String> values) {
        super(name);

        this.values = values;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.ENUM;
    }

    public Set<String> getValues() {
        return values;
    }

    @Override
    public void coerceValue(JsonObject raw, String key, JsonObject out) throws QueryException {
        try {
            out.put(key, check(raw.getString(key)));
        } catch (ClassCastException e) {
            throw new QueryException("Expected an enum value (String)");
        }
    }

    @Override
    public void coerceValue(JsonArray raw, int index, JsonArray out) throws QueryException {
        try {
            out.add(check(raw.getString(index)));
        } catch (ClassCastException e) {
            throw new QueryException("Expected an enum value (String)");
        }
    }

    private String check(String string) throws QueryException {
        if (string == null || values.contains(string))
            return string;

        throw new QueryException("Invalid enum value (" + string + "). The possibilities are: " + values);
    }

    @Override
    public void toString(StringBuilder sb) {
        sb.append("enum ").append(getName()).append(" {\n");
        for (String val : values)
            sb.append("  ").append(val).append("\n");
        sb.append("}\n");
    }

    public List<GqlIntroEnumValue> getValuesForIntrospection(boolean includeDeprecated) {
        ArrayList<GqlIntroEnumValue> res = new ArrayList<>();
        for (String value : values)
            res.add(new GqlIntroEnumValue(value));
        return res;
    }
}
