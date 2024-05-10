package com.xs0.gqlktx.testschemas.refl

import com.xs0.gqlktx.GqlField
import com.xs0.gqlktx.GqlIgnore
import com.xs0.gqlktx.GqlObject

class ReflTestSchema {
    fun getQuery() = ReflQueryRoot()
}

class ReflQueryRoot {
    fun getTest(): ReflTest1 = ReflTest1(123, 234)
}


@GqlObject
data class ReflTest1(
    @GqlIgnore
    val entityId: Long,

    @get:GqlIgnore
    val entityId2: Long,
) {

    @GqlIgnore
    val entityId3: Long = 0L

    @get:GqlIgnore
    val entityId4: Long = 0L

    @GqlField
    fun getEntity(): String {
        return "abc$entityId"
    }
}