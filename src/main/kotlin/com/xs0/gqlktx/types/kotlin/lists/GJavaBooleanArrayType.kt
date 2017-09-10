package com.xs0.gqlktx.types.kotlin.lists

import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaListLikeType
import com.xs0.gqlktx.types.kotlin.GJavaType
import io.vertx.core.json.JsonArray
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

    @Throws(Exception::class)
    override fun transformFromJson(array: JsonArray, inputVarParser: InputVarParser<CTX>): BooleanArray {
        val n = array.size()

        val res = BooleanArray(n)
        for (i in 0 until n) {
            val bool = array.getBoolean(i)
            if (bool == null) {
                throw ValidationException("Null encountered in list of non-null booleans")
            } else {
                res[i] = bool
            }
        }

        return res
    }
}
