package com.xs0.gqlktx.schema.intro

import com.xs0.gqlktx.GqlObject

@GqlObject("__Directive")
class GqlIntroDirective(
    val name: String,
    val description: String,
    val locations: List<GqlIntroDirectiveLocation>,
    val args: List<GqlIntroInputValue>
)
