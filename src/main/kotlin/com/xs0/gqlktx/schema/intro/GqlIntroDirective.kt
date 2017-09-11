package com.xs0.gqlktx.schema.intro

import com.xs0.gqlktx.ann.GraphQLObject

@GraphQLObject("__Directive")
class GqlIntroDirective(
    val name: String,
    val description: String,
    val locations: List<GqlIntroDirectiveLocation>,
    val args: List<GqlIntroInputValue>
)
