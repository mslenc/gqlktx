package com.xs0.gqlktx.types.kotlin.scalars

import com.fasterxml.jackson.databind.node.ObjectNode
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

data class GJavaObjectNode<CTX: Any>(override val type: KType, override val gqlType: GType) : GJavaScalarLikeType<CTX>() {
    init {
        checkGqlType()

        if (type.classifier != ObjectNode::class)
            throw IllegalArgumentException("Expected ByteArray type")
    }

    override val name = ResolvedName.forBaseline(gqlType.kind != TypeKind.NON_NULL, "ObjectNode", null, "JsonObject")

    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): ObjectNode {
        return BaselineInputParser.parseObjectNodeNotNull(value, inputVarParser.inputVariables)
    }

    override fun toJson(result: Any, coercion: ScalarCoercion): Any {
        return BaselineExporter.exportObjectNodeNotNull(result as ObjectNode, coercion)
    }
}
