package com.xs0.gqlktx.types.gql

abstract class GBaseType protected constructor(val name: String) : GType() {
    override val gqlTypeString = name

    override val baseType: GBaseType
        get() = this

    override fun toString(): String {
        var sb = StringBuilder()
        toString(sb)
        return sb.toString()
    }

    abstract fun toString(sb: StringBuilder)
}
