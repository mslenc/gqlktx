package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.QueryException
import com.xs0.gqlktx.dom.*
import com.xs0.gqlktx.schema.builder.TypeKind

class GInputObjType(name: String, fields: Map<String, GField>) : GFieldedType(name, fields) {

    override val kind: TypeKind
        get() = TypeKind.INPUT_OBJECT

    override val validAsArgumentType: Boolean
        get() {
            return true
        }

    override fun coerceValue(raw: Value): ValueObject {
        if (raw !is ValueObject)
            throw QueryException("Expected an object value for type $gqlTypeString")

        if (!fields.keys.containsAll(raw.elements.keys)) {
            val unknown = raw.elements.keys - fields.keys
            throw QueryException("Unknown field(s) in value of type $gqlTypeString: $unknown")
        }

        val coerced = LinkedHashMap<String, ValueOrNull>()
        for ((key, field) in fields) {
            if (key !in raw.elements.keys)
                continue

            when (val rawValue: ValueOrVar = raw.elements.getValue(key)) {
                is ValueNull -> {
                    if (field.type.kind == TypeKind.NON_NULL)
                        throw QueryException("Null value not allowed for field $key")

                    coerced[key] = rawValue
                }

                is Variable -> {
                    throw IllegalStateException("Variable?!?") // this should never happen
                }

                is Value -> {
                    coerced[key] = field.type.coerceValue(rawValue)
                }
            }
        }

        return ValueObject(coerced)
    }

    override fun toString(sb: StringBuilder) {
        sb.append("input ").append(name).append(" {\n")
        dumpFieldsToString(sb)
        sb.append("}\n")
    }
}
