package com.xs0.gqlktx.schema

import com.xs0.gqlktx.*
import com.xs0.gqlktx.exec.findMethod
import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.schema.intro.GqlIntroSchema
import com.xs0.gqlktx.schema.intro.GqlIntroType
import com.xs0.gqlktx.types.gql.GBaseType
import com.xs0.gqlktx.types.kotlin.GJavaType

import java.util.LinkedHashMap
import java.util.TreeMap
import kotlin.reflect.KType
import kotlin.reflect.full.createType

class Schema<SCHEMA, CTX>(
        val queryRoot: SyncInvokable<SCHEMA>,
        val mutationRoot: SyncInvokable<SCHEMA>?,
        private val types: Map<KType, GJavaType<CTX>>,
        private val baseTypes: Map<String, GBaseType>) {

    internal val INTRO_SCHEMA: FieldGetter<CTX>
    internal val INTRO_TYPE: FieldGetter<CTX>
    internal val INTRO_TYPENAME: FieldGetter<CTX>

    init {
        try {
            INTRO_SCHEMA = FieldGetterRegularFunction(
                    SemiType.create(GqlIntroSchema::class.createType(nullable=false)) ?: throw Error("Couldn't process __schema type"),
                    "__schema",
                    GqlIntroSchema::class.findMethod("self"),
                    arrayOf(ParamInfo(ParamKind.THIS)),
                    emptyMap()
            )

            val nonNullString = SemiType.create(String::class.createType(nullable = false))!!
            val nameParam = ParamInfo<CTX>("type", nonNullString)

            INTRO_TYPE = FieldGetterRegularFunction(
                    SemiType.create(GqlIntroType::class.createType(nullable=true)) ?: throw Error("Couldn't process __type"),
                    "__type",
                    GqlIntroSchema::class.findMethod("type"),
                    arrayOf(ParamInfo(ParamKind.THIS), nameParam),
                    mapOf("name" to PublicParamInfo("name", nonNullString, null))
            )

            INTRO_TYPENAME = FieldGetterRegularFunction(
                    nonNullString,
                    "__typename",
                    Any::class.findMethod("toString"),
                    arrayOf(ParamInfo(ParamKind.THIS)),
                    emptyMap()
            )
        } catch (e: Exception) {
            throw Error(e)
        }
    }

    private val introspector: GqlIntroSchema

    init {
        this.introspector = GqlIntroSchema(this)
    }

    fun introspector(): GqlIntroSchema {
        return introspector
    }

    fun getJavaType(type: KType): GJavaType<CTX> {
        val res = types[type]
        if (res != null)
            return res
        throw UnsupportedOperationException("For some reason, type $type is not supported")
    }

    fun getGQLBaseType(name: String): GBaseType {
        return getGQLBaseTypeMaybe(name) ?: throw UnsupportedOperationException("For some reason, GQL type $name is not supported")
    }

    fun getGQLBaseTypeMaybe(name: String): GBaseType? {
        return baseTypes[name]
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
