package com.xs0.gqlktx

import io.vertx.core.Future
import io.vertx.core.Handler
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf

enum class SemiTypeKind {
    COLLECTION_OF,
    ARRAY_OF,
    PRIMITIVE_ARRAY,
    OBJECT
}

private val primitiveArrayTypes = setOf(
    ByteArray::class, ShortArray::class, IntArray::class, LongArray::class,
    FloatArray::class, DoubleArray::class, BooleanArray::class, CharArray::class
)

private val ignoredTypes = listOf(
    Handler::class, Future::class, CompletableFuture::class,
    java.util.concurrent.Future::class
)

class SemiType(
    val nullable: Boolean,
    val kind: SemiTypeKind,
    val inner: SemiType?,
    val klass: KClass<*>?,
    val sourceType: KType
) {

    companion object {
        fun create(type: KType): SemiType? {
            val nullable = type.isMarkedNullable

            if (type.arguments.size == 1) {
                val container = type.classifier as? KClass<*> ?: return null

                if (container.java.isArray) {
                    // Array<X>
                    val innerType = create(type.arguments[0].type ?: return null) ?: return null
                    return SemiType(nullable, SemiTypeKind.ARRAY_OF, innerType, container, type)
                } else
                    if (container.isSubclassOf(Collection::class)) {
                        val innerType = create(type.arguments[0].type ?: return null) ?: return null
                        return SemiType(nullable, SemiTypeKind.COLLECTION_OF, innerType, container, type)
                    } else {
                        return null
                    }
            } else
            if (type.arguments.isEmpty()) {
                val klass = type.classifier as? KClass<*> ?: return null

                return when {
                    klass in primitiveArrayTypes ->
                        SemiType(nullable, SemiTypeKind.PRIMITIVE_ARRAY, null, klass, type)

                    klass != Any::class -> {
                        for (ignored in ignoredTypes)
                            if (ignored.isSuperclassOf(klass))
                                return null

                        SemiType(nullable, SemiTypeKind.OBJECT, null, klass, type)
                    }

                    else ->
                        null
                }
            } else {
                return null
            }
        }
    }

    val isBoolean: Boolean
        get() = kind == SemiTypeKind.OBJECT && klass == Boolean::class

    fun withNullable(): SemiType {
        if (nullable)
            return this

        return SemiType(true, kind, inner, klass, sourceType)
    }

    override fun toString(): String {
        if (inner == null) {
            return "$kind($klass)${ if (nullable) "?" else "" }"
        } else {
            return "$kind($inner)${ if (nullable) "?" else "" }"
        }
    }
}