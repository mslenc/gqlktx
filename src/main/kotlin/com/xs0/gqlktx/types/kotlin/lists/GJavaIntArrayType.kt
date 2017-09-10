package com.xs0.gqlktx.types.kotlin.lists

import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaListLikeType
import com.xs0.gqlktx.types.kotlin.GJavaType
import io.vertx.core.json.JsonArray
import kotlin.reflect.full.createType

class GJavaIntArrayType<CTX>(gqlType: GType, elementType: GJavaType<CTX>) : GJavaListLikeType<CTX>(IntArray::class.createType(), gqlType, elementType) {
    init {
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

    override fun transformFromJson(array: JsonArray, inputVarParser: InputVarParser<CTX>): IntArray {
        val n = array.size()

        val res = IntArray(n)
        for (i in 0 until n) {
            val o = array.getValue(i) ?: throw ValidationException("Null encountered in list of non-null ints")

            if (o is Int) {
                res[i] = o
                continue
            }

            if (o !is Number)
                throw ValidationException("Found a non-numeric value instead of int")

            if (o.toInt().toDouble() != o.toDouble())
                throw ValidationException("Value has a fraction or is out of range")

            res[i] = o.toInt()
        }

        return res
    }
}
