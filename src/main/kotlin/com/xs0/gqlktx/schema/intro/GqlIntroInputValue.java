package com.xs0.gqlktx.schema.intro;

import com.xs0.gqlktx.ann.GraphQLObject;
import com.xs0.gqlktx.types.gql.GArgument;
import com.xs0.gqlktx.types.gql.GField;

@GraphQLObject("__InputValue")
public class GqlIntroInputValue {
    private final String name;
    private final String description;
    private final GqlIntroType type;
    private final String defaultValue;

    public GqlIntroInputValue(String name, String description, GqlIntroType type, String defaultValue) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public GqlIntroInputValue(GArgument arg) {
        this.name = arg.getName();
        this.description = null; // TODO
        this.type = arg.getType().introspector();
        this.defaultValue = arg.getDefaultValue() != null ? arg.getDefaultValue().toString() : null;
    }

    public GqlIntroInputValue(GField value) {
        this.name = value.getName();
        this.description = null; // TODO
        this.type = value.getType().introspector();
        this.defaultValue = null;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public GqlIntroType getType() {
        return type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
