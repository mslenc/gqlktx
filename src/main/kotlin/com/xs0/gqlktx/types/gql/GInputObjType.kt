package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.QueryException
import com.xs0.gqlktx.schema.builder.TypeKind

class GInputObjType(name: String, fields: Map<String, GField>) : GFieldedType(name, fields) {

    override val kind: TypeKind
        get() = TypeKind.INPUT_OBJECT

    override val validAsArgumentType: Boolean
        get() {
            return true
        }

    override fun coerceValue(raw: Any): Map<String, Any?> {
        if (raw !is Map<*, *>)
            throw QueryException("Expected a JSON object value for type $gqlTypeString")

        raw as Map<String, Any?>

        if (!fields.keys.containsAll(raw.keys)) {
            val unknown = raw.keys - fields.keys
            throw QueryException("Unknown field(s) in value of type $gqlTypeString: $unknown")
        }

        val coerced = LinkedHashMap<String, Any?>()
        for ((key, field) in fields) {
            if (key !in raw.keys)
                continue

            val rawValue: Any? = raw[key]
            if (rawValue == null) {
                if (field.type.kind == TypeKind.NON_NULL)
                    throw QueryException("Null value not allowed for field $key")

                coerced[key] = null
            } else {
                coerced[key] = field.type.coerceValue(rawValue)
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
