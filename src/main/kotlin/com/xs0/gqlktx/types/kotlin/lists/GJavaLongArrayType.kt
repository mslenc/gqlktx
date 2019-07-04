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

    override fun transformFromJson(array: ValueList, inputVarParser: InputVarParser<CTX>): LongArray {
        return LongArray(array.elements.size) { index ->
            when (val element = array.elements[index]) {
                is ValueNumber -> ScalarUtils.validateLong(element)
                is ValueNull -> throw ValidationException("Null encountered in list of non-null longs")
                is Variable -> inputVarParser.parseVar(element, NON_NULL_LONG_TYPE) as Long
                else -> throw ValidationException("Something other than a number encountered in list of non-null longs")
            }
        }
    }

    companion object {
        val NON_NULL_LONG_TYPE = Long::class.nonNullType()
    }
}
