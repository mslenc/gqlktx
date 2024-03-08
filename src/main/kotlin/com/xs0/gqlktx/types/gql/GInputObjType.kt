package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.QueryException
import com.xs0.gqlktx.dom.*
import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.schema.intro.GqlIntroInputValue

class GInputObjType(name: String, val inputFields: Map<String, GInputField>, description: String?) : GBaseType(name, description) {

    override val kind: TypeKind
        get() = TypeKind.INPUT_OBJECT

    override fun coerceValue(raw: Value): ValueObject {
        if (raw !is ValueObject)
            throw QueryException("Expected an object value for type $gqlTypeString")

        if (!inputFields.keys.containsAll(raw.elements.keys)) {
            val unknown = raw.elements.keys - inputFields.keys
            throw QueryException("Unknown field(s) in value of type $gqlTypeString: $unknown")
        }

        val coerced = LinkedHashMap<String, ValueOrNull>()
        for ((key, field) in inputFields) {
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

    val inputFieldsForIntrospection: List<GqlIntroInputValue>
        get() {
            val res = ArrayList<GqlIntroInputValue>(inputFields.size)
            for ((_, value) in inputFields)
                res.add(GqlIntroInputValue(value))

            return res
        }

    override fun toString(sb: StringBuilder) {
        sb.append("input ").append(name).append(" {\n")
        for ((_, value) in inputFields) {
            sb.append("  ")
            value.toString(sb)
            sb.append("\n")
        }
        sb.append("}\n")
    }
}
