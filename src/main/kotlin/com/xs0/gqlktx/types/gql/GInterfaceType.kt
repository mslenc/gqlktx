package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.QueryException
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.schema.intro.GqlIntroType
import java.util.ArrayList
import java.util.LinkedHashSet

class GInterfaceType(name: String, fields: Map<String, GField>, description: String?) : GFieldedType(name, fields, description) {
    lateinit var implementations: Set<GFieldedType>
        private set

    fun setImpls(implementations: Set<GFieldedType>) {
        this.implementations = implementations

        for (objectType in implementations) {
            objectType.addInterface(this)
        }
    }

    val implsForIntrospection: List<GqlIntroType> by lazy {
        val res = ArrayList<GqlIntroType>(implementations.size)
        for (posib in implementations)
            res.add(posib.introspector)
        res
    }

    override val kind: TypeKind
        get() = TypeKind.INTERFACE

    override fun coerceValue(raw: Value): Value {
        throw QueryException("Interface type $name can't be used as a variable")
    }

    override fun toString(sb: StringBuilder) {
        sb.append("interface ").append(name).append(" {\n")
        dumpFieldsToString(sb)
        sb.append("}\n")
    }
}
