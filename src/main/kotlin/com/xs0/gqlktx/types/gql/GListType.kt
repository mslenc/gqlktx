package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.QueryException
import com.xs0.gqlktx.schema.builder.TypeKind
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

class GListType(innerType: GType) : GWrappingType(innerType) {

    override val kind: TypeKind
        get() = TypeKind.LIST

    override val gqlTypeString: String
        get() = "[" + wrappedType.gqlTypeString + "]"

    override fun coerceValue(raw: JsonObject, key: String, out: JsonObject) {
        val rawList: JsonArray?
        try {
            rawList = raw.getJsonArray(key)
        } catch (e: ClassCastException) {
            throw QueryException("List type $gqlTypeString needs a list value, but had something else")
        }

        if (rawList == null) {
            out.putNull(key)
        } else {
            val coerced = JsonArray()
            var i = 0
            val n = rawList.size()
            while (i < n) {
                wrappedType.coerceValue(rawList, i, coerced)
                i++
            }
            out.put(key, coerced)
        }
    }

    override fun coerceValue(raw: JsonArray, index: Int, out: JsonArray) {
        val rawList: JsonArray?
        try {
            rawList = raw.getJsonArray(index)
        } catch (e: ClassCastException) {
            throw QueryException("List type $gqlTypeString needs a list value, but had something else")
        }

        if (rawList == null) {
            out.addNull()
        } else {
            val coerced = JsonArray()
            var i = 0
            val n = rawList.size()
            while (i < n) {
                wrappedType.coerceValue(rawList, i, coerced)
                i++
            }
            out.add(coerced)
        }
    }
}
