package com.xs0.gqlktx.types.kotlin

import com.xs0.gqlktx.codegen.CodeGen
import com.xs0.gqlktx.codegen.InputParseCodeGenInfo
import com.xs0.gqlktx.schema.builder.ResolvedName
import com.xs0.gqlktx.types.gql.GUnionType
import kotlin.reflect.KClass
import kotlin.reflect.KType

data class GJavaUnionType<CTX: Any>(override val name: ResolvedName, override val type: KType, override val gqlType: GUnionType, override val implementations: List<KClass<*>>) : GJavaImplementableType<CTX>() {
    override fun checkUsage(isInput: Boolean) {
        if (isInput)
            throw IllegalStateException("Union $type is used as input")
    }

    override fun inputElementType(): GJavaType<CTX>? {
        return null
    }

    override fun hasSubSelections(): Boolean {
        return true
    }

    override fun inputParseInfo(gen: CodeGen<*, CTX>): InputParseCodeGenInfo {
        throw IllegalStateException("inputParseInfo() called on a union type")
    }
}
