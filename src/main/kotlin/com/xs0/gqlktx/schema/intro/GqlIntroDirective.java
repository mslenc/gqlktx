package com.xs0.gqlktx.schema.intro;

import si.datastat.graphql.schema.api.GraphQLObject;
import si.datastat.graphql.schema.api.Wrapped;

import java.util.List;

@GraphQLObject("__Directive")
public class GqlIntroDirective {
    private final String name;
    private final String description;
    private final List<GqlIntroDirectiveLocation> locations;
    private final List<GqlIntroInputValue> args;

    public GqlIntroDirective(String name, String description, List<GqlIntroDirectiveLocation> locations, List<GqlIntroInputValue> args) {
        this.name = name;
        this.description = description;
        this.locations = locations;
        this.args = args;
    }

    @Wrapped("T!")
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Wrapped("[T!]!")
    public List<GqlIntroDirectiveLocation> getLocations() {
        return locations;
    }

    @Wrapped("[T!]!")
    public List<GqlIntroInputValue> getArgs() {
        return args;
    }
}
