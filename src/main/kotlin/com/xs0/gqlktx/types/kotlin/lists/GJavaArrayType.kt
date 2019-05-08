package com.xs0.gqlktx.types.kotlin.lists

import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaListLikeType
import com.xs0.gqlktx.types.kotlin.GJavaType

import kotlin.reflect.KClass
import kotlin.reflect.KType

typealias javaArray = java.lang.reflect.Array

class GJavaArrayType<CTX>(type: KType, elementType: GJavaType<CTX>, gqlType: GType) : GJavaListLikeType<CTX>(type, gqlType, elementType) {
    private val concreteElementType: Class<*>

    init {
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

    override fun transformFromJson(array: List<Any?>, inputVarParser: InputVarParser<CTX>): Any {
        val n = array.size
        val res = createList(n)

        for (i in 0 until n) {
            val el = array[i]
            if (el == null) {
                if (!elementType.isNullAllowed())
                    throw ValidationException("null encountered in list of non-nulls")
            } else {
                javaArray.set(res, i, elementType.getFromJson(el, inputVarParser))
            }
        }


        return res
    }
}
