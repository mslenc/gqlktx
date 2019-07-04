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

    override fun transformFromJson(array: ValueList, inputVarParser: InputVarParser<CTX>): FloatArray {
        return FloatArray(array.elements.size) { index ->
            when (val element = array.elements[index]) {
                is ValueNumber -> ScalarUtils.validateSingleFloat(element)
                is ValueNull -> throw ValidationException("Null encountered in list of non-null floats")
                is Variable -> inputVarParser.parseVar(element, NON_NULL_FLOAT_TYPE) as Float
                else -> throw ValidationException("Something other than a number encountered in list of non-null floats")
            }
        }
    }

    companion object {
        val NON_NULL_FLOAT_TYPE = Float::class.nonNullType()
    }
}
