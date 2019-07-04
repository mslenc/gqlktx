package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.dom.Value
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

    override fun coerceValue(raw: Value): Value {
        return wrappedType.coerceValue(raw)
    }
}
