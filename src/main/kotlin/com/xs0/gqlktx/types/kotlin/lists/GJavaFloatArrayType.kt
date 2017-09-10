package com.xs0.gqlktx.types.kotlin.lists

import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaListLikeType
import com.xs0.gqlktx.types.kotlin.GJavaType
import io.vertx.core.json.JsonArray
import kotlin.reflect.full.createType

class GJavaFloatArrayType<CTX>(gqlType: GType, elType: GJavaType<CTX>) : GJavaListLikeType<CTX>(FloatArray::class.createType(), gqlType, elType) {
    init {
        if ("[Float!]" != gqlType.gqlTypeString)
            throw IllegalStateException()
    }

    override fun createList(size: Int): FloatArray {
        return FloatArray(size)
    }

    override fun getListSize(list: Any): Int {
        return (list as FloatArray).size
    }

    override fun getIterator(list: Any): Iterator<*> {
        return (list as FloatArray).iterator()
    }

    override fun appendListElement(list: Any, index: Int, value: Any) {
        (list as FloatArray)[index] = (value as Number).toFloat()
    }

    override fun transformFromJson(array: JsonArray, inputVarParser: InputVarParser<CTX>): FloatArray {
        val n = array.size()

        val res = FloatArray(n)
        for (i in 0 until n) {
            val d = array.getDouble(i) ?: throw ValidationException("Null encountered in list of non-null floats")
            if (d.isNaN())
                throw ValidationException("NaN encountered in list of non-null floats")

            if (d < -java.lang.Float.MAX_VALUE || d > java.lang.Float.MAX_VALUE)
                throw ValidationException("Value out of range")

            res[i] = d.toFloat()
        }

        return res
    }
}
