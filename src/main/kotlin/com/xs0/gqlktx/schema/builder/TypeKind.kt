package com.xs0.gqlktx.schema.builder

import com.xs0.gqlktx.ann.*
import kotlin.reflect.KClass

@GraphQLEnum("__TypeKind")
enum class TypeKind(val explicitAnnotation: KClass<out Annotation>?) {
    SCALAR(GraphQLScalar::class),
    ENUM(GraphQLEnum::class),

    INPUT_OBJECT(GraphQLInput::class),

    INTERFACE(GraphQLInterface::class),
    OBJECT(GraphQLObject::class),
    UNION(GraphQLUnion::class),

    NON_NULL(null),
    LIST(null)
}
