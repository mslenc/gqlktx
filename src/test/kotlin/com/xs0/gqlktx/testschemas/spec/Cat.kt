package com.xs0.gqlktx.testschemas.spec

import com.xs0.gqlktx.ann.GQLArg
import com.xs0.gqlktx.ann.GraphQLField

interface Cat : Pet, CatOrDog {
    val nickname: String

    @GraphQLField
    fun doesKnowCommand(@GQLArg(required = true) catCommand: CatCommand): Boolean

    val meowVolume: Int?
}
