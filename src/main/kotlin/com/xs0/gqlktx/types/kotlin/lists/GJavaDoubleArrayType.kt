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

    override fun transformFromJson(array: ValueList, inputVarParser: InputVarParser<CTX>): DoubleArray {
        return DoubleArray(array.elements.size) { index ->
            when (val element = array.elements[index]) {
                is ValueNumber -> ScalarUtils.validateFloat(element)
                is ValueNull -> throw ValidationException("Null encountered in list of non-null doubles")
                is Variable -> inputVarParser.parseVar(element, NON_NULL_DOUBLE_TYPE) as Double
                else -> throw ValidationException("Something other than a number encountered in list of non-null doubles")
            }
        }
    }

    companion object {
        val NON_NULL_DOUBLE_TYPE = Double::class.nonNullType()
    }
}
