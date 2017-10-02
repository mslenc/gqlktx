package com.xs0.gqlktx.testschemas.spec

import com.xs0.gqlktx.ann.GqlParam
import com.xs0.gqlktx.ann.GraphQLField

interface Cat : Pet, CatOrDog {
    val nickname: String

    @GraphQLField
    fun doesKnowCommand(@GqlParam(required = true) catCommand: CatCommand): Boolean

    val meowVolume: Int?
}
