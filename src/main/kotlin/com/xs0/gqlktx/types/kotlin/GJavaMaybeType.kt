package com.xs0.gqlktx.types.kotlin

import com.xs0.gqlktx.codegen.CodeGen
import com.xs0.gqlktx.codegen.InputParseCodeGenInfo
import com.xs0.gqlktx.codegen.InputParseKind
import com.xs0.gqlktx.codegen.OutputExportCodeGenInfo
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.schema.builder.ResolvedName
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.utils.Maybe
import kotlin.reflect.KType

data class GJavaMaybeType<CTX: Any>(override val type: KType, val innerType: GJavaType<CTX>, override val gqlType: GType) : GJavaType<CTX>() {
    override val name = ResolvedName(
        innerType.name.gqlName,
        innerType.name.imports + setOf("com.xs0.gqlktx.utils" to "Maybe"),
        codeGenFunName = "MaybeOf${ innerType.name.codeGenFunName }",
        codeGenTypeNN = "Maybe<${ innerType.name.codeGenType }>",
        isNullableType = true,
    )

    override fun isNullAllowed(): Boolean {
        return true
    }

    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): Any {
        return Maybe(innerType.getFromJson(value, inputVarParser))
    }

    override fun baselineType(): GJavaType<CTX> {
        return innerType.baselineType()
    }

    override fun inputElementType(): GJavaType<CTX> {
        return innerType
    }

    override fun checkUsage(isInput: Boolean) {
        if (!isInput)
            throw IllegalStateException("Maybe is only supported for input (type $type)")

        innerType.checkUsage(isInput)
    }

    override fun hasSubSelections(): Boolean {
        return innerType.hasSubSelections()
    }

    override fun inputParseInfo(gen: CodeGen<*, CTX>): InputParseCodeGenInfo {
        val base = innerType.inputParseInfo(gen)
        return InputParseCodeGenInfo(
            kind = InputParseKind.MAYBE,
            funName = "MaybeOf" + base.funName,
            funReturnType = name.codeGenType,
            funCreateType = name.codeGenTypeNN,
            outPackageName = base.outPackageName,
            exprTemplate = "parseMaybeOf" + base.funName + "(VALUE, variables)",
            importsForGen = base.importsForUse + setOf("com.xs0.gqlktx.utils" to "Maybe"),
            importsForUse = setOf(base.outPackageName to "parseMaybeOf" + base.funName),
        )
    }

    override fun outputExportInfo(gen: CodeGen<*, CTX>): OutputExportCodeGenInfo {
        throw IllegalStateException("outputExportInfo called on Maybe")
    }

    override fun anythingSuspends(gen: CodeGen<*, CTX>): Boolean {
        return innerType.suspendingOutput
    }
}
