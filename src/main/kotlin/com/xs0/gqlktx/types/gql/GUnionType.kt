package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.QueryException
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.schema.intro.GqlIntroType

import java.util.ArrayList
import java.util.HashSet

class GUnionType(name: String, description: String?) : GBaseType(name, description) {
    lateinit var members: Set<GObjectType>
        private set

    override val kind: TypeKind
        get() = TypeKind.UNION

    override fun coerceValue(raw: Value): Value {
        throw QueryException("Union type $name can't be used as a variable")
    }

    override fun toString(sb: StringBuilder) {
        sb.append("union ").append(name)

        var first = true
        for (member in members) {
            sb.append(if (first) " = " else " | ")
            first = false
            sb.append(member.name)
        }
        sb.append("\n")
    }

    val membersForIntrospection: List<GqlIntroType> by lazy {
        val res = ArrayList<GqlIntroType>(members.size)
        for (posib in members)
            res.add(posib.introspector)
        res
    }

    fun setMembers(gqlImpls: HashSet<GObjectType>) {
        this.members = gqlImpls
    }
}
