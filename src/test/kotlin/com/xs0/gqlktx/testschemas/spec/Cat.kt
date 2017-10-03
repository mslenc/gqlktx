package com.xs0.gqlktx.testschemas.spec

import com.xs0.gqlktx.GqlParam
import com.xs0.gqlktx.GqlField

interface Cat : Pet, CatOrDog {
    val nickname: String

    @GqlField
    fun doesKnowCommand(@GqlParam(required = true) catCommand: CatCommand): Boolean

    val meowVolume: Int?
}
