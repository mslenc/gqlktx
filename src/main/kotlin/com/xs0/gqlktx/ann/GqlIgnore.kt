package com.xs0.gqlktx.ann

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.*

@Target(PROPERTY, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION)
@Retention(RUNTIME)
annotation class GqlIgnore