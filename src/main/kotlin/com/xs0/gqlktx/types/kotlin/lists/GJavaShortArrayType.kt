package com.xs0.gqlktx.types.kotlin.lists

import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaListLikeType
import com.xs0.gqlktx.types.kotlin.GJavaType
import kotlin.reflect.full.createType

class GJavaShortArrayType<CTX>(gqlType: GType, elementType: GJavaType<CTX>) : GJavaListLikeType<CTX>(ShortArray::class.createType(), gqlType, elementType) {
    init {
        if ("[Int!]" != gqlType.gqlTypeString)
            throw IllegalStateException()
    }

    override fun createList(size: Int): ShortArray {
        return ShortArray(size)
    }

    override fun getListSize(list: Any): Int {
        return (list as ShortArray).size
    }

    override fun getIterator(list: Any): Iterator<*> {
        return (list as ShortArray).iterator()
    }

    override fun appendListElement(list: Any, index: Int, value: Any) {
        (list as ShortArray)[index] = (value as Number).toShort()
    }

    override fun transformFromJson(array: List<Any?>, inputVarParser: InputVarParser<CTX>): ShortArray {
        val n = array.size

        val res = ShortArray(n)
        for (i in 0 until n) {
            val o = array[i] ?: throw ValidationException("Null encountered in list of non-null ints")

            if (o is Short) {
                res[i] = o
                continue
            }

            if (o !is Number)
                throw ValidationException("Found a non-numeric value instead of int")

            if (o.toInt().toDouble() != o.toDouble())
                throw ValidationException("Value has a fraction or is out of range")

            if (o.toInt() < java.lang.Short.MIN_VALUE || o.toInt() > java.lang.Short.MAX_VALUE)
                throw ValidationException("Value out of range")

            res[i] = o.toShort()
        }

        return res
    }
}
