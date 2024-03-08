package com.xs0.gqlktx.types.kotlin

import com.xs0.gqlktx.ScalarCoercion
import com.xs0.gqlktx.codegen.*
import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.types.gql.GNotNullType

abstract class GJavaScalarLikeType<CTX: Any> : GJavaType<CTX>() {
    protected fun checkGqlType() {
        when (gqlType.kind) {
            TypeKind.SCALAR, TypeKind.ENUM -> {
                // ok
            }

            TypeKind.NON_NULL -> {
                when ((gqlType as GNotNullType).wrappedType.kind) {
                    TypeKind.SCALAR, TypeKind.ENUM -> {
                        // ok
                    }

                    else ->
                        throw IllegalStateException("Expected a value type instead of " + gqlType.gqlTypeString)
                }

            }

            else ->
                throw IllegalStateException("Expected a value type instead of " + gqlType.gqlTypeString)
        }
    }

    override fun baselineType(): GJavaType<CTX> {
        return this
    }

    override fun inputElementType(): GJavaType<CTX>? {
        return null
    }

    override fun checkUsage(isInput: Boolean) {
        // ok - scalars can be both inputs and outputs
    }

    abstract fun toJson(result: Any, coercion: ScalarCoercion): Any

    override fun inputParseInfo(gen: CodeGen<*, CTX>): InputParseCodeGenInfo {
        return BaselineInputParser.codeGenInfo(name, gen)
    }

    override fun outputExportInfo(gen: CodeGen<*, CTX>): OutputExportCodeGenInfo {
        return BaselineExporter.codeGenInfo(name, gen)
    }

    override fun hasSubSelections(): Boolean {
        return false
    }

    override fun anythingSuspends(gen: CodeGen<*, CTX>): Boolean {
        return false
    }
}
