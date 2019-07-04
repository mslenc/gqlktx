package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.QueryException
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.dom.ValueList
import com.xs0.gqlktx.dom.ValueNull
import com.xs0.gqlktx.dom.ValueOrNull
import com.xs0.gqlktx.schema.builder.TypeKind

class GListType(innerType: GType) : GWrappingType(innerType) {

    override val kind: TypeKind
        get() = TypeKind.LIST

    override val gqlTypeString: String
        get() = "[" + wrappedType.gqlTypeString + "]"

    override fun coerceValue(raw: Value): ValueList {
        if (raw !is ValueList)
            throw QueryException("List type $gqlTypeString needs a list value, but had something else")

        val coerced = ArrayList<ValueOrNull>()

        for (i in 0 until raw.elements.size) {
            val rawEl = raw.elements[i]

            if (rawEl is ValueNull) {
                if (wrappedType.kind == TypeKind.NON_NULL)
                    throw QueryException("Null value not allowed in this list")
                coerced.add(rawEl)
            } else {
                coerced.add(wrappedType.coerceValue(rawEl as Value))
            }
        }

        return ValueList(coerced)
    }
}
