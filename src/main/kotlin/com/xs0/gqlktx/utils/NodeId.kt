package com.xs0.gqlktx.utils

import java.io.IOException
import java.io.OutputStream
import java.util.*
import kotlin.reflect.KClass

/**
 * A helper ID thing for use with Relay-compliant schemas. Contains a type part,
 * and an array of id parts, which can be/are type-specific.
 *
 *
 * The string encoding is produced by converting the whole thing into a binary encoding
 * (see [)), then base64url-encoding that.][PackedIdListWriter]
 */
class NodeId private constructor(val typeId: String, private val parts: Array<Any>, private val encoded: String) {

    fun matches(typeId: String, vararg partTypes: KClass<*>): Boolean {
        if (typeId != this.typeId)
            return false

        if (partTypes.size != parts.size)
            return false

        return parts.indices.all { partTypes[it].isInstance(parts[it]) }
    }

    fun <T: Any> getPart(i: Int, type: KClass<T>): T {
        if (type.isInstance(parts[i])) {
            @Suppress("UNCHECKED_CAST")
            return parts[i] as T
        }
        throw IllegalArgumentException("Type mismatch")
    }

    override fun toString(): String {
        return typeId + parts.joinToString(prefix="[", postfix = "]")
    }

    fun toPublicId(): String {
        return encoded
    }

    fun numParts(): Int {
        return parts.size
    }

    class Builder internal constructor(private val typeId: String) {
        private val encoded: StringOutputStream = StringOutputStream()
        private val base64: OutputStream = Base64.getEncoder().withoutPadding().wrap(encoded)
        private val partWriter: PackedIdListWriter = PackedIdListWriter(base64)
        private val parts: ArrayList<Any> = ArrayList()

        init {
            this.partWriter.writeString(typeId)
        }

        fun build(): NodeId {
            if (parts.isEmpty())
                throw IllegalStateException("No identifiers")

            base64.close()

            return NodeId(typeId, parts.toTypedArray(), encoded.toString())
        }

        fun add(value: Boolean): Builder {
            parts.add(value)
            partWriter.writeBoolean(value)
            return this
        }

        fun add(value: Byte): Builder {
            parts.add(value)
            partWriter.writeByte(value)
            return this
        }

        fun add(value: Short): Builder {
            parts.add(value)
            partWriter.writeShort(value)
            return this
        }

        fun add(value: Char): Builder {
            parts.add(value)
            partWriter.writeChar(value)
            return this
        }

        fun add(value: Int): Builder {
            parts.add(value)
            partWriter.writeInt(value)
            return this
        }

        fun add(value: Long): Builder {
            parts.add(value)
            partWriter.writeLong(value)
            return this
        }

        fun add(value: Float): Builder {
            parts.add(value)
            partWriter.writeFloat(value)
            return this
        }

        fun add(value: Double): Builder {
            parts.add(value)
            partWriter.writeDouble(value)
            return this
        }

        fun add(value: UUID): Builder {
            parts.add(value)
            partWriter.writeUUID(value)
            return this
        }

        fun add(s: String): Builder {
            parts.add(s)
            partWriter.writeString(s)
            return this
        }
    }

    companion object {
        fun create(typeId: String): Builder {
            return Builder(typeId)
        }

        fun fromPublicID(encodedId: String): NodeId {
            val reader = PackedIdListReader(Base64.getDecoder().wrap(StringInputStream(encodedId)))

            try {
                val typeIdObj = reader.readNext()
                if (typeIdObj is String) {
                    val parts = reader.readRest()
                    if (parts.isEmpty())
                        throw IllegalArgumentException("Missing id parts")

                    return NodeId(typeIdObj, parts, encodedId)
                } else {
                    throw IllegalArgumentException("NodeId didn't start with typeId (a string)")
                }
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to read encodedId", e)
            }
        }
    }
}
