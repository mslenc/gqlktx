package com.xs0.gqlktx

import kotlin.reflect.KCallable
import kotlin.reflect.KMutableProperty1

interface FieldSetter {
    val publicType: SemiType
    val name: String

    fun invoke(receiver: Any, argument: Any?)
}

class FieldSetterRegularFunction(
        override val publicType: SemiType,
        override val name: String,
        private val callable: KCallable<*>
): FieldSetter {
    override fun invoke(receiver: Any, argument: Any?) {
        callable.call(receiver, argument)
    }
}

class FieldSetterProperty(
        override val publicType: SemiType,
        override val name: String,
        private val property: KMutableProperty1<Any, Any?>
): FieldSetter {
    override fun invoke(receiver: Any, argument: Any?) {
        property.set(receiver, argument)
    }
}
