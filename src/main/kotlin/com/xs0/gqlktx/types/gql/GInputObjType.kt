package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.QueryException
import com.xs0.gqlktx.schema.builder.TypeKind
import io.vertx.core.json.JsonObject

class GInputObjType(name: String, fields: Map<String, GField>) : GFieldedType(name, fields) {

    override val kind: TypeKind
        get() = TypeKind.INPUT_OBJECT

    override val validAsArgumentType: Boolean
        get() {
            return true
        }

    override fun coerceValue(raw: Any): JsonObject {
        if (raw !is JsonObject)
            throw QueryException("Expected a JSON object value for type " + gqlTypeString)

        if (!fields.keys.containsAll(raw.fieldNames())) {
            val unknown = raw.fieldNames() - fields.keys
            throw QueryException("Unknown field(s) in value of type $gqlTypeString: $unknown")
        }

        val coerced = JsonObject()
        for ((key, field) in fields) {
            if (key !in raw.fieldNames())
                continue

            val rawValue: Any? = raw.getValue(key)
            if (rawValue == null) {
                if (field.type.kind == TypeKind.NON_NULL)
                    throw QueryException("Null value not allowed for field $key")

                coerced.putNull(key)
            } else {
                coerced.put(key, field.type.coerceValue(rawValue))
            }
        }

        return coerced
    }

    override fun toString(sb: StringBuilder) {
        sb.append("input ").append(name).append(" {\n")
        dumpFieldsToString(sb)
        sb.append("}\n")
    }
}
