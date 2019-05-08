package com.xs0.gqlktx.types.kotlin.lists

import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaListLikeType
import com.xs0.gqlktx.types.kotlin.GJavaType
import kotlin.reflect.full.createType

class GJavaDoubleArrayType<CTX>(gqlType: GType, elType: GJavaType<CTX>) : GJavaListLikeType<CTX>(DoubleArray::class.createType(), gqlType, elType) {
    init {
        if ("[Float!]" != gqlType.gqlTypeString)
            throw IllegalStateException()
    }

    override fun createList(size: Int): DoubleArray {
        return DoubleArray(size)
    }

    override fun getListSize(list: Any): Int {
        return (list as DoubleArray).size
    }

    override fun getIterator(list: Any): Iterator<*> {
        return (list as DoubleArray).iterator()
    }

    override fun appendListElement(list: Any, index: Int, value: Any) {
        (list as DoubleArray)[index] = (value as Number).toDouble()
    }

    override fun transformFromJson(array: List<Any?>, inputVarParser: InputVarParser<CTX>): DoubleArray {
        val n = array.size

        val res = DoubleArray(n)
        for (i in 0 until n) {
            when (val el = array[i]) {
                is Number -> res[i] = el.toDouble()
                null -> throw ValidationException("Null encountered in list of non-null floats")
                else -> throw ValidationException("Something other than a number encountered in list of non-null floats")
            }
        }

        return res
    }
}
