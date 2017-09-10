package com.xs0.gqlktx.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GraphQLField {
    /**
     * The name of the field as exposed in GraphQL schema.
     */
    String value() default "";
}
