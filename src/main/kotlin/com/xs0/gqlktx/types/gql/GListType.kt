package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.QueryException
import com.xs0.gqlktx.schema.builder.TypeKind
import io.vertx.core.json.JsonArray

class GListType(innerType: GType) : GWrappingType(innerType) {

    override val kind: TypeKind
        get() = TypeKind.LIST

    override val gqlTypeString: String
        get() = "[" + wrappedType.gqlTypeString + "]"

    override fun coerceValue(raw: Any): JsonArray {
        if (raw !is JsonArray)
            throw QueryException("List type $gqlTypeString needs a list value, but had something else")

        val coerced = JsonArray()

        for (i in 0 until raw.size()) {
            val rawEl: Any? = raw.getValue(i)
            if (rawEl == null) {
                if (wrappedType.kind == TypeKind.NON_NULL)
                    throw QueryException("Null value not allowed in this list")
                coerced.addNull()
            } else {
                coerced.add(wrappedType.coerceValue(rawEl))
            }
        }

        return coerced
    }
}
