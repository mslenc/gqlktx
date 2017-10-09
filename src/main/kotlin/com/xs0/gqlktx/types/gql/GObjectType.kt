package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.QueryException
import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.schema.intro.GqlIntroType
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

import java.util.*

class GObjectType(name: String, fields: Map<String, GField>) : GFieldedType(name, fields) {
    private var interfaces: MutableSet<GInterfaceType> = LinkedHashSet()

    override val kind: TypeKind
        get() = TypeKind.OBJECT

    override val validAsArgumentType: Boolean
        get() {
            return false
        }

    override fun coerceValue(raw: Any): Any {
        throw QueryException("Object type $name can't be used as a variable")
    }

    override fun toString(sb: StringBuilder) {
        sb.append("type ").append(name)

        var first = true
        for (i in interfaces) {
            sb.append(if (first) " implements " else ", ")
            first = false
            sb.append(i.name)
        }

        sb.append(" {\n")
        dumpFieldsToString(sb)
        sb.append("}\n")
    }

    internal fun addInterface(interfaceType: GInterfaceType) {
        interfaces.add(interfaceType)
    }

    val interfacesForIntrospection: List<GqlIntroType> by lazy {
        val res = ArrayList<GqlIntroType>(interfaces.size)
        for (inter in interfaces)
            res.add(inter.introspector)
        res
    }
}
