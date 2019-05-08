package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.QueryException
import com.xs0.gqlktx.schema.builder.TypeKind

class GScalarType(name: String, private val varValueValidator: (Any)->Any) : GValueType(name) {
    override val kind: TypeKind
        get() = TypeKind.SCALAR

    override fun coerceValue(raw: Any): Any {
        try {
            return varValueValidator(raw)
        } catch (e: Exception) {
            throw QueryException("Invalid value $raw for type $gqlTypeString")
        }
    }

    override fun toString(sb: StringBuilder) {
        sb.append("scalar ").append(name).append("\n")
    }
}
