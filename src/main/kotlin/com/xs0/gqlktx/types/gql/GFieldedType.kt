package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.schema.intro.GqlIntroField

import java.util.ArrayList
import java.util.LinkedHashSet

abstract class GFieldedType protected constructor(
        name: String,
        val fields: Map<String, GField>, // note that fields are populated later not at construction time
        description: String?)
    : GBaseType(name, description) {

    protected var _interfaces: MutableSet<GInterfaceType> = LinkedHashSet()

    internal fun addInterface(interfaceType: GInterfaceType) {
        _interfaces.add(interfaceType)
    }

    fun getInterfaces(): Set<GInterfaceType> {
        return _interfaces
    }

    protected fun dumpFieldsToString(sb: StringBuilder) {
        for ((_, value) in fields) {
            sb.append("  ")
            value.toString(sb)
            sb.append("\n")
        }
    }

    fun getFieldsForIntrospection(includeDeprecated: Boolean): List<GqlIntroField> {
        val res = ArrayList<GqlIntroField>(fields.size)

        for ((_, value) in fields)
            if (includeDeprecated || !value.deprecated)
                res.add(GqlIntroField(value))

        return res
    }
}
