package com.xs0.gqlktx.schema.intro

import com.xs0.gqlktx.ann.GraphQLObject
import com.xs0.gqlktx.types.gql.GField

@GraphQLObject("__Field")
class GqlIntroField(private val gField: GField) {

    val name: String
        get() = gField.name

    val description: String?
        get() = gField.description

    val args: List<GqlIntroInputValue>
        get() = gField.argumentsForIntrospection

    val type: GqlIntroType
        get() = gField.type.introspector()

    // TODO
    val isDeprecated: Boolean
        get() = false

    // TODO
    val deprecationReason: String?
        get() = null
}
