package com.xs0.gqlktx.schema.builder;

import com.xs0.gqlktx.ann.*;

import java.lang.annotation.Annotation;

@GraphQLEnum("__TypeKind")
public enum TypeKind {
    SCALAR(GraphQLScalar.class),
    ENUM(GraphQLEnum.class),

    INPUT_OBJECT(GraphQLInput.class),

    INTERFACE(GraphQLInterface.class),
    OBJECT(GraphQLObject.class),
    UNION(GraphQLUnion.class),

    NON_NULL(null),
    LIST(null);

    private final Class<? extends Annotation> explicitAnnotation;

    TypeKind(Class<? extends Annotation> explicitAnnotation) {
        this.explicitAnnotation = explicitAnnotation;
    }

    public Class<? extends Annotation> getExplicitAnnotation() {
        return explicitAnnotation;
    }
}
