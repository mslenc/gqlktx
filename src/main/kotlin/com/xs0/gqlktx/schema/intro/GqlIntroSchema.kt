package com.xs0.gqlktx.schema.intro

import com.xs0.gqlktx.ann.GraphQLObject
import com.xs0.gqlktx.schema.Schema
import com.xs0.gqlktx.types.gql.GBaseType

import java.util.ArrayList
import java.util.Arrays

import com.xs0.gqlktx.schema.intro.GqlIntroDirectiveLocation.FIELD
import com.xs0.gqlktx.schema.intro.GqlIntroDirectiveLocation.FRAGMENT_SPREAD
import com.xs0.gqlktx.schema.intro.GqlIntroDirectiveLocation.INLINE_FRAGMENT
import java.util.Collections.singletonList

@GraphQLObject("__Schema")
class GqlIntroSchema(private val schema: Schema<*, *>) {
    val directives: List<GqlIntroDirective> = buildDirectives()

    private fun buildDirectives(): List<GqlIntroDirective> {
        val res = ArrayList<GqlIntroDirective>()

        val boolNotNull = schema.getGQLBaseType("Boolean").notNull().introspector

        res.add(
                GqlIntroDirective(
                        "if",
                        "Directs the executor to include this field or fragment only when the `if` argument is true.",
                        Arrays.asList(FIELD, FRAGMENT_SPREAD, INLINE_FRAGMENT),
                        listOf(GqlIntroInputValue("if", "Included when true.", boolNotNull, null))
                )
        )

        res.add(
                GqlIntroDirective(
                        "skip",
                        "Directs the executor to skip this field or fragment when the `if` argument is true.",
                        Arrays.asList(FIELD, FRAGMENT_SPREAD, INLINE_FRAGMENT),
                        listOf(GqlIntroInputValue("if", "Skipped when true.", boolNotNull, null))
                )
        )

        return res
    }

    val types: List<GqlIntroType>
        get() {
            val res = ArrayList<GqlIntroType>()

            for (type in schema.allBaseTypes)
                res.add(type.introspector)

            return res
        }

    val queryType: GqlIntroType
        get() = schema.getJavaType(schema.queryRoot.type).gqlType.introspector

    val mutationType: GqlIntroType?
        get() = if (schema.mutationRoot == null) null else schema.getJavaType(schema.mutationRoot.type).gqlType.introspector

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
