package com.xs0.gqlktx.types.gql;

import com.xs0.gqlktx.schema.intro.GqlIntroInputValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GField {
    private final String name;
    private final GType type;
    private final Map<String, GArgument> arguments;

    public GField(String name, GType type, Map<String, GArgument> arguments) {
        this.name = name;
        this.type = type;
        this.arguments = arguments;
    }

    public String getName() {
        return name;
    }

    public GType getType() {
        return type;
    }

    public Map<String, GArgument> getArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    public void toString(StringBuilder sb) {
        sb.append(name);
        if (!arguments.isEmpty()) {
            sb.append("(");
            boolean first = true;
            for (GArgument arg : arguments.values()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                arg.toString(sb);
            }
            sb.append(")");
        }
        sb.append(": ").append(type.getGqlTypeString());
    }

    public String getDescription() {
        return null; // TODO
    }

    public List<GqlIntroInputValue> getArgumentsForIntrospection() {
        ArrayList<GqlIntroInputValue> res = new ArrayList<>(arguments.size());
        for (GArgument arg : arguments.values()) {
            res.add(arg.introspector());
        }
        return res;
    }
}
