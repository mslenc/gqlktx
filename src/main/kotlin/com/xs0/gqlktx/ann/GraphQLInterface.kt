package com.xs0.gqlktx.ann

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
annotation class GraphQLInterface(val value: String = "", val implementedBy: Array<KClass<*>> = arrayOf())
