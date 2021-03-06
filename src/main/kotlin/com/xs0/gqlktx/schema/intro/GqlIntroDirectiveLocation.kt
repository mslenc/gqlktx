package com.xs0.gqlktx.schema.intro

import com.xs0.gqlktx.GqlEnum

@GqlEnum("__DirectiveLocation")
enum class GqlIntroDirectiveLocation {
    QUERY,
    MUTATION,
    SUBSCRIPTION,
    FIELD,
    FRAGMENT_DEFINITION,
    FRAGMENT_SPREAD,
    INLINE_FRAGMENT
}
