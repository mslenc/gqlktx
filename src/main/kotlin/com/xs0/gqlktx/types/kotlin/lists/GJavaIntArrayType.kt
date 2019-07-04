package com.xs0.gqlktx.types.kotlin.lists

import com.xs0.gqlktx.ScalarUtils
import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.dom.ValueList
import com.xs0.gqlktx.dom.ValueNull
import com.xs0.gqlktx.dom.ValueNumber
import com.xs0.gqlktx.dom.Variable
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.schema.builder.nonNullType
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaListLikeType
import com.xs0.gqlktx.types.kotlin.GJavaType
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

    override fun transformFromJson(array: ValueList, inputVarParser: InputVarParser<CTX>): IntArray {
        return IntArray(array.elements.size) { index ->
            when (val element = array.elements[index]) {
                is ValueNumber -> ScalarUtils.validateInteger(element)
                is ValueNull -> throw ValidationException("Null encountered in list of non-null ints")
                is Variable -> inputVarParser.parseVar(element, NON_NULL_INT_TYPE) as Int
                else -> throw ValidationException("Something other than a number encountered in list of non-null ints")
            }
        }
    }

    companion object {
        val NON_NULL_INT_TYPE = Int::class.nonNullType()
    }
}
