package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ScalarCoercion
import com.xs0.gqlktx.codegen.BaselineExporter
import com.xs0.gqlktx.codegen.BaselineInputParser
import com.xs0.gqlktx.codegen.CodeGen
import com.xs0.gqlktx.codegen.OutputExportCodeGenInfo
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.schema.builder.ResolvedName
import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import kotlin.reflect.KType

data class GJavaBoolean<CTX: Any>(override val type: KType, override val gqlType: GType) : GJavaScalarLikeType<CTX>() {
    init {
        checkGqlType()

        if (type.classifier != Boolean::class)
            throw IllegalArgumentException("Not a boolean type: $type")
    }

    override val name = ResolvedName.forBaseline(gqlType.kind != TypeKind.NON_NULL, "Boolean")

    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): Boolean {
        return BaselineInputParser.parseBooleanNotNull(value, inputVarParser.inputVariables)
    }

    override fun toJson(result: Any, coercion: ScalarCoercion): Any {
        return BaselineExporter.exportBooleanNotNull(result as Boolean, coercion)
    }

    override fun outputExportInfo(gen: CodeGen<*, CTX>): OutputExportCodeGenInfo {
        return BaselineExporter.codeGenInfo(name, gen, "Boolean")
    }
}
