package com.xs0.gqlktx.schema.intro

import com.xs0.gqlktx.GqlObject
import com.xs0.gqlktx.schema.Schema
import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.types.gql.GBaseType

import java.util.ArrayList
import java.util.Arrays

import com.xs0.gqlktx.schema.intro.GqlIntroDirectiveLocation.FIELD
import com.xs0.gqlktx.schema.intro.GqlIntroDirectiveLocation.FRAGMENT_SPREAD
import com.xs0.gqlktx.schema.intro.GqlIntroDirectiveLocation.INLINE_FRAGMENT
import com.xs0.gqlktx.types.kotlin.GJavaNotNullType
import java.util.Collections.singletonList

@GqlObject("__Schema")
class GqlIntroSchema(private val schema: Schema<*, *>) {
    val directives: List<GqlIntroDirective> = buildDirectives()

    private fun buildDirectives(): List<GqlIntroDirective> {
        val res = ArrayList<GqlIntroDirective>()

        val boolNotNull = schema.getGQLBaseType("Boolean").notNull().introspector

        res.add(
                GqlIntroDirective(
                        "if",
                        "Directs the executor to include this field or fragment only when the `if` argument is true.",
                        listOf(FIELD, FRAGMENT_SPREAD, INLINE_FRAGMENT),
                        listOf(GqlIntroInputValue("if", "Included when true.", boolNotNull, null))
                )
        )

        res.add(
                GqlIntroDirective(
                        "skip",
                        "Directs the executor to skip this field or fragment when the `if` argument is true.",
                        listOf(FIELD, FRAGMENT_SPREAD, INLINE_FRAGMENT),
                        listOf(GqlIntroInputValue("if", "Skipped when true.", boolNotNull, null))
                )
        )

        return res
    }

    fun getTypes(kinds: Array<TypeKind>?): List<GqlIntroType> {
        val res = ArrayList<GqlIntroType>()
        val filter: (TypeKind)->Boolean
        if (kinds != null && kinds.isNotEmpty()) {
            filter = kinds.toSet()::contains
        } else {
            filter = { _ -> true }
        }

        for (type in schema.allBaseTypes)
            if (filter(type.kind))
                res.add(type.introspector)

        return res
    }

    val queryType: GqlIntroType
        get() {
            var type = schema.getJavaType(schema.queryRoot.type)
            if (type is GJavaNotNullType)
                type = type.innerType

            return type.gqlType.introspector
        }

    val mutationType: GqlIntroType?
        get() {
            if (schema.mutationRoot == null)
                return null

            var type = schema.getJavaType(schema.mutationRoot.type)
            if (type is GJavaNotNullType)
                type = type.innerType

            return type.gqlType.introspector
        }

    // TODO
    val subscriptionType: GqlIntroType?
        get() = null

    fun self(): GqlIntroSchema {
        return this // used for simple implementation
    }

    fun type(name: String): GqlIntroType {
        return schema.getGQLBaseType(name).introspector
    }
}
