package com.xs0.gqlktx.types.kotlin.lists

import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaListLikeType
import com.xs0.gqlktx.types.kotlin.GJavaType
import io.vertx.core.json.JsonArray
import kotlin.reflect.full.createType

class GJavaLongArrayType<CTX>(gqlType: GType, elementType: GJavaType<CTX>) : GJavaListLikeType<CTX>(LongArray::class.createType(), gqlType, elementType) {
    init {
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

    override fun transformFromJson(array: JsonArray, inputVarParser: InputVarParser<CTX>): LongArray {
        val n = array.size()

        val res = LongArray(n)
        for (i in 0 until n) {
            val o = array.getValue(i) ?: throw ValidationException("Null encountered in list of non-null longs")

            if (o is Long) {
                res[i] = o
                continue
            }

            if (o !is Number)
                throw ValidationException("Found a non-numeric value instead of long")

            if (o is Byte || o is Short || o is Int) {
                res[i] = o.toLong()
                continue
            }

            if (o is Float || o is Double) {
                val d = o.toDouble()
                if ((d < java.lang.Long.MIN_VALUE) or (d > java.lang.Long.MAX_VALUE))
                    throw ValidationException("Value out of range")
                if (d != d.toLong().toDouble())
                    throw ValidationException("Value is not representable as a long (it probably has a fraction)")
                res[i] = o.toLong()
                continue
            }

            throw ValidationException("Unrecognized type of number encountered")
        }

        return res
    }
}
