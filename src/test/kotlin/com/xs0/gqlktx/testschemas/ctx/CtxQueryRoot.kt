package com.xs0.gqlktx.testschemas.ctx

import com.xs0.gqlktx.GqlField
import com.xs0.gqlktx.GqlIgnore
import com.xs0.gqlktx.GqlObject

class CtxTestContext {
    fun process(a: String): String {
        return "ctx($a)"
    }
}

class CtxTestSchema {
    fun getQuery() = CtxQueryRoot()
}

class CtxQueryRoot {
    context(ctx: CtxTestContext)
    fun getTest(): CtxTest1 = CtxTest1(ctx.process("root"))
}

@GqlObject
data class CtxTest1(
    @GqlField val name: String
) {
    context(ctx: CtxTestContext)
    fun getBibi(): String {
        return ctx.process("bibi")
    }

    fun getBobo(): String {
        return "bobo"
    }
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