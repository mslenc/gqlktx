package com.xs0.gqlktx.codegen

import com.fasterxml.jackson.core.type.TypeReference
import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.schema.intro.GqlIntroDirectiveLocation
import java.io.InputStream

data class EnumValueIntroData(
    val name: String,
    val isDeprecated: Boolean = false,
    val description: String? = null,
    val deprecationReason: String? = null,
)

data class InputValueIntroData(
    val name: String,
    val typeName: String,
    val description: String? = null,
    val defaultValue: String? = null,
)

data class FieldIntroData(
    val name: String,
    val typeName: String,
    val args: List<InputValueIntroData> = emptyList(),
    val description: String? = null,
    val isDeprecated: Boolean = false,
    val deprecationReason: String? = null,
)

data class DirectiveIntroData(
    val name: String,
    val locations: List<GqlIntroDirectiveLocation>,
    val description: String,
    val args: List<InputValueIntroData>,
)

abstract class BaseIntroData {
    val allTypeNames by lazy { buildAllTypeNames() }

    private fun buildAllTypeNames(): List<String> {
        val out = ArrayList<String>()

        getSpecificTypeNames(out)

        out += "__Directive"
        out += "__DirectiveLocation"
        out += "__EnumValue"
        out += "__Field"
        out += "__InputValue"
        out += "__Schema"
        out += "__Type"
        out += "__TypeKind"

        return out
    }
    protected abstract fun getSpecificTypeNames(out: ArrayList<String>)

    abstract fun getQueryTypeName(): String
    abstract fun getMutationTypeName(): String?
    abstract fun getSubscriptionTypeName(): String?

    open fun getEnumValues(typeName: String, includeDeprecated: Boolean): List<EnumValueIntroData>? {
        return when (typeName) {
            "__TypeKind" -> listOf(
                EnumValueIntroData("SCALAR"),
                EnumValueIntroData("ENUM"),
                EnumValueIntroData("INPUT_OBJECT"),
                EnumValueIntroData("INTERFACE"),
                EnumValueIntroData("OBJECT"),
                EnumValueIntroData("UNION"),
                EnumValueIntroData("NON_NULL"),
                EnumValueIntroData("LIST"),
            )
            "__DirectiveLocation" -> listOf(
                EnumValueIntroData("QUERY"),
                EnumValueIntroData("MUTATION"),
                EnumValueIntroData("SUBSCRIPTION"),
                EnumValueIntroData("FIELD"),
                EnumValueIntroData("FRAGMENT_DEFINITION"),
                EnumValueIntroData("FRAGMENT_SPREAD"),
                EnumValueIntroData("INLINE_FRAGMENT"),
            )
            else -> null
        }
    }

    open fun getFields(typeName: String, includeDeprecated: Boolean): List<FieldIntroData>? {
        return when (typeName) {
            "__Directive" -> listOf(
                FieldIntroData(
                    "name",
                    "String!",
                ),
                FieldIntroData(
                    "description",
                    "String!",
                ),
                FieldIntroData(
                    "locations",
                    "[__DirectiveLocation!]!"
                ),
                FieldIntroData(
                    "args",
                    "[__InputValue!]!"
                ),
            )
            "__EnumValue" -> listOf(
                FieldIntroData(
                    "name",
                    "String!",
                ),
                FieldIntroData(
                    "description",
                    "String",
                ),
                FieldIntroData(
                    "isDeprecated",
                    "Boolean!",
                ),
                FieldIntroData(
                    "deprecationReason",
                    "String",
                )
            )
            "__Field" -> listOf(
                FieldIntroData(
                    "name",
                    "String!",
                ),
                FieldIntroData(
                    "description",
                    "String",
                ),
                FieldIntroData(
                    "isDeprecated",
                    "Boolean!",
                ),
                FieldIntroData(
                    "deprecationReason",
                    "String",
                ),
                FieldIntroData(
                    "type",
                    "__Type!",
                ),
                FieldIntroData(
                    "args",
                    "[__InputValue!]!"
                )
            )
            "__InputValue" -> listOf(
                FieldIntroData(
                    "name",
                    "String!",
                ),
                FieldIntroData(
                    "description",
                    "String",
                ),
                FieldIntroData(
                    "type",
                    "__Type!",
                ),
                FieldIntroData(
                    "defaultValue",
                    "String",
                ),
            )
            "__Schema" -> listOf(
                FieldIntroData(
                    "directives",
                    "[__Directive!]!"
                ),
                FieldIntroData(
                    "mutationType",
                    "__Type",
                ),
                FieldIntroData(
                    "queryType",
                    "__Type!",
                ),
                FieldIntroData(
                    "subscriptionType",
                    "__Type",
                ),
                FieldIntroData(
                    "types",
                    "[__Type!]!",
                    listOf(
                        InputValueIntroData(
                            "kinds",
                            "[__TypeKind!]",
                        )
                    ),
                )
            )
            "__Type" -> listOf(
                FieldIntroData(
                    "description",
                    "String",
                ),
                FieldIntroData(
                    "inputFields",
                    "[__InputValue!]",
                ),
                FieldIntroData(
                    "interfaces",
                    "[__Type!]",
                ),
                FieldIntroData(
                    "kind",
                    "__TypeKind!",
                ),
                FieldIntroData(
                    "name",
                    "String!",
                ),
                FieldIntroData(
                    "ofType",
                    "__Type",
                ),
                FieldIntroData(
                    "possibleTypes",
                    "[__Type!]",
                ),
                FieldIntroData(
                    "enumValues",
                    "[__EnumValue!]",
                    listOf(
                        InputValueIntroData(
                            "includeDeprecated",
                            "Boolean!",
                            defaultValue = "false",
                        )
                    ),
                ),
                FieldIntroData(
                    "fields",
                    "[__Field!]",
                    listOf(
                        InputValueIntroData(
                            "includeDeprecated",
                            "Boolean!",
                            defaultValue = "false",
                        )
                    ),
                )
            )
            else -> null
        }
    }

    open fun getTypeKind(typeName: String): TypeKind {
        return when (typeName) {
            "__Directive" -> TypeKind.OBJECT
            "__DirectiveLocation" -> TypeKind.ENUM
            "__EnumValue" -> TypeKind.OBJECT
            "__Field" -> TypeKind.OBJECT
            "__InputValue" -> TypeKind.OBJECT
            "__Schema" -> TypeKind.OBJECT
            "__Type" -> TypeKind.OBJECT
            "__TypeKind" -> TypeKind.ENUM

            else -> throw IllegalArgumentException("Unsupported type $typeName")
        }
    }

    abstract fun getInterfaces(typeName: String): List<String>?
    abstract fun getPossibleTypes(typeName: String): List<String>?
    abstract fun getInputFields(typeName: String): List<InputValueIntroData>?
    abstract fun getTypeDescription(typeName: String): String?

    open fun getDirectives(): List<DirectiveIntroData> {
        return listOf(
            DirectiveIntroData(
                "include",
                listOf(GqlIntroDirectiveLocation.FIELD, GqlIntroDirectiveLocation.FRAGMENT_SPREAD, GqlIntroDirectiveLocation.INLINE_FRAGMENT),
                "Directs the executor to include this field or fragment only when the `if` argument is true.",
                listOf(
                    InputValueIntroData(
                        "if",
                        "Boolean!",
                        "Included when true."
                    )
                )
            ),
            DirectiveIntroData(
                "skip",
                listOf(GqlIntroDirectiveLocation.FIELD, GqlIntroDirectiveLocation.FRAGMENT_SPREAD, GqlIntroDirectiveLocation.INLINE_FRAGMENT),
                "Directs the executor to skip this field or fragment when the `if` argument is true.",
                listOf(
                    InputValueIntroData(
                        "if",
                        "Boolean!",
                        "Skipped when true.",
                    )
                )
            ),
        )
    }
}

private val jsonTypeKindsType =     object : TypeReference<LinkedHashMap<String, TypeKind>>() { }
private val jsonEnumValuesType =    object : TypeReference<LinkedHashMap<String, List<EnumValueIntroData>>>() { }
private val jsonFieldsType =        object : TypeReference<LinkedHashMap<String, List<FieldIntroData>>>() { }
private val jsonInputFieldsType =   object : TypeReference<LinkedHashMap<String, List<InputValueIntroData>>>() { }
private val jsonInterfacesType =    object : TypeReference<LinkedHashMap<String, List<String>>>() { }
private val jsonPossibleTypesType = object : TypeReference<LinkedHashMap<String, List<String>>>() { }
private val jsonDescriptionsType =  object : TypeReference<LinkedHashMap<String, String>>() { }


abstract class JsonIntroData(val packageName: String, val schemaName: String) : BaseIntroData() {
    private fun getInputStream(filenameBase: String): InputStream {
        val fileName = "introData_" + schemaName + "_" + filenameBase + ".json"
        return this::class.java.classLoader.getResourceAsStream(packageName.replace(".", "/") + "/" + fileName) ?: throw IllegalStateException("Couldn't read JSON $filenameBase")
    }

    private fun <T: Any> readJson(filenameBase: String, type: TypeReference<T>): T {
        return getInputStream(filenameBase).use { s ->
            INTRO_JSON_MAPPER.readValue(s, type)
        }
    }

    private val typeKinds by lazy { readJson("typeKinds", jsonTypeKindsType) }
    private val interfaces by lazy { readJson("interfaces", jsonInterfacesType) }
    private val possibleTypes by lazy { readJson("possibleTypes", jsonPossibleTypesType) }
    private val descriptions by lazy { readJson("descriptions", jsonDescriptionsType) }
    private val inputFields by lazy { readJson("inputFields", jsonInputFieldsType) }
    private val fields by lazy { readJson("fields", jsonFieldsType) }
    private val enumValues by lazy { readJson("enumValues", jsonEnumValuesType) }

    override fun getSpecificTypeNames(out: ArrayList<String>) {
        out.addAll(typeKinds.keys)
    }

    override fun getInterfaces(typeName: String): List<String>? {
        return interfaces[typeName]
    }

    override fun getPossibleTypes(typeName: String): List<String>? {
        return possibleTypes[typeName]
    }

    override fun getTypeDescription(typeName: String): String? {
        return descriptions[typeName]
    }

    override fun getInputFields(typeName: String): List<InputValueIntroData>? {
        return inputFields[typeName]
    }

    override fun getFields(typeName: String, includeDeprecated: Boolean): List<FieldIntroData>? {
        return fields[typeName]?.let { when (includeDeprecated) {
            true -> it
            else -> it.filter { !it.isDeprecated }
        } } ?: super.getFields(typeName, includeDeprecated)
    }

    override fun getEnumValues(typeName: String, includeDeprecated: Boolean): List<EnumValueIntroData>? {
        return enumValues[typeName]?.let { when (includeDeprecated) {
            true -> it
            else -> it.filter { !it.isDeprecated }
        } } ?: super.getEnumValues(typeName, includeDeprecated)
    }

    override fun getTypeKind(typeName: String): TypeKind {
        return typeKinds[typeName] ?: super.getTypeKind(typeName)
    }
}