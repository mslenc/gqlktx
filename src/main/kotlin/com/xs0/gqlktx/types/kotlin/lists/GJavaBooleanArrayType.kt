package com.xs0.gqlktx.types.kotlin.lists

import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.dom.*
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.schema.builder.nonNullType
import com.xs0.gqlktx.schema.builder.nullableType
import com.xs0.gqlktx.types.gql.GType
import com.xs0.gqlktx.types.kotlin.GJavaListLikeType
import com.xs0.gqlktx.types.kotlin.GJavaType
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

    override fun transformFromJson(array: ValueList, inputVarParser: InputVarParser<CTX>): BooleanArray {
        return BooleanArray(array.elements.size) { index ->
            when (val element = array.elements[index]) {
                is ValueBool -> element.value
                is ValueNull -> throw ValidationException("Null encountered in list of non-null booleans")
                is Variable -> inputVarParser.parseVar(element, NON_NULL_BOOL_TYPE) as Boolean
                else -> throw ValidationException("A non-boolean encountered in list of non-null booleans")
            }
        }
    }

    companion object {
        val NON_NULL_BOOL_TYPE = Boolean::class.nonNullType()
        val NULLABLE_BOOL_TYPE = Boolean::class.nullableType()
    }
}
