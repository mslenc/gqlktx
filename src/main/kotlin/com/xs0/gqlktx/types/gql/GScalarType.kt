package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.QueryException
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.schema.builder.TypeKind

class GScalarType(name: String, private val varValueValidator: (Value)->Any, description: String?) : GValueType(name, description) {
    override val kind: TypeKind
        get() = TypeKind.SCALAR

    override fun coerceValue(raw: Value): Value {
        try {
            varValueValidator(raw)
        } catch (e: Exception) {
            throw QueryException("Invalid value $raw for type $gqlTypeString")
        }

        return raw
    }

    override fun toString(sb: StringBuilder) {
        sb.append("scalar ").append(name).append("\n")
    }
}
