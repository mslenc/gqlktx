package com.xs0.gqlktx.types.gql

import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.schema.intro.GqlIntroType
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

abstract class GType protected constructor() {
    val introspector = GqlIntroType(this)

    abstract val kind: TypeKind
    abstract val gqlTypeString: String

    abstract val validAsArgumentType: Boolean
    abstract val validAsQueryFieldType: Boolean

    abstract val baseType: GBaseType

    private var myNotNullType: GNotNullType? = null
    fun notNull(): GNotNullType {
        var cached = myNotNullType
        if (cached == null) {
            cached = GNotNullType(this)
            myNotNullType = cached
        }
        return cached
    }

    private var myListType: GListType? = null
    fun listOf(): GListType {
        var cached = myListType
        if (cached == null) {
            cached = GListType(this)
            myListType = cached
        }
        return cached
    }

    abstract fun coerceValue(raw: JsonObject, key: String, out: JsonObject)

    abstract fun coerceValue(raw: JsonArray, index: Int, out: JsonArray)
}