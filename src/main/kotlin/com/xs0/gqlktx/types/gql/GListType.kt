package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.QueryException
import com.xs0.gqlktx.schema.builder.TypeKind

class GListType(innerType: GType) : GWrappingType(innerType) {

    override val kind: TypeKind
        get() = TypeKind.LIST

    override val gqlTypeString: String
        get() = "[" + wrappedType.gqlTypeString + "]"

    override fun coerceValue(raw: Any): List<Any?> {
        if (raw !is List<*>)
            throw QueryException("List type $gqlTypeString needs a list value, but had something else")

        val coerced = ArrayList<Any?>()

        for (i in 0 until raw.size) {
            val rawEl: Any? = raw[i]
            if (rawEl == null) {
                if (wrappedType.kind == TypeKind.NON_NULL)
                    throw QueryException("Null value not allowed in this list")
                coerced.add(null)
            } else {
                coerced.add(wrappedType.coerceValue(rawEl))
            }
        }

        return coerced
    }
}
