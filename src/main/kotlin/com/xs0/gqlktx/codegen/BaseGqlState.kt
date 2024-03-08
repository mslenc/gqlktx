package com.xs0.gqlktx.codegen

import com.xs0.gqlktx.FieldException
import com.xs0.gqlktx.QueryException
import com.xs0.gqlktx.ScalarCoercion
import com.xs0.gqlktx.dom.*
import com.xs0.gqlktx.exec.FieldPath
import com.xs0.gqlktx.parser.GraphQLParser
import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.utils.QueryInput
import java.util.HashMap
import kotlinx.coroutines.CancellationException
import java.util.LinkedHashMap

abstract class BaseGqlState(queryInput: QueryInput) {
    protected val errors = ArrayList<Any?>()
    val fragmentsByName = HashMap<String, FragmentDefinition>()
    private val opsByName = HashMap<String?, OperationDefinition>()
    private val rawQuery: String = queryInput.query
    private val opName: String? = queryInput.opName
    private val rawVariables: Map<String, ValueOrNull> = queryInput.variables ?: emptyMap()
    private val mutationsAllowed = queryInput.allowMutations
    private lateinit var query: Document
    lateinit var variables: Map<String, ValueOrNull>
    abstract val introData: BaseIntroData
    abstract val coercion: ScalarCoercion

    protected abstract suspend fun doExecuteQuery(selectionSet: List<Selection>): Map<String, Any?>?
    protected abstract suspend fun doExecuteMutation(selectionSet: List<Selection>): Map<String, Any?>?

    private fun addError(message: String, fieldPath: FieldPath?) {
        val error = mutableMapOf<String, Any?>("message" to message)
        if (fieldPath != null)
            error["path"] = fieldPath.toArray()
        errors.add(error)
    }

    fun handleException(e: Throwable, path: FieldPath? = null) {
        when (e) {
            is QueryException -> addError(e.message ?: return, path)
            is FieldException -> addError(e.message ?: "Unknown error", e.path)
            is Error -> addError("An error occurred: $e", path)
            is CancellationException -> {
                // ignore
            }
            else -> addError("An unexpected exception occurred: $e", path)
        }
    }

    fun mergeSelectionSets(fields: List<SelectionField>): List<Selection> {
        return fields.flatMap { it.subSelections }
    }

    suspend fun executeRequest(): Map<String, Any?> {
        var data: Map<String, Any?>?
        try {
            data = doExecuteRequest()
        } catch (e: Exception) {
            handleException(e)
            data = null
        }

        return createResponse(data, errors)
    }

    private fun createResponse(data: Map<String, Any?>?, errors: List<Any?>?): Map<String, Any?> {
        val result = LinkedHashMap<String, Any?>()

        if (data != null && data.isNotEmpty())
            result["data"] = data

        if (errors != null && errors.isNotEmpty())
            result["errors"] = errors

        return result
    }

    private fun makeIndex() {
        for (def in query.definitions) {
            when (def) {
                is OperationDefinition -> opsByName[def.name] = def
                is FragmentDefinition -> fragmentsByName[def.name] = def
            }
        }
    }

    fun getOperation(): OperationDefinition {
        return when {
            opName != null -> opsByName.get<String?, OperationDefinition>(opName) ?: throw QueryException("Operation $opName not found")
            else -> opsByName.values.singleOrNull() ?: throw QueryException("Missing operationName")
        }
    }

    private fun coerceVariableValues(op: OperationDefinition): Map<String, ValueOrNull> {
        val coercedValues = LinkedHashMap<String, ValueOrNull>()

        for ((varName, value) in op.varDefs) {
            val varTypeDef = value.type
            val defaultValue = value.defaultValue

            when {
                rawVariables.containsKey(varName) -> {
                    coercedValues[varName] = rawVariables.getValue(varName)
                }
                defaultValue != null -> {
                    coercedValues[varName] = defaultValue
                }
                else -> {
                    if (varTypeDef is NotNullType)
                        throw QueryException("Missing data for variable $$varName")
                }
            }
        }

        return coercedValues
    }

    suspend fun doExecuteRequest(): Map<String, Any?>? {
        this.query = GraphQLParser.parseQueryDoc(rawQuery)
        makeIndex()
        val op = getOperation()
        variables = coerceVariableValues(op)

        if (op.type === OpType.QUERY) {
            return doExecuteQuery(op.selectionSet)
        }

        if (op.type === OpType.MUTATION) {
            if (!mutationsAllowed)
                throw QueryException("Mutations not allowed")

            return doExecuteMutation(op.selectionSet)
        }

        throw QueryException("No subscriptions supported (yet)")
    }

    fun shouldSkip(selection: Selection): Boolean {
        val skipDir = selection.findDirective("skip")
        if (skipDir != null) {
            val skip = skipDir.args["if"]?.let { BaselineInputParser.parseBoolean(it, variables) }
            if (skip != null && skip)
                return true
        }

        val includeDir = selection.findDirective("include")
        if (includeDir != null) {
            val include = includeDir.args["if"]?.let { BaselineInputParser.parseBoolean(it, variables) }
            if (include == null || !include)
                return true
        }

        return false
    }

    private fun incDeprecated(selection: SelectionField): Boolean {
        return selection.arguments["includeDeprecated"]?.let { BaselineInputParser.parseBooleanNotNull(it, variables) } == true
    }

    private fun collectFields(typeName: String, selectionSet: List<Selection>, visitedFragments: MutableSet<String>, out: LinkedHashMap<String, ArrayList<SelectionField>>): Map<String, List<SelectionField>> {
        for (selection in selectionSet) {
            if (shouldSkip(selection))
                continue

            when (selection) {
                is SelectionField -> {
                    out.getOrPut(selection.responseKey) { ArrayList() }.add(selection)
                }
                is SelectionFragmentSpread -> {
                    val fragmentSpreadName = selection.getFragmentName()
                    if (visitedFragments.contains(fragmentSpreadName))
                        continue
                    visitedFragments.add(fragmentSpreadName)
                    val fragmentDef = fragmentsByName[fragmentSpreadName] ?: continue
                    if (fragmentDef.type.name == typeName) {
                        collectFields(typeName, fragmentDef.selectionSet, visitedFragments, out)
                    }
                }
                is SelectionInlineFragment -> {
                    val typeOpt = selection.typeConditionOpt
                    if (typeOpt == null || typeOpt.name == typeName) {
                        collectFields(typeName, selection.selectionSet, visitedFragments, out)
                    }
                }
            }
        }
        return out
    }

    fun handleSchemaQuery(selection: List<Selection>): Map<String, Any?> {
        val groupedFieldSet = collectFields("__Schema", selection, HashSet(), LinkedHashMap())
        val jsonResult = LinkedHashMap<String, Any?>()

        for ((responseKey, fields) in groupedFieldSet) {
            when (fields[0].fieldName) {
                "__typename" -> {
                    jsonResult[responseKey] = BaselineExporter.exportStringNotNull("__Schema", coercion)
                }
                "directives" -> {
                    jsonResult[responseKey] = introData.getDirectives().let { handleDirectivesQuery(it, mergeSelectionSets(fields)) }
                }
                "mutationType" -> {
                    jsonResult[responseKey] = introData.getMutationTypeName()?.let { handleTypeQuery(it, mergeSelectionSets(fields)) }
                }
                "queryType" -> {
                    jsonResult[responseKey] = introData.getQueryTypeName().let { handleTypeQuery(it, mergeSelectionSets(fields)) }
                }
                "subscriptionType" -> {
                    jsonResult[responseKey] = introData.getSubscriptionTypeName()?.let { handleTypeQuery(it, mergeSelectionSets(fields)) }
                }
                "types" -> {
                    jsonResult[responseKey] = handleTypesListQuery(introData.allTypeNames, mergeSelectionSets(fields))
                }
            }
        }

        return jsonResult
    }

    fun handleTypeByNameQuery(typeName: String, selection: List<Selection>): Map<String, Any?>? {
        try {
            introData.getTypeKind(typeName)
        } catch (e: IllegalArgumentException) {
            return null
        }

        return handleTypeQuery(typeName, selection)
    }

    private fun handleTypesListQuery(typeNames: List<String>, selection: List<Selection>): List<Map<String, Any?>> {
        return typeNames.map { handleTypeQuery(it, selection) }
    }

    private fun handleTypeQuery(typeName: String, selection: List<Selection>): Map<String, Any?> {
        val groupedFieldSet = collectFields("__Type", selection, HashSet(), LinkedHashMap())
        val jsonResult = LinkedHashMap<String, Any?>()

        val isNotNull = typeName.endsWith("!")
        val isList = !isNotNull && typeName.startsWith("[")
        val isType = !isNotNull && !isList

        val subTypeName = when {
            isNotNull -> typeName.substring(0, typeName.length - 1)
            isList -> typeName.substring(1, typeName.length - 1)
            else -> typeName
        }

        val kind = when {
            isList -> TypeKind.LIST
            isNotNull -> TypeKind.NON_NULL
            else -> introData.getTypeKind(typeName)
        }

        for ((responseKey, fields) in groupedFieldSet) {
            val activeField = fields[0]
            val fieldName = activeField.fieldName

            when (fieldName) {
                "__typename" -> {
                    jsonResult[responseKey] = BaselineExporter.exportStringNotNull("__Type", coercion)
                }

                "name" -> {
                    jsonResult[responseKey] = when {
                        isType -> BaselineExporter.exportStringNotNull(typeName, coercion)
                        else -> null
                    }
                }

                "description" -> {
                    jsonResult[responseKey] = when {
                        isType -> BaselineExporter.exportString(introData.getTypeDescription(typeName), coercion)
                        else -> null
                    }
                }

                "fields" -> {
                    jsonResult[responseKey] = when (kind) {
                        TypeKind.OBJECT,
                        TypeKind.INTERFACE -> introData.getFields(typeName, incDeprecated(activeField))?.let { handleFieldsQuery(it, mergeSelectionSets(fields)) } ?: emptyList()
                        else -> null
                    }
                }

                "inputFields" -> {
                    jsonResult[responseKey] = when (kind) {
                        TypeKind.INPUT_OBJECT -> introData.getInputFields(typeName)?.let { handleInputValuesQuery(it, mergeSelectionSets(fields)) } ?: emptyList()
                        else -> null
                    }
                }

                "interfaces" -> {
                    jsonResult[responseKey] = when (kind) {
                        TypeKind.OBJECT,
                        TypeKind.INTERFACE -> introData.getInterfaces(typeName)?.let { handleTypesListQuery(it, mergeSelectionSets(fields)) } ?: emptyList()
                        else -> null
                    }
                }

                "kind" -> {
                    jsonResult[responseKey] = when (coercion) {
                        ScalarCoercion.NONE -> kind
                        else -> kind.name
                    }
                }

                "ofType" -> {
                    jsonResult[responseKey] = when {
                        isList || isNotNull -> handleTypeQuery(subTypeName, mergeSelectionSets(fields))
                        else -> null
                    }
                }

                "possibleTypes" -> {
                    jsonResult[responseKey] = when (kind) {
                        TypeKind.INTERFACE,
                        TypeKind.UNION -> introData.getPossibleTypes(typeName)?.let { handleTypesListQuery(it, mergeSelectionSets(fields)) } ?: emptyList()
                        else -> null
                    }
                }

                "enumValues" -> {
                    jsonResult[responseKey] = when (kind) {
                        TypeKind.ENUM -> introData.getEnumValues(typeName, incDeprecated(activeField))?.let { handleEnumValuesQuery(it, mergeSelectionSets(fields)) } ?: emptyList()
                        else -> null
                    }
                }

                "specifiedByURL" -> {
                    jsonResult[responseKey] = null
                }
            }
        }

        return jsonResult
    }

    private fun handleInputValuesQuery(items: List<InputValueIntroData>, selection: List<Selection>): List<Map<String, Any?>> {
        return items.map { handleInputValueQuery(it, selection) }
    }

    private fun handleInputValueQuery(item: InputValueIntroData, selection: List<Selection>): Map<String, Any?> {
        val groupedFieldSet = collectFields("__InputValue", selection, HashSet(), LinkedHashMap())
        val jsonResult = LinkedHashMap<String, Any?>()

        for ((responseKey, fields) in groupedFieldSet) {
            when (fields[0].fieldName) {
                "__typename" -> {
                    jsonResult[responseKey] = BaselineExporter.exportStringNotNull("__InputValue", coercion)
                }

                "name" -> {
                    jsonResult[responseKey] = BaselineExporter.exportStringNotNull(item.name, coercion)
                }

                "description" -> {
                    jsonResult[responseKey] = BaselineExporter.exportString(item.description, coercion)
                }

                "type" -> {
                    jsonResult[responseKey] = handleTypeQuery(item.typeName, mergeSelectionSets(fields))
                }

                "defaultValue" -> {
                    jsonResult[responseKey] = BaselineExporter.exportString(item.defaultValue, coercion)
                }
            }
        }

        return jsonResult
    }

    private fun handleFieldsQuery(items: List<FieldIntroData>, selection: List<Selection>): List<Map<String, Any?>> {
        return items.map { handleFieldQuery(it, selection) }
    }

    private fun handleFieldQuery(item: FieldIntroData, selection: List<Selection>): Map<String, Any?> {
        val groupedFieldSet = collectFields("__Field", selection, HashSet(), LinkedHashMap())
        val jsonResult = LinkedHashMap<String, Any?>()

        for ((responseKey, fields) in groupedFieldSet) {
            when (fields[0].fieldName) {
                "__typename" -> {
                    jsonResult[responseKey] = BaselineExporter.exportStringNotNull("__Field", coercion)
                }

                "name" -> {
                    jsonResult[responseKey] = BaselineExporter.exportStringNotNull(item.name, coercion)
                }

                "description" -> {
                    jsonResult[responseKey] = BaselineExporter.exportString(item.description, coercion)
                }

                "type" -> {
                    jsonResult[responseKey] = handleTypeQuery(item.typeName, mergeSelectionSets(fields))
                }

                "isDeprecated" -> {
                    jsonResult[responseKey] = BaselineExporter.exportBooleanNotNull(item.isDeprecated, coercion)
                }

                "deprecationReason" -> {
                    jsonResult[responseKey] = BaselineExporter.exportString(item.deprecationReason, coercion)
                }

                "args" -> {
                    jsonResult[responseKey] = handleInputValuesQuery(item.args, mergeSelectionSets(fields))
                }
            }
        }

        return jsonResult
    }

    private fun handleEnumValuesQuery(items: List<EnumValueIntroData>, selection: List<Selection>): List<Map<String, Any?>> {
        return items.map { handleEnumValueQuery(it, selection) }
    }

    private fun handleEnumValueQuery(item: EnumValueIntroData, selection: List<Selection>): Map<String, Any?> {
        val groupedFieldSet = collectFields("__EnumValue", selection, HashSet(), LinkedHashMap())
        val jsonResult = LinkedHashMap<String, Any?>()

        for ((responseKey, fields) in groupedFieldSet) {
            when (fields[0].fieldName) {
                "__typename" -> {
                    jsonResult[responseKey] = BaselineExporter.exportStringNotNull("__EnumValue", coercion)
                }

                "name" -> {
                    jsonResult[responseKey] = BaselineExporter.exportStringNotNull(item.name, coercion)
                }

                "description" -> {
                    jsonResult[responseKey] = BaselineExporter.exportString(item.description, coercion)
                }

                "isDeprecated" -> {
                    jsonResult[responseKey] = BaselineExporter.exportBooleanNotNull(item.isDeprecated, coercion)
                }

                "deprecationReason" -> {
                    jsonResult[responseKey] = BaselineExporter.exportString(item.deprecationReason, coercion)
                }
            }
        }

        return jsonResult
    }

    private fun handleDirectivesQuery(items: List<DirectiveIntroData>, selection: List<Selection>): List<Map<String, Any?>> {
        return items.map { handleDirectiveQuery(it, selection) }
    }

    private fun handleDirectiveQuery(item: DirectiveIntroData, selection: List<Selection>): Map<String, Any?> {
        val groupedFieldSet = collectFields("__Directive", selection, HashSet(), LinkedHashMap())
        val jsonResult = LinkedHashMap<String, Any?>()

        for ((responseKey, fields) in groupedFieldSet) {
            when (fields[0].fieldName) {
                "__typename" -> {
                    jsonResult[responseKey] = BaselineExporter.exportStringNotNull("__Directive", coercion)
                }

                "name" -> {
                    jsonResult[responseKey] = BaselineExporter.exportStringNotNull(item.name, coercion)
                }

                "description" -> {
                    jsonResult[responseKey] = BaselineExporter.exportString(item.description, coercion)
                }

                "locations" -> {
                    jsonResult[responseKey] = item.locations.map { loc -> when (coercion) {
                        ScalarCoercion.NONE -> loc
                        else -> loc.name
                    } }
                }

                "args" -> {
                    jsonResult[responseKey] = handleInputValuesQuery(item.args, mergeSelectionSets(fields))
                }
            }
        }

        return jsonResult
    }
}