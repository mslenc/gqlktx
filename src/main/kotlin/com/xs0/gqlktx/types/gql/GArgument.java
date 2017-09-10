package com.xs0.gqlktx.types.gql;

import com.xs0.gqlktx.dom.Value;
import com.xs0.gqlktx.schema.intro.GqlIntroInputValue;

public class GArgument {
    private final String name;
    private final GType type;
    private final Value defaultValue;
    private final GqlIntroInputValue introspector;

    public GArgument(String name, GType type, Value defaultValue) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.introspector = new GqlIntroInputValue(this);
    }

    public GqlIntroInputValue introspector() {
        return introspector;
    }

    public String getName() {
        return name;
    }

    public GType getType() {
        return type;
    }

    public Value getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    public void toString(StringBuilder sb) {
        sb.append(name).append(": ").append(type.getGqlTypeString());
        if (defaultValue != null) {
            sb.append(" = ");
            defaultValue.toString(sb);
        }
    }
}
