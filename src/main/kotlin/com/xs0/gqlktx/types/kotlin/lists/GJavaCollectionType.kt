package com.xs0.gqlktx.types.kotlin.lists

import com.xs0.gqlktx.codegen.*
import com.xs0.gqlktx.dom.ValueList
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.schema.builder.ResolvedName
import com.xs0.gqlktx.types.gql.GListType
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaListLikeType
import com.xs0.gqlktx.types.kotlin.GJavaType
import kotlin.reflect.KType

import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

data class GJavaCollectionType<CTX: Any>(val listClass: KType, override val elementType: GJavaType<CTX>, val gqlListType: GListType) : GJavaListLikeType<CTX>() {
    override val type: KType
        get() = listClass
    override val gqlType: GType
        get() = gqlListType

    private val factoryClass: KClass<*>
    private val declaredClass: KClass<*>

    init {
        checkGqlType()

        val (fc, dc) = findConcreteClasses(listClass)

        this.factoryClass = fc
        this.declaredClass = dc

        try {
            if (this.factoryClass.java.newInstance() !is Collection<*>)
                throw IllegalStateException("Not a Collection")
        } catch (e: Exception) {
            throw IllegalArgumentException("Can't construct a $factoryClass instance", e)
        }
    }

    override val name = ResolvedName(
        factoryClass.simpleName + "Of" + elementType.name.gqlName,
        elementType.name.imports + setOf(factoryClass.packageName()!! to factoryClass.simpleName!!),
        codeGenFunName = factoryClass.simpleName + "Of" + elementType.name.codeGenFunName,
        codeGenTypeNN = factoryClass.simpleName + "<" + elementType.name.codeGenType + ">",
        codeGenTypeExNN = declaredClass.simpleName + "<" + elementType.name.codeGenType + ">",
        isNullableType = true,
    )

    private fun findConcreteClasses(listClass: KType): Pair<KClass<*>, KClass<*>> {
        var klass: KClass<*>? = listClass.classifier as? KClass<*>

        if (klass != null && !Collection::class.isSuperclassOf(klass))
            klass = null

        if (klass == null)
            throw IllegalArgumentException("Can't resolve $listClass to a collection type")

        return if (klass.isAbstract) {
            if (List::class.isSuperclassOf(klass)) {
                ArrayList::class to List::class
            } else if (Set::class.isSuperclassOf(klass)) {
                LinkedHashSet::class to Set::class
            } else if (Queue::class.isSuperclassOf(klass)) {
                LinkedBlockingQueue::class to Queue::class
            } else {
                ArrayList::class to List::class
            }
        } else {
            klass to klass
        }
    }

    override fun createList(size: Int): MutableCollection<Any?> {
        try {
            return factoryClass.java.newInstance() as MutableCollection<Any?>
        } catch (e: Exception) {
            throw Error(e) // this is most unusual- we verify the thing in constructor
        }
    }

    override fun getListSize(list: Any): Int {
        return (list as Collection<*>).size
    }

    override fun getIterator(list: Any): Iterator<*> {
        return (list as Collection<*>).iterator()
    }

    override fun appendListElement(list: Any, index: Int, value: Any) {
        (list as MutableCollection<Any?>).add(value)
    }

    override fun inputElementType(): GJavaType<CTX> {
        return elementType
    }

    override fun hasSubSelections(): Boolean {
        return elementType.hasSubSelections()
    }

    override fun inputParseInfo(gen: CodeGen<*, CTX>): InputParseCodeGenInfo {
        val sub = elementType.inputParseInfo(gen)

        return InputParseCodeGenInfo(
            kind = InputParseKind.COLLECTION_OF,
            funName = factoryClass.simpleName + "Of" + sub.funName,
            funReturnType = name.codeGenType,
            funCreateType = name.codeGenTypeNN,
            outPackageName = sub.outPackageName,
            exprTemplate = "parse" + factoryClass.simpleName + "Of" + sub.funName + "(VALUE, variables)",
            importsForGen = sub.importsForUse,
            importsForUse = setOf(sub.outPackageName to "parse" + factoryClass.simpleName + "Of" + sub.funName),
        )
    }

    override fun outputExportInfo(gen: CodeGen<*, CTX>): OutputExportCodeGenInfo {
        val sub = elementType.outputExportInfo(gen)

        return sub.buildWrapper(
            OutputExportKind.COLLECTION_OF,
            declaredClass.simpleName + "Of",
            elementType.hasSubSelections(),
            "List<" + sub.funReturnType + ">"
        )
    }

    override fun transformFromJson(array: ValueList, inputVarParser: InputVarParser<CTX>): Collection<Any?> {
        val elements = array.elements
        val res = createList(elements.size)

        elements.forEach {
            res.add(inputVarParser.parseVar(it, elementType))
        }

        return res
    }

    override fun anythingSuspends(gen: CodeGen<*, CTX>): Boolean {
        return elementType.suspendingOutput
    }
}
