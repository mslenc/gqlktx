package com.xs0.gqlktx.schema.builder

import com.xs0.gqlktx.SyncInvokable
import com.xs0.gqlktx.schema.Schema
import com.xs0.gqlktx.types.gql.GBaseType
import com.xs0.gqlktx.types.gql.GScalarType
import com.xs0.gqlktx.types.kotlin.GJavaType
import kotlin.reflect.KType

import java.util.LinkedHashMap
import kotlin.reflect.KClass

class SchemaBuilder<SCHEMA: Any, CTX: Any>(
        val schemaClass: KClass<SCHEMA>,
        val contextClass: KClass<CTX>) {

    private lateinit var getQueryRoot: SyncInvokable<SCHEMA>
    private var getMutationRoot: SyncInvokable<SCHEMA>? = null
    private val types = LinkedHashMap<KType, GJavaType<CTX>>()
    private val baseTypes = LinkedHashMap<String, GBaseType>()

    /**
     * Creates a new GraphQL scalar type of the given name, and adds it to the registry.
     *
     * @param name the name of the new scalar type, which must be a valid GraphQL name and previously unused
     * @param varValueValidator a validator function, which takes a value obtained from JSON, verifies it is valid, and coerces it to the expected type (e.g. for Int, the validator would check that the value is a number which fits into 32 bits and has no non-zero fraction; it would also convert it to Integer, if needed)
     * @return the new type
     */
    fun createScalarType(name: String, varValueValidator: (Any)->Any): GScalarType {
        return addBaseType(GScalarType(name, varValueValidator))
    }

    /**
     * Registers a new base type. Its name must be unused.
     *
     * @param baseType the new base type
     * @return the same base type
     */
    fun <T : GBaseType> addBaseType(baseType: T): T {
        val name = baseType.name

        if (baseTypes.containsKey(name))
            throw IllegalStateException("Type $name is already defined")

        baseTypes.put(name, baseType)
        return baseType
    }

    fun build(): Schema<SCHEMA, CTX> {
        return Schema(getQueryRoot, getMutationRoot, types, baseTypes)
    }

    fun add(javaType: GJavaType<CTX>) {
        if (types.containsKey(javaType.type))
            throw IllegalStateException("Type " + javaType.type + " is already defined")

        types.put(javaType.type, javaType)
    }

    fun getBaseType(name: String): GBaseType? {
        return baseTypes[name]
    }

    fun setRoots(getQueryRoot: SyncInvokable<SCHEMA>, getMutationRoot: SyncInvokable<SCHEMA>?): SchemaBuilder<SCHEMA, CTX> {
        this.getQueryRoot = getQueryRoot
        this.getMutationRoot = getMutationRoot
        return this
    }

    fun getJavaType(type: KClass<*>): GJavaType<CTX>? {
        return types[type.nullableType()]
    }

    fun getJavaType(type: KType): GJavaType<CTX>? {
        return types[type]
    }

    fun hasJavaType(type: KType): Boolean {
        return types.containsKey(type)
    }
}
