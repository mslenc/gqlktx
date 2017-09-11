package com.xs0.gqlktx.ann

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class GraphQLInput(val value: String = "", val defaultsTo: String = "")
