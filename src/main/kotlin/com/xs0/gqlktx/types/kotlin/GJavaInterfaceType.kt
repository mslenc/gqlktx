package com.xs0.gqlktx.types.kotlin

import com.xs0.gqlktx.codegen.*
import com.xs0.gqlktx.schema.builder.ResolvedName
import com.xs0.gqlktx.schema.builder.nonNullType
import com.xs0.gqlktx.types.gql.GInterfaceType
import kotlin.reflect.KClass
import kotlin.reflect.KType

data class GJavaInterfaceType<CTX: Any>(override val name: ResolvedName, override val type: KType, override val gqlType: GInterfaceType, override val implementations: List<KClass<*>>) : GJavaImplementableType<CTX>() {
    override fun checkUsage(isInput: Boolean) {
        if (isInput)
            throw IllegalStateException("Interface $type is used as input")
    }

    override fun inputElementType(): GJavaType<CTX>? {
        return null
    }

    override fun hasSubSelections(): Boolean {
        return true
    }

    override fun inputParseInfo(gen: CodeGen<*, CTX>): InputParseCodeGenInfo {
        throw IllegalStateException("inputParseInfo() called on an interface type")
    }
}
