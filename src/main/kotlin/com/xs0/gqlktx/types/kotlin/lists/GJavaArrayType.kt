package com.xs0.gqlktx.types.kotlin.lists

import com.xs0.gqlktx.codegen.*
import com.xs0.gqlktx.dom.ValueList
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.schema.builder.ResolvedName
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaListLikeType
import com.xs0.gqlktx.types.kotlin.GJavaType

import kotlin.reflect.KClass
import kotlin.reflect.KType

typealias javaArray = java.lang.reflect.Array

class GJavaArrayType<CTX: Any>(override val type: KType, override val elementType: GJavaType<CTX>, override val gqlType: GType) : GJavaListLikeType<CTX>() {
    private val concreteElementType: Class<*>

    override val name = ResolvedName(
        gqlName = "ArrayOf" + elementType.name.gqlName,
        elementType.name.imports,
        codeGenFunName = "ArrayOf" + elementType.name.codeGenFunName,
        codeGenTypeNN = "Array<" + elementType.name.codeGenType + ">",
        isNullableType = true,
    )

    init {
        checkGqlType()

        val cl = type.classifier
        if (cl !is KClass<*> || type.arguments.size != 1)
            throw IllegalStateException("Not a class type: $cl")
        if (!cl.java.isArray)
            throw IllegalStateException("Not an class type: $cl")

        concreteElementType = cl.java.componentType
    }

    override fun createList(size: Int): Any {
        return javaArray.newInstance(concreteElementType, size)
    }

    override fun getListSize(list: Any): Int {
        return javaArray.getLength(list)
    }

    override fun getIterator(list: Any): Iterator<*> {
        return (list as Array<*>).iterator()
    }

    override fun appendListElement(list: Any, index: Int, value: Any) {
        javaArray.set(list, index, value)
    }

    override fun inputElementType(): GJavaType<CTX> {
        return elementType
    }

    override fun transformFromJson(array: ValueList, inputVarParser: InputVarParser<CTX>): Any {
        val elements = array.elements
        val n = elements.size
        val res = createList(n)

        for (i in 0 until n) {
            javaArray.set(res, i, inputVarParser.parseVar(elements[i], elementType))
        }

        return res
    }

    override fun hasSubSelections(): Boolean {
        return elementType.hasSubSelections()
    }

    override fun inputParseInfo(gen: CodeGen<*, CTX>): InputParseCodeGenInfo {
        val sub = elementType.inputParseInfo(gen)
        return InputParseCodeGenInfo(
            kind = InputParseKind.ARRAY_OF,
            funName = "ArrayOf" + sub.funName,
            funReturnType = name.codeGenType,
            funCreateType = name.codeGenTypeNN,
            outPackageName = sub.outPackageName,
            exprTemplate = "parseArrayOf" + sub.funName + "(VALUE, variables)",
            importsForGen = sub.importsForUse,
            importsForUse = setOf(sub.outPackageName to "parseArrayOf" + sub.funName),
        )
    }

    override fun outputExportInfo(gen: CodeGen<*, CTX>): OutputExportCodeGenInfo {
        val sub = elementType.outputExportInfo(gen)

        return sub.buildWrapper(
            OutputExportKind.ARRAY_OF,
            "ArrayOf",
            elementType.hasSubSelections(),
            "List<" + sub.funReturnType + ">"
        )
    }

    override fun anythingSuspends(gen: CodeGen<*, CTX>): Boolean {
        return elementType.suspendingOutput
    }
}
