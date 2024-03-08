package com.xs0.gqlktx.types.kotlin

import com.xs0.gqlktx.codegen.*
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.schema.builder.ResolvedName
import com.xs0.gqlktx.types.gql.GNotNullType
import kotlin.reflect.KType

data class GJavaNotNullType<CTX : Any>(override val name: ResolvedName, override val type: KType, val innerType: GJavaType<CTX>, override val gqlType: GNotNullType) : GJavaType<CTX>() {
    override fun isNullAllowed(): Boolean {
        return false
    }

    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): Any {
        return innerType.getFromJson(value, inputVarParser)
    }

    override fun baselineType(): GJavaType<CTX> {
        return innerType.baselineType()
    }

    override fun inputElementType(): GJavaType<CTX> {
        return innerType
    }

    override fun checkUsage(isInput: Boolean) {
        innerType.checkUsage(isInput)
    }

    override fun hasSubSelections(): Boolean {
        return innerType.hasSubSelections()
    }

    override fun inputParseInfo(gen: CodeGen<*, CTX>): InputParseCodeGenInfo {
        val sub = innerType.inputParseInfo(gen)

        if (sub.kind == InputParseKind.BASELINE) {
            val name = ResolvedName(
                innerType.name.gqlName,
                innerType.name.imports,
                codeGenFunName = innerType.name.codeGenFunName,
                codeGenTypeNN = innerType.name.codeGenTypeNN,
                isNullableType = false,
            )

            return BaselineInputParser.codeGenInfo(name, gen)
        } else {
            return InputParseCodeGenInfo(
                InputParseKind.NOT_NULL,
                funName = "NotNullOf" + sub.funName,
                funReturnType = sub.funCreateType,
                funCreateType = sub.funCreateType,
                outPackageName = sub.outPackageName,
                exprTemplate = "parseNotNullOf" + sub.funName + "(VALUE, variables)",
                importsForGen = sub.importsForUse,
                importsForUse = setOf(sub.outPackageName to ("parseNotNullOf" + sub.funName)),
                nullable = false
            )
        }
    }

    override fun outputExportInfo(gen: CodeGen<*, CTX>): OutputExportCodeGenInfo {
        val sub = innerType.outputExportInfo(gen)

        if (sub.kind == OutputExportKind.BASELINE) {
            val name = ResolvedName(
                innerType.name.gqlName,
                innerType.name.imports,
                codeGenFunName = innerType.name.codeGenFunName,
                codeGenTypeNN = innerType.name.codeGenTypeNN,
                isNullableType = false,
            )

            return BaselineExporter.codeGenInfo(name, gen)
        } else {
            return sub.buildWrapper(
                OutputExportKind.NOT_NULL,
                "NotNullOf",
                innerType.hasSubSelections(),
                sub.funReturnTypeNN,
                sub.funReturnTypeNN
            )
        }
    }

    override fun anythingSuspends(gen: CodeGen<*, CTX>): Boolean {
        return innerType.suspendingOutput
    }
}
