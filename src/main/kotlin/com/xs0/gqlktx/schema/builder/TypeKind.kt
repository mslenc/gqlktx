package com.xs0.gqlktx.schema.builder

import com.xs0.gqlktx.*
import kotlin.reflect.KClass

@GqlEnum("__TypeKind")
enum class TypeKind(val explicitAnnotation: KClass<out Annotation>?) {
    SCALAR(GqlScalar::class),
    ENUM(GqlEnum::class),

    INPUT_OBJECT(GqlInput::class),

    INTERFACE(GqlInterface::class),
    OBJECT(GqlObject::class),
    UNION(GqlUnion::class),

    NON_NULL(null),
    LIST(null)
}
