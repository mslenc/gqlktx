package com.xs0.gqlktx.types.kotlin

import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.dom.ValueList
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.types.gql.GType

import kotlin.reflect.KType

abstract class GJavaListLikeType<CTX>(type: KType, gqlType: GType, val elementType: GJavaType<CTX>) : GJavaType<CTX>(type, gqlType) {
    init {
        if (gqlType.kind != TypeKind.LIST)
            throw IllegalStateException("Expected a list type")
    }

    override fun checkUsage(isInput: Boolean) {
        elementType.checkUsage(isInput)
    }

    /**
     * Creates a new instance of the underlying list/array type of the
     * given size.
     */
    abstract fun createList(size: Int): Any

    /**
     * Retrieves the size of the list parameter.
     */
    abstract fun getListSize(list: Any): Int

    /**
     * Creates an iterator over the provided list/array.
     */
    abstract fun getIterator(list: Any): Iterator<*>

    /**
     * Appends an element to the end of the list. The index is provided as
     * a convenience for array-like containers.
     */
    abstract fun appendListElement(list: Any, index: Int, value: Any)

    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): Any {
        val array = value as? ValueList ?: throw ValidationException("Expected a list, but got something else")

        return transformFromJson(array, inputVarParser)
    }

    protected abstract fun transformFromJson(array: ValueList, inputVarParser: InputVarParser<CTX>): Any
}
