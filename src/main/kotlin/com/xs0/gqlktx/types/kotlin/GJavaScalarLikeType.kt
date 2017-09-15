package com.xs0.gqlktx.types.kotlin

import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.types.gql.GNotNullType
import com.xs0.gqlktx.types.gql.GType
import kotlin.reflect.KType

abstract class GJavaScalarLikeType<CTX>(type: KType, gqlType: GType) : GJavaType<CTX>(type, gqlType) {
    init {
        when (gqlType.kind) {
            TypeKind.SCALAR, TypeKind.ENUM -> {
                // ok
            }

            TypeKind.NON_NULL -> {
                when ((gqlType as GNotNullType).wrappedType.kind) {
                    TypeKind.SCALAR, TypeKind.ENUM -> {
                        // ok
                    }

                    else ->
                        throw IllegalStateException("Expected a value type instead of " + gqlType.gqlTypeString)
                }

            }

            else ->
                throw IllegalStateException("Expected a value type instead of " + gqlType.gqlTypeString)
        }
    }

    override fun checkUsage(isInput: Boolean) {
        // ok - scalars can be both inputs and outputs
    }

    open fun toJson(result: Any): Any {
        return result
    }
}
