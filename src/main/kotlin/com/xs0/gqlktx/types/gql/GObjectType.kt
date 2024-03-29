package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.QueryException
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.schema.intro.GqlIntroType

import java.util.*

class GObjectType(name: String, fields: Map<String, GField>, description: String?) : GFieldedType(name, fields, description) {

    override val kind: TypeKind
        get() = TypeKind.OBJECT

    override fun coerceValue(raw: Value): Value {
        throw QueryException("Object type $name can't be used as a variable")
    }

    override fun toString(sb: StringBuilder) {
        sb.append("type ").append(name)

        var first = true
        for (i in getInterfaces()) {
            sb.append(if (first) " implements " else ", ")
            first = false
            sb.append(i.name)
        }

        sb.append(" {\n")
        dumpFieldsToString(sb)
        sb.append("}\n")
    }

    val interfacesForIntrospection: List<GqlIntroType> by lazy {
        val res = ArrayList<GqlIntroType>(getInterfaces().size)
        for (inter in getInterfaces())
            res.add(inter.introspector)
        res
    }
}
