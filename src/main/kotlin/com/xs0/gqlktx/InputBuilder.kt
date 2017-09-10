package com.xs0.gqlktx

import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor



fun <T: Any> findInputFields(klass: KClass<T>): InputObjectBuilder<T> {
    if (klass.primaryConstructor != null) {
        klass.primaryConstructor.call()
    } else {

    }
}