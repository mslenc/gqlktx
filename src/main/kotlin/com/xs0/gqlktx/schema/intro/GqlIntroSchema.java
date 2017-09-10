package com.xs0.gqlktx.schema.intro;

import com.xs0.gqlktx.ann.GraphQLObject;
import com.xs0.gqlktx.schema.Schema;
import com.xs0.gqlktx.types.gql.GBaseType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.xs0.gqlktx.schema.intro.GqlIntroDirectiveLocation.FIELD;
import static com.xs0.gqlktx.schema.intro.GqlIntroDirectiveLocation.FRAGMENT_SPREAD;
import static com.xs0.gqlktx.schema.intro.GqlIntroDirectiveLocation.INLINE_FRAGMENT;
import static java.util.Collections.singletonList;

@GraphQLObject("__Schema")
public class GqlIntroSchema {
    private final Schema<?> schema;
    private final List<GqlIntroDirective> directives;

    public GqlIntroSchema(Schema<?> schema) {
        this.schema = schema;
        this.directives = buildDirectives();
    }

    private List<GqlIntroDirective> buildDirectives() {
        ArrayList<GqlIntroDirective> res = new ArrayList<>();

        GqlIntroType boolNotNull = schema.getGQLBaseType("Boolean").notNull().introspector();

        res.add(
            new GqlIntroDirective(
                "if",
                "Directs the executor to include this field or fragment only when the `if` argument is true.",
                Arrays.asList(FIELD, FRAGMENT_SPREAD, INLINE_FRAGMENT),
                singletonList(new GqlIntroInputValue("if", "Included when true.", boolNotNull, null))
            )
        );

        res.add(
            new GqlIntroDirective(
                "skip",
                "Directs the executor to skip this field or fragment when the `if` argument is true.",
                Arrays.asList(FIELD, FRAGMENT_SPREAD, INLINE_FRAGMENT),
                singletonList(new GqlIntroInputValue("if", "Skipped when true.", boolNotNull, null))
            )
        );

        return res;
    }

    public List<GqlIntroType> getTypes() {
        ArrayList<GqlIntroType> res = new ArrayList<>();

        for (GBaseType type : schema.getAllBaseTypes())
            res.add(type.introspector());

        return res;
    }

    public GqlIntroType getQueryType() {
        return schema.getJavaType(schema.getQueryRoot().getType()).getGqlType().introspector();
    }

    public GqlIntroType getMutationType() {
        if (schema.getMutationRoot() == null)
            return null;

        return schema.getJavaType(schema.getMutationRoot().getType()).getGqlType().introspector();
    }

    public GqlIntroType getSubscriptionType() {
        return null; // TODO
    }

    public List<GqlIntroDirective> getDirectives() {
        return directives;
    }

    public GqlIntroSchema self() {
        return this; // used for simple implementation
    }

    public GqlIntroType type(String name) {
        GBaseType baseType = schema.getGQLBaseType(name);
        return baseType != null ? baseType.introspector() : null;
    }
}
