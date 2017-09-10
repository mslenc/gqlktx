package com.xs0.gqlktx.schema.intro;

import si.datastat.graphql.schema.api.GraphQLObject;
import si.datastat.graphql.schema.api.Wrapped;

@GraphQLObject("__EnumValue")
public class GqlIntroEnumValue {
    private final String name;

    public GqlIntroEnumValue(String name) {
        this.name = name;
    }

    @Wrapped("T!")
    public String getName() {
        return name;
    }

    public String getDescription() {
        return null; // TODO
    }

    public boolean getIsDeprecated() {
        return false; // TODO
    }

    public String getDeprecationReason() {
        return null;
    }
}
