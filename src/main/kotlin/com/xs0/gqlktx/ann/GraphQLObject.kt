package com.xs0.gqlktx.ann

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
annotation class GraphQLObject(val value: String = "", val implements: Array<KClass<*>> = arrayOf())
