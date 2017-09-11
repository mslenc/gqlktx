package com.xs0.gqlktx

import kotlin.reflect.KFunction
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1
import kotlin.reflect.KType

interface SyncInvokable<in CTX> {
    val type: KType
    val name: String
    val nullable: Boolean
    fun invoke(ctx: CTX): Any?
}

class SyncInvokableFunction<in CTX>(
        override val type: KType,
        override val name: String,
        override val nullable: Boolean,
        private val function: KFunction<*>,
        private val ignoreCtx: Boolean
) : SyncInvokable<CTX> {
    override fun invoke(ctx: CTX): Any? {
        return if (ignoreCtx) {
            function.call()
        } else {
            function.call(ctx)
        }
    }

    override fun toString(): String {
        return "InvokableFunction($function)"
    }
}

class SyncInvokableUnboundProperty<in CTX>(
        override val type: KType,
        override val name: String,
        override val nullable: Boolean,
        private val property: KProperty0<*>
) : SyncInvokable<CTX> {
    override fun invoke(ctx: CTX): Any? {
        return property.get()
    }

    override fun toString(): String {
        return "InvokableUnboundProperty($property)"
    }
}

class SyncInvokableBoundProperty<in CTX>(
        override val type: KType,
        override val name: String,
        override val nullable: Boolean,
        private val property: KProperty1<CTX, *>
) : SyncInvokable<CTX> {
    override fun invoke(ctx: CTX): Any? {
        return property.get(ctx)
    }

    override fun toString(): String {
        return "InvokableBoundProperty($property)"
    }
}