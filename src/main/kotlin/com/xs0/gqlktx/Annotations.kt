package com.xs0.gqlktx

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.KClass

@Target(FUNCTION, PROPERTY, VALUE_PARAMETER, CONSTRUCTOR)
@Retention(RUNTIME)
annotation class GqlIgnore

@Retention(RUNTIME)
@Target(CLASS)
annotation class GqlScalar(
    val name: String = ""
)

@Retention(RUNTIME)
@Target(CLASS, FILE)
annotation class GqlEnum(
    val name: String = ""
)

@Target(PROPERTY, FUNCTION)
@Retention(RUNTIME)
annotation class GqlField(
    val name: String = ""
)

@Retention(AnnotationRetention.RUNTIME)
@Target(VALUE_PARAMETER)
annotation class GqlParam(
    /**
     * The name of the parameter as exposed in GraphQL schema.
     */
    val name: String = "",
    /**
     * The default value for this parameter (in GraphQL syntax). For example,
     * "null", "true", "123", "[ RED, BLUE ]", etc. Exclusive with required=true.
     */
    val defaultsTo: String = "",
    /**
     * Whether a value is required to be provided in the query. Exclusive with
     * defaultsTo.
     */
    val required: Boolean = false
)

@Retention(RUNTIME)
@Target(CLASS)
annotation class GqlObject(
    val name: String = "",
    val implements: Array<KClass<*>> = arrayOf()
)

@Retention(RUNTIME)
@Target(CLASS)
annotation class GqlInterface(
    val name: String = "",
    val implementedBy: Array<KClass<*>> = arrayOf()
)

@Retention(RUNTIME)
@Target(CLASS)
annotation class GqlUnion(
    val name: String = "",
    val implementedBy: Array<KClass<*>> = arrayOf()
)


@Retention(RUNTIME)
@Target(CLASS)
annotation class GqlInput(
    val name: String = "",
    val defaultsTo: String = "",
    val implementedBy: Array<KClass<*>> = arrayOf()
)

@Retention(RUNTIME)
@Target(CLASS)
annotation class GqlSchema(
    val name: String = ""
)

@Retention(RUNTIME)
@Target(FUNCTION, PROPERTY)
annotation class GqlMutationRoot

@Retention(RUNTIME)
@Target(FUNCTION, PROPERTY)
annotation class GqlQueryRoot
