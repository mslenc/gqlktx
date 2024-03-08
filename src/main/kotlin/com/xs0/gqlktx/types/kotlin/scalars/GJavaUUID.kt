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
import kotlin.reflect.KType

import java.util.UUID

data class GJavaUUID<CTX: Any>(override val type: KType, override val gqlType: GType) : GJavaScalarLikeType<CTX>() {
    init { checkGqlType() }

    override val name = ResolvedName.forBaseline(gqlType.kind != TypeKind.NON_NULL, "UUID", "java.util", "ID")

    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): UUID {
        return BaselineInputParser.parseUUIDNotNull(value, inputVarParser.inputVariables)
    }

    override fun toJson(result: Any, coercion: ScalarCoercion): Any {
        return BaselineExporter.exportUUIDNotNull(result as UUID, coercion)
    }
}
