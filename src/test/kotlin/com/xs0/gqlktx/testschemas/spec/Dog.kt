package com.xs0.gqlktx.testschemas.spec

import com.xs0.gqlktx.GqlParam
import com.xs0.gqlktx.GqlField

interface Dog : Pet, DogOrHuman {
    val nickname: String

    val barkVolume: Int?

    @GqlField
    fun doesKnowCommand(@GqlParam(required = true) dogCommand: DogCommand): Boolean

    @GqlField("isHousetrained")
    fun isHousetrained(atOtherHomes: Boolean?): Boolean

    val owner: Human
}
