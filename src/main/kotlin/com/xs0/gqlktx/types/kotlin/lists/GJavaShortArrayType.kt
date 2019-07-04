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

    override fun transformFromJson(array: ValueList, inputVarParser: InputVarParser<CTX>): ShortArray {
        return ShortArray(array.elements.size) { index ->
            when (val element = array.elements[index]) {
                is ValueNumber -> ScalarUtils.validateShort(element)
                is ValueNull -> throw ValidationException("Null encountered in list of non-null shorts")
                is Variable -> inputVarParser.parseVar(element, NON_NULL_SHORT_TYPE) as Short
                else -> throw ValidationException("Something other than a number encountered in list of non-null shorts")
            }
        }
    }

    companion object {
        val NON_NULL_SHORT_TYPE = Short::class.nonNullType()
    }
}