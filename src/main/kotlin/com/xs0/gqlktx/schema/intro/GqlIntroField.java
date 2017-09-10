package com.xs0.gqlktx.schema.intro;

import com.xs0.gqlktx.ann.GraphQLObject;
import com.xs0.gqlktx.types.gql.GField;

import java.util.List;

@GraphQLObject("__Field")
public class GqlIntroField {
    private final GField field;

    public GqlIntroField(GField field) {
        this.field = field;
    }

    public String getName() {
        return field.getName();
    }

    public String getDescription() {
        return field.getDescription();
    }

    public List<GqlIntroInputValue> getArgs() {
        return field.getArgumentsForIntrospection();
    }

    public GqlIntroType getType() {
        return field.getType().introspector();
    }

    public boolean getIsDeprecated() {
        return false; // TODO
    }

    public String getDeprecationReason() {
        return null; // TODO
    }
}
