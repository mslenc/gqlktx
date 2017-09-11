package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.QueryException
import com.xs0.gqlktx.schema.builder.TypeKind
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

class GScalarType(name: String, private val varValueValidator: (Any)->Any) : GValueType(name) {

    override fun getKind(): TypeKind {
        return TypeKind.SCALAR
    }

    @Throws(QueryException::class)
    override fun coerceValue(raw: JsonObject, key: String, out: JsonObject) {
        if (raw.containsKey(key))
            out.put(key, coerce(raw.getValue(key)))
    }

    @Throws(QueryException::class)
    override fun coerceValue(raw: JsonArray, index: Int, out: JsonArray) {
        out.add(coerce(raw.getValue(index))!!)
    }

    @Throws(QueryException::class)
    private fun coerce(value: Any?): Any? {
        if (value != null) {
            val coerced: Any
            try {
                coerced = varValueValidator(value)
            } catch (e: Exception) {
                throw QueryException("Invalid value $value for type $gqlTypeString")
            }

            return coerced
        } else {
            return null
        }
    }

    override fun toString(sb: StringBuilder) {
        sb.append("scalar ").append(name).append("\n")
    }
}
