package com.xs0.gqlktx.ann;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface GQLArg {
    /**
     * The name of the argument as exposed in GraphQL schema.
     */
    String value() default "";

    /**
     * The default value for this argument (in GraphQL syntax). For example,
     * "null", "true", "123", "[ RED, BLUE ]", etc. Exclusive with required=true.
     */
    String defaultsTo() default "";

    /**
     * Whether a value is required to be provided in the query. Exclusive with
     * defaultsTo.
     */
    boolean required() default false;


    GQLArg DEFAULTS = new GQLArg() {
        @Override
        public Class<? extends Annotation> annotationType() {
            return GQLArg.class;
        }

        @Override
        public String value() {
            return "";
        }

        @Override
        public String defaultsTo() {
            return "";
        }

        @Override
        public boolean required() {
            return false;
        }
    };
}
