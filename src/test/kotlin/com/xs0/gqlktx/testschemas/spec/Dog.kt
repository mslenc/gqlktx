package com.xs0.gqlktx.testschemas.spec

import com.xs0.gqlktx.ann.GqlParam
import com.xs0.gqlktx.ann.GraphQLField

interface Dog : Pet, DogOrHuman {
    val nickname: String

    val barkVolume: Int?

    @GraphQLField
    fun doesKnowCommand(@GqlParam(required = true) dogCommand: DogCommand): Boolean

    @GraphQLField("isHousetrained")
    fun isHousetrained(atOtherHomes: Boolean?): Boolean

    val owner: Human
}
