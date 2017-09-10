package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ValidationException
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

    override fun getFromJson(value: Any, inputVarParser: InputVarParser<CTX>): NodeId {
        val string: String
        if (value is CharSequence) {
            string = value.toString()
        } else {
            throw ValidationException("Expected a string, but got something else")
        }

        try {
            return NodeId.fromPublicID(string)
        } catch (e: IllegalArgumentException) {
            throw ValidationException(e.message)
        }
    }

    override fun toJson(result: Any): Any {
        return (result as NodeId).toPublicId()
    }
}
