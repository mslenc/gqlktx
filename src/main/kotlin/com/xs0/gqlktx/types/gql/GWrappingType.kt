package com.xs0.gqlktx.types.gql

abstract class GWrappingType protected constructor(val wrappedType: GType) : GType() {

    override val validAsArgumentType: Boolean
        get() {
            return wrappedType.validAsArgumentType
        }

    override val validAsQueryFieldType: Boolean
        get() {
            return wrappedType.validAsQueryFieldType
        }

    override val baseType: GBaseType
        get() = wrappedType.baseType
}
