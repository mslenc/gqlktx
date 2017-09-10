package com.xs0.gqlktx.schema

import com.xs0.gqlktx.SyncInvokable
import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.schema.intro.GqlIntroSchema
import com.xs0.gqlktx.types.gql.GBaseType
import com.xs0.gqlktx.types.kotlin.GJavaType

import java.util.LinkedHashMap
import java.util.TreeMap
import kotlin.reflect.KType

class Schema<SCHEMA, CTX>(
        val queryRoot: SyncInvokable<SCHEMA>,
        val mutationRoot: SyncInvokable<SCHEMA>?,
        private val types: Map<KType, GJavaType<CTX>>,
        private val baseTypes: Map<String, GBaseType>) {

    private val introspector: GqlIntroSchema

    init {
        this.introspector = GqlIntroSchema(this)
    }

    fun introspector(): GqlIntroSchema {
        return introspector
    }

    fun getJavaType(type: KType): GJavaType<CTX> {
        return types[type] ?: throw UnsupportedOperationException("For some reason, type $type is not supported")
    }

    fun getGQLBaseType(name: String): GBaseType {
        return baseTypes[name] ?: throw UnsupportedOperationException("For some reason, GQL type $name is not supported")
    }

    override fun toString(): String {
        val sb = StringBuilder()

        sb.append("schema {\n")
        sb.append("  query: ").append(types[queryRoot.type]?.gqlType?.gqlTypeString).append("\n")
        if (mutationRoot != null)
            sb.append("  mutation: ").append(types[mutationRoot.type]?.gqlType?.gqlTypeString).append("\n")
        sb.append("}\n")

        val typesByKind = LinkedHashMap<TypeKind, TreeMap<String, GBaseType>>()
        for (kind in TypeKind.values())
            typesByKind.put(kind, TreeMap())

        for ((key, value) in baseTypes)
            if (!key.startsWith("__"))
                typesByKind[value.kind]?.put(key, value)

        for ((key, value) in typesByKind) {
            if (value.isEmpty())
                continue

            sb.append("\n")
            for (type in value.values) {
                type.toString(sb)
                if (key != TypeKind.SCALAR)
                    sb.append("\n")
            }
        }

        return sb.toString()
    }

    val allBaseTypes: Collection<GBaseType>
        get() = baseTypes.values
}
