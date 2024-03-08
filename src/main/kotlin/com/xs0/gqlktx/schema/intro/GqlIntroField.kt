package com.xs0.gqlktx.schema.intro

import com.xs0.gqlktx.GqlObject
import com.xs0.gqlktx.types.gql.GField

@GqlObject("__Field")
class GqlIntroField(private val gField: GField) {

    val name: String
        get() = gField.name

    val description: String?
        get() = gField.description

    val args: List<GqlIntroInputValue>
        get() = gField.argumentsForIntrospection

    val type: GqlIntroType
        get() = gField.type.introspector

    val isDeprecated: Boolean
        get() = gField.deprecated

    val deprecationReason: String?
        get() = gField.deprecationReason
}
