package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.QueryException
import com.xs0.gqlktx.schema.builder.TypeKind
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

class GNotNullType(innerType: GType) : GWrappingType(innerType) {
    init {
        if (innerType is GNotNullType)
            throw IllegalArgumentException("Not null already asserted")
    }

    override val kind: TypeKind
        get() = TypeKind.NON_NULL

    override val gqlTypeString: String
        get() = wrappedType.gqlTypeString + "!"

    override fun coerceValue(raw: JsonObject, key: String, out: JsonObject) {
        wrappedType.coerceValue(raw, key, out)
        if (out.getValue(key) == null)
            throw QueryException("Non-null type $gqlTypeString can't have null as a value")
    }

    override fun coerceValue(raw: JsonArray, index: Int, out: JsonArray) {
        wrappedType.coerceValue(raw, index, out)
        if (out.getValue(index) == null)
            throw QueryException("Non-null type $gqlTypeString can't have null as a value")
    }
}
