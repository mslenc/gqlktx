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

data class GJavaShort<CTX: Any>(override val type: KType, override val gqlType: GType) : GJavaScalarLikeType<CTX>() {
    init {
        checkGqlType()

        if (type.classifier != Short::class)
            throw IllegalArgumentException("Not a short class ${type.classifier}")
    }

    override val name = ResolvedName.forBaseline(gqlType.kind != TypeKind.NON_NULL, "Short", null, "Int")

    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): Short {
        return BaselineInputParser.parseShortNotNull(value, inputVarParser.inputVariables)
    }

    override fun toJson(result: Any, coercion: ScalarCoercion): Any {
        return BaselineExporter.exportShortNotNull(result as Short, coercion)
    }

    override fun outputExportInfo(gen: CodeGen<*, CTX>): OutputExportCodeGenInfo {
        return BaselineExporter.codeGenInfo(name, gen, "Number")
    }
}
