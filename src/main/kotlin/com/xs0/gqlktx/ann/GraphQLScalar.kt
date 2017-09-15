package com.xs0.gqlktx.ann

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
annotation class GraphQLScalar(val value: String = "")
