package com.xs0.gqlktx.types.gql

abstract class GWrappingType protected constructor(val wrappedType: GType) : GType() {
    override val baseType: GBaseType
        get() = wrappedType.baseType
}
