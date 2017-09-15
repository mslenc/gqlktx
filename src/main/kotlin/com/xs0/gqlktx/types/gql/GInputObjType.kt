package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.QueryException
import com.xs0.gqlktx.schema.builder.TypeKind
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

import java.util.HashSet

class GInputObjType(name: String, fields: Map<String, GField>) : GFieldedType(name, fields) {

    override val kind: TypeKind
        get() = TypeKind.INPUT_OBJECT

    override val validAsArgumentType: Boolean
        get() {
            return true
        }

    override fun coerceValue(raw: JsonArray, index: Int, out: JsonArray) {
        val rawObj: JsonObject?
        try {
            rawObj = raw.getJsonObject(index)
        } catch (e: ClassCastException) {
            throw QueryException("Expected a JSON object value for type " + gqlTypeString)
        }

        if (rawObj == null) {
            out.addNull()
        } else {
            out.add(makeCoercedObj(rawObj))
        }
    }

    override fun coerceValue(raw: JsonObject, key: String, out: JsonObject) {
        val rawObj: JsonObject?
        try {
            rawObj = raw.getJsonObject(key)
        } catch (e: ClassCastException) {
            throw QueryException("Expected a JSON object value for type " + gqlTypeString)
        }

        if (rawObj == null) {
            out.putNull(key)
        } else {
            out.put(key, makeCoercedObj(rawObj))
        }
    }

    @Throws(QueryException::class)
    private fun makeCoercedObj(rawObj: JsonObject): JsonObject {
        if (!fields.keys.containsAll(rawObj.fieldNames())) {
            val names = HashSet(rawObj.fieldNames())
            names.removeAll(fields.keys)

            throw QueryException("Unknown field(s) in value of type $gqlTypeString: $names")
        }

        val coerced = JsonObject()
        for ((key, value) in fields) {
            value.type.coerceValue(rawObj, key, coerced)
        }
        return coerced
    }

    override fun toString(sb: StringBuilder) {
        sb.append("input ").append(name).append(" {\n")
        dumpFieldsToString(sb)
        sb.append("}\n")
    }
}
