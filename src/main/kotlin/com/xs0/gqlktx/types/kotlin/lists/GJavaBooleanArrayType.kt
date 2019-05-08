package com.xs0.gqlktx.types.kotlin.lists

import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaListLikeType
import com.xs0.gqlktx.types.kotlin.GJavaType
import kotlin.reflect.full.createType

class GJavaBooleanArrayType<CTX>(gqlType: GType, elementType: GJavaType<CTX>) : GJavaListLikeType<CTX>(BooleanArray::class.createType(), gqlType, elementType) {
    init {
        if ("[Boolean!]" != gqlType.gqlTypeString)
            throw IllegalStateException()
    }

    override fun createList(size: Int): BooleanArray {
        return BooleanArray(size)
    }

    override fun getListSize(list: Any): Int {
        return (list as BooleanArray).size
    }

    override fun getIterator(list: Any): Iterator<*> {
        return (list as BooleanArray).iterator()
    }

    override fun appendListElement(list: Any, index: Int, value: Any) {
        (list as BooleanArray)[index] = value as Boolean
    }

    override fun transformFromJson(array: List<Any?>, inputVarParser: InputVarParser<CTX>): BooleanArray {
        val n = array.size

        val res = BooleanArray(n)
        for (i in 0 until n) {
            when (val el = array[i]) {
                null -> throw ValidationException("Null encountered in list of non-null booleans")
                is Boolean -> res[i] = el
                else -> throw ValidationException("A non-boolean encountered in list of non-null booleans")
            }
        }

        return res
    }
}
