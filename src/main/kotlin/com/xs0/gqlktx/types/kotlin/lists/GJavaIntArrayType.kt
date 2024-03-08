package com.xs0.gqlktx.types.kotlin.lists

import com.xs0.gqlktx.codegen.*
import com.xs0.gqlktx.dom.ValueList
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.schema.builder.ResolvedName
import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.schema.builder.nonNullType
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaListLikeType
import com.xs0.gqlktx.types.kotlin.GJavaType
import kotlin.reflect.KType
import kotlin.reflect.full.createType

data class GJavaIntArrayType<CTX: Any>(override val gqlType: GType, override val elementType: GJavaType<CTX>) : GJavaListLikeType<CTX>() {
    override val type: KType = IntArray::class.createType()

    override val name = ResolvedName.forBaseline(gqlType.kind != TypeKind.NON_NULL, "IntArray")

    init {
        checkGqlType()

        if ("[Int!]" != gqlType.gqlTypeString)
            throw IllegalStateException()
    }

    override fun createList(size: Int): IntArray {
        return IntArray(size)
    }

    override fun getListSize(list: Any): Int {
        return (list as IntArray).size
    }

    override fun getIterator(list: Any): Iterator<*> {
        return (list as IntArray).iterator()
    }

    override fun appendListElement(list: Any, index: Int, value: Any) {
        (list as IntArray)[index] = (value as Number).toInt()
    }

    override fun transformFromJson(array: ValueList, inputVarParser: InputVarParser<CTX>): IntArray {
        return BaselineInputParser.parseIntArrayNotNull(array, inputVarParser.inputVariables)
    }

    override fun inputElementType(): GJavaType<CTX>? {
        return null // we process the thing as a whole
    }

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

    companion object {
        val NON_NULL_INT_TYPE = Int::class.nonNullType()
    }
}
