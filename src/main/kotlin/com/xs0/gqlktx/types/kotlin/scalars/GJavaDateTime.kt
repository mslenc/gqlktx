package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ScalarCoercion
import com.xs0.gqlktx.codegen.BaselineExporter
import com.xs0.gqlktx.codegen.BaselineInputParser
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.schema.builder.ResolvedName
import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import java.time.LocalDateTime
import kotlin.reflect.KType

data class GJavaDateTime<CTX: Any>(override val type: KType, override val gqlType: GType) : GJavaScalarLikeType<CTX>() {
    init { checkGqlType() }

    override val name = ResolvedName.forBaseline(gqlType.kind != TypeKind.NON_NULL, "LocalDateTime", "java.time", "DateTime")

    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): LocalDateTime {
        return BaselineInputParser.parseLocalDateTimeNotNull(value, inputVarParser.inputVariables)
    }

    override fun toJson(result: Any, coercion: ScalarCoercion): Any {
        return BaselineExporter.exportLocalDateTimeNotNull(result as LocalDateTime, coercion)
    }
}