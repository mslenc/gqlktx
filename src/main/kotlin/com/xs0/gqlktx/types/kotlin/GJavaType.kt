package com.xs0.gqlktx.types.kotlin

import com.xs0.gqlktx.codegen.CodeGen
import com.xs0.gqlktx.codegen.InputParseCodeGenInfo
import com.xs0.gqlktx.codegen.OutputExportCodeGenInfo
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.schema.builder.ResolvedName
import com.xs0.gqlktx.types.gql.GType

import kotlin.reflect.KType

abstract class GJavaType<CTX: Any> {
    abstract val name: ResolvedName
    abstract val type: KType
    abstract val gqlType: GType

    var suspendingOutput: Boolean = false

    fun processSuspendingDetermination(gen: CodeGen<*, CTX>): Boolean {
        if (suspendingOutput)
            return false

        if (anythingSuspends(gen)) {
            suspendingOutput = true
            return true
        } else {
            return false
        }
    }

    abstract protected fun anythingSuspends(gen: CodeGen<*, CTX>): Boolean

    open fun isNullAllowed(): Boolean {
        return type.isMarkedNullable
    }

    abstract fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): Any

    abstract fun checkUsage(isInput: Boolean)
    abstract fun baselineType(): GJavaType<CTX>
    abstract fun inputElementType(): GJavaType<CTX>?
    abstract fun inputParseInfo(gen: CodeGen<*, CTX>): InputParseCodeGenInfo
    abstract fun outputExportInfo(gen: CodeGen<*, CTX>): OutputExportCodeGenInfo
    abstract fun hasSubSelections(): Boolean
}
