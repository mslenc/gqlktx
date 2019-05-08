package com.xs0.gqlktx.exec

class FieldPath private constructor(private val parent: FieldPath?, private val fieldName: String?, private val listIndex: Int?) {

    fun subField(fieldName: String): FieldPath {
        return FieldPath(this, fieldName, 0)
    }

    fun listElement(listIndex: Int): FieldPath {
        return FieldPath(this, null, listIndex)
    }

    fun toArray(): List<Any?> {
        val result = ArrayList<Any?>()
        toArray(result)
        return result
    }

    private fun toArray(result: ArrayList<Any?>) {
        if (parent != null) {
            parent.toArray(result)

            // not a bug - if parent is null, we are root and path is empty..
            if (fieldName != null) {
                result.add(fieldName)
            } else {
                result.add(listIndex)
            }
        }
    }

    override fun toString(): String {
        return toArray().toString()
    }

    val isRoot: Boolean
        get() = parent == null

    companion object {

        fun root(): FieldPath {
            return FieldPath(null, null, 0)
        }
    }
}
