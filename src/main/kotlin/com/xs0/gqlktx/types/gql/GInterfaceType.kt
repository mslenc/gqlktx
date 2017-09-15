package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.QueryException
import com.xs0.gqlktx.schema.builder.TypeKind
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

class GInterfaceType(name: String, fields: Map<String, GField>) : GFieldedType(name, fields) {
    lateinit var implementations: Set<GObjectType>
        private set

    fun setImpls(implementations: Set<GObjectType>) {
        this.implementations = implementations

        for (objectType in implementations) {
            objectType.addInterface(this)
        }
    }

    override val kind: TypeKind
        get() = TypeKind.INTERFACE

    override val validAsArgumentType: Boolean
        get() {
            return false
        }

    override fun coerceValue(raw: JsonObject, key: String, out: JsonObject) {
        throw QueryException("Interface type $name can't be used as a variable")
    }

    override fun coerceValue(raw: JsonArray, index: Int, out: JsonArray) {
        throw QueryException("Interface type $name can't be used as a variable")
    }

    override fun toString(sb: StringBuilder) {
        sb.append("interface ").append(name).append(" {\n")
        dumpFieldsToString(sb)
        sb.append("}\n")
    }
}
