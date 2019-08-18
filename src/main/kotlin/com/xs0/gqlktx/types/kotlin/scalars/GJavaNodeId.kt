package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ScalarCoercion
import com.xs0.gqlktx.ScalarUtils
import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import com.xs0.gqlktx.utils.NodeId
import kotlin.reflect.KType

class GJavaNodeId<CTX>(type: KType, gqlType: GType) : GJavaScalarLikeType<CTX>(type, gqlType) {
    init {
        if (type.classifier != NodeId::class)
            throw IllegalArgumentException("Not a NodeId type ${type.classifier}")
    }

    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): NodeId {
        val string = ScalarUtils.validateString(value)

        try {
            return NodeId.fromPublicID(string)
        } catch (e: IllegalArgumentException) {
            throw ValidationException(e)
        }
    }

    override fun toJson(result: Any, coercion: ScalarCoercion): Any {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET ->
                (result as NodeId).toPublicId()

            ScalarCoercion.NONE ->
                result
        }
    }
}
