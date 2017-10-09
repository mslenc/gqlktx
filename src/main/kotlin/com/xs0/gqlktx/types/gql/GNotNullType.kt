package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.schema.builder.TypeKind

class GNotNullType(innerType: GType) : GWrappingType(innerType) {
    init {
        if (innerType is GNotNullType)
            throw IllegalArgumentException("Not null already asserted")
    }

    override val kind: TypeKind
        get() = TypeKind.NON_NULL

    override val gqlTypeString: String
        get() = wrappedType.gqlTypeString + "!"

    override fun coerceValue(raw: Any): Any {
        return wrappedType.coerceValue(raw)
    }
}
