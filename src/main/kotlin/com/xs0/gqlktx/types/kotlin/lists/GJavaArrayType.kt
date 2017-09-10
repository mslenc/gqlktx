package com.xs0.gqlktx.types.kotlin.lists

import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaListLikeType
import com.xs0.gqlktx.types.kotlin.GJavaType
import io.vertx.core.json.JsonArray

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

    override fun getListSize(array: Any): Int {
        return javaArray.getLength(array)
    }

    override fun getIterator(array: Any): Iterator<*> {
        return (array as kotlin.Array<*>).iterator()
    }

    override fun appendListElement(array: Any, index: Int, value: Any) {
        javaArray.set(array, index, value)
    }

    override fun transformFromJson(array: JsonArray, inputVarParser: InputVarParser<CTX>): Any {
        val n = array.size()
        val res = createList(array.size())

        for (i in 0 until n)
            javaArray.set(res, i, elementType.getFromJson(array.getValue(i), inputVarParser))

        return res
    }
}
