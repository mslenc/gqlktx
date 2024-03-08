package com.xs0.gqlktx.types.kotlin

import com.xs0.gqlktx.codegen.*
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.schema.builder.nonNullType
import com.xs0.gqlktx.schema.builder.nullableType
import com.xs0.gqlktx.types.gql.GBaseType
import kotlin.reflect.KClass
import kotlin.reflect.KType

abstract class GJavaImplementableType<CTX: Any> protected constructor() : GJavaType<CTX>() {
    abstract val implementations: List<KClass<*>>

    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): Any {
        throw UnsupportedOperationException()
    }

    override fun baselineType(): GJavaType<CTX> {
        return this
    }

    override fun outputExportInfo(gen: CodeGen<*, CTX>): OutputExportCodeGenInfo {
        val packageName = type.packageName()?.excludeSystemPackages(gen) ?: gen.statePackage

        return OutputExportCodeGenInfo(
            OutputExportKind.INTERFACE,
            funName = name.codeGenFunName,
            funReturnType = "Map<String, Any?>?",
            funReturnTypeNN = "Map<String, Any?>",
            anythingSuspends(gen),
            outPackageName = packageName,
            "executeGQL" + name.codeGenFunName + "(SUBSEL, SUBOBJ, SUBPATH, state)",
            name.imports,
            setOf(packageName to "executeGQL" + name.codeGenFunName)
        )
    }

    override fun anythingSuspends(gen: CodeGen<*, CTX>): Boolean {
        return implementations.any {
            gen.schema.getJavaType(it.nullableType()).suspendingOutput
        }
    }
}
