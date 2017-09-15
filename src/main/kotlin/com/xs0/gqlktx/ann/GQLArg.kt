package com.xs0.gqlktx.ann

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class GQLArg(
    /**
     * The name of the argument as exposed in GraphQL schema.
     */
    val value: String = "",
    /**
     * The default value for this argument (in GraphQL syntax). For example,
     * "null", "true", "123", "[ RED, BLUE ]", etc. Exclusive with required=true.
     */
    val defaultsTo: String = "",
    /**
     * Whether a value is required to be provided in the query. Exclusive with
     * defaultsTo.
     */
    val required: Boolean = false
)
