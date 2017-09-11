package com.xs0.gqlktx.schema.intro

import com.xs0.gqlktx.ann.GraphQLObject

@GraphQLObject("__EnumValue")
class GqlIntroEnumValue(val name: String) {
    // TODO
    val description: String?
        get() = null

    // TODO
    val isDeprecated: Boolean
        get() = false

    // TODO
    val deprecationReason: String?
        get() = null
}
