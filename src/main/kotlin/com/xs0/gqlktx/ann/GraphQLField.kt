package com.xs0.gqlktx.ann

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class GraphQLField(val value: String = "")
