package com.xs0.gqlktx.types.gql

abstract class GValueType protected constructor(name: String) : GBaseType(name) {

    override val validAsArgumentType: Boolean
        get() {
            return true
        }

    override val validAsQueryFieldType: Boolean
        get() {
            return true
        }
}
