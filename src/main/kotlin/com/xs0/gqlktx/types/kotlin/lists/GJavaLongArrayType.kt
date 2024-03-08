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

data class GJavaLongArrayType<CTX: Any>(override val gqlType: GType, override val elementType: GJavaType<CTX>) : GJavaListLikeType<CTX>() {
    override val type: KType = LongArray::class.createType()

    override val name = ResolvedName.forBaseline(gqlType.kind != TypeKind.NON_NULL, "LongArray")

    init {
        checkGqlType()

        if ("[Long!]" != gqlType.gqlTypeString)
            throw IllegalStateException()
    }

    override fun createList(size: Int): LongArray {
        return LongArray(size)
    }

    override fun getListSize(list: Any): Int {
        return (list as LongArray).size
    }

    override fun getIterator(list: Any): Iterator<*> {
        return (list as LongArray).iterator()
    }

    override fun appendListElement(list: Any, index: Int, value: Any) {
        (list as LongArray)[index] = (value as Number).toLong()
    }

    override fun transformFromJson(array: ValueList, inputVarParser: InputVarParser<CTX>): LongArray {
        return BaselineInputParser.parseLongArrayNotNull(array, inputVarParser.inputVariables)
    }

    override fun inputParseInfo(gen: CodeGen<*, CTX>): InputParseCodeGenInfo {
        return BaselineInputParser.codeGenInfo(name, gen)
    }

    override fun outputExportInfo(gen: CodeGen<*, CTX>): OutputExportCodeGenInfo {
        return BaselineExporter.codeGenInfo(name, gen)
    }

    override fun inputElementType(): GJavaType<CTX>? {
        return null // we process the thing as a whole
    }

    override fun hasSubSelections(): Boolean {
        return false
    }

    override fun anythingSuspends(gen: CodeGen<*, CTX>): Boolean {
        return false
    }

    companion object {
        val NON_NULL_LONG_TYPE = Long::class.nonNullType()
    }
}
