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
import com.xs0.gqlktx.utils.NodeId
import kotlin.reflect.KType

data class GJavaNodeId<CTX: Any>(override val type: KType, override val gqlType: GType) : GJavaScalarLikeType<CTX>() {
    init {
        checkGqlType()

        if (type.classifier != NodeId::class)
            throw IllegalArgumentException("Not a NodeId type ${type.classifier}")
    }

    override val name = ResolvedName.forBaseline(gqlType.kind != TypeKind.NON_NULL, "NodeId", "com.xs0.gqlktx.utils", "ID")

    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): NodeId {
        return BaselineInputParser.parseNodeIdNotNull(value, inputVarParser.inputVariables)
    }

    override fun toJson(result: Any, coercion: ScalarCoercion): Any {
        return BaselineExporter.exportNodeIdNotNull(result as NodeId, coercion)
    }
}
