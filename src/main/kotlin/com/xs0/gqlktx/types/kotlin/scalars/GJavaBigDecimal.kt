package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ScalarCoercion
import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.codegen.BaselineExporter
import com.xs0.gqlktx.codegen.BaselineInputParser
import com.xs0.gqlktx.codegen.CodeGen
import com.xs0.gqlktx.codegen.OutputExportCodeGenInfo
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.dom.ValueNumber
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.schema.builder.ResolvedName
import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import java.math.BigDecimal
import kotlin.reflect.KType

data class GJavaBigDecimal<CTX: Any>(override val type: KType, override val gqlType: GType) : GJavaScalarLikeType<CTX>() {
    init { checkGqlType() }

    override val name = ResolvedName.forBaseline(gqlType.kind != TypeKind.NON_NULL, "BigDecimal", "java.math", "Float")

    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): BigDecimal {
        return BaselineInputParser.parseBigDecimalNotNull(value, inputVarParser.inputVariables)
    }

    override fun toJson(result: Any, coercion: ScalarCoercion): Any {
        return BaselineExporter.exportBigDecimalNotNull(result as BigDecimal, coercion)
    }

    override fun outputExportInfo(gen: CodeGen<*, CTX>): OutputExportCodeGenInfo {
        return BaselineExporter.codeGenInfo(name, gen, "Number")
    }
}