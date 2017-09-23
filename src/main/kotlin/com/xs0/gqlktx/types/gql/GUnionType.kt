package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.QueryException
import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.schema.intro.GqlIntroType
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

import java.util.ArrayList
import java.util.HashSet

class GUnionType(name: String) : GBaseType(name) {
    lateinit var members: Set<GObjectType>
        private set

    override val kind: TypeKind
        get() = TypeKind.UNION

    override val validAsQueryFieldType: Boolean
        get() {
            return true
        }

    override val validAsArgumentType: Boolean
        get() {
            return false
        }

    @Throws(QueryException::class)
    override fun coerceValue(raw: JsonObject, key: String, out: JsonObject) {
        throw QueryException("Union type $name can't be used as a variable")
    }

    @Throws(QueryException::class)
    override fun coerceValue(raw: JsonArray, index: Int, out: JsonArray) {
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
