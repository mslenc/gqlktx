package com.xs0.gqlktx.types.kotlin

import com.xs0.gqlktx.FieldGetter
import com.xs0.gqlktx.codegen.*
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.schema.builder.ResolvedName
import com.xs0.gqlktx.types.gql.GObjectType
import kotlin.reflect.KType

data class GJavaObjectType<CTX: Any>(override val name: ResolvedName, override val type: KType, override val gqlType: GObjectType, val fields: Map<String, FieldGetter<CTX>>) : GJavaType<CTX>() {
    override fun checkUsage(isInput: Boolean) {
        if (isInput)
            throw IllegalStateException("$type is used as both input and output")
    }

    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): Any {
        throw UnsupportedOperationException()
    }

    override fun baselineType(): GJavaType<CTX> {
        return this
    }

    override fun inputElementType(): GJavaType<CTX>? {
        return null
    }

    override fun hasSubSelections(): Boolean {
        return true
    }

    override fun inputParseInfo(gen: CodeGen<*, CTX>): InputParseCodeGenInfo {
        throw IllegalStateException("inputParseInfo() called on an object type")
    }

    override fun outputExportInfo(gen: CodeGen<*, CTX>): OutputExportCodeGenInfo {
        val packageName = type.packageName()?.excludeSystemPackages(gen) ?: gen.statePackage

        return OutputExportCodeGenInfo(
            OutputExportKind.OBJECT,
            funName = name.codeGenFunName,
            funReturnType = "Map<String, Any?>?",
            funReturnTypeNN = "Map<String, Any?>",
            funIsSuspending = anythingSuspends(gen),
            outPackageName = packageName,
            "executeGQL" + name.codeGenFunName + "(SUBSEL, SUBOBJ, SUBPATH, state)",
            name.imports,
            setOf(packageName to "executeGQL" + name.codeGenFunName)
        )
    }

    override fun anythingSuspends(gen: CodeGen<*, CTX>): Boolean {
        return fields.values.any { it.isSuspending || gen.schema.getJavaType(it.publicType.sourceType).suspendingOutput  }
    }
}
