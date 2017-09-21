package com.xs0.gqlktx.exec

import com.xs0.gqlktx.*
import com.xs0.gqlktx.dom.*
import com.xs0.gqlktx.parser.GraphQLParser
import com.xs0.gqlktx.parser.Token
import com.xs0.gqlktx.schema.Schema
import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.types.gql.*
import com.xs0.gqlktx.types.kotlin.*
import com.xs0.gqlktx.utils.QueryInput
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import kotlin.reflect.KClass

import java.util.*

import com.xs0.gqlktx.appendLists
import com.xs0.gqlktx.dom.OpType.MUTATION
import com.xs0.gqlktx.dom.OpType.QUERY
import com.xs0.gqlktx.utils.awaitAll
import com.xs0.gqlktx.utils.transformForJson
import kotlinx.coroutines.experimental.*
import mu.KLogging
import kotlin.collections.HashSet
import kotlin.reflect.KCallable
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSuperclassOf

interface QueryExecutor {
    suspend fun <SCHEMA: Any, CTX>
    execute(schema: Schema<SCHEMA, CTX>, rootObject: SCHEMA, context: CTX, queryInput: QueryInput): JsonObject
}

object SimpleQueryExecutor : QueryExecutor {
    override suspend fun <SCHEMA: Any, CTX>
    execute(schema: Schema<SCHEMA, CTX>, rootObject: SCHEMA, context: CTX, queryInput: QueryInput): JsonObject {
        return SimpleQueryState(schema, rootObject, context, queryInput).executeRequest()
    }
}

internal class SimpleQueryState<SCHEMA: Any, CTX>(
        private val schema: Schema<SCHEMA, CTX>,
        private val rootObject: SCHEMA,
        private val context: CTX,
        queryInput: QueryInput) {

    private val rawQuery: String
    private val opName: String?

    private val fragmentsByName = HashMap<String, FragmentDefinition>()
    private val opsByName = HashMap<String?, OperationDefinition>()

    private val errors = JsonArray()
    private val rawVariables: JsonObject
    private lateinit var inputVarParser: InputVarParser<CTX>
    private var query: Document? = null

    init {
        this.rawQuery = queryInput.query
        this.opName = queryInput.opName
        this.rawVariables = queryInput.variables ?: JsonObject()
    }

    suspend fun executeRequest(): JsonObject {
        var data: JsonObject?
        try {
            data = doExecuteRequest()
        } catch (e: Exception) {
            handleException(e)
            data = null
        }

        return sendResponse(data, errors)
    }

    private fun handleException(e: Throwable) {
        if (e is QueryException) {
            addError(e.message ?: return, null, null)
        } else if (e is FieldException) {
            addError(e.message ?: "Unknown error", e.path, null)
        } else if (e is Error) {
            addError("An error occurred: " + e, null, null)
        } else {
            addError("An unexpected exception occurred: " + e, null, null)
        }
    }

    suspend fun doExecuteRequest(): JsonObject? {
        parseQuery()
        makeIndex()
        val op = operation

        val coercedVariableValues = coerceVariableValues(op)

        inputVarParser = InputVarParser(context, coercedVariableValues, schema)

        if (op.type === QUERY) {
            val queryRoot = schema.queryRoot
            val initialObject = queryRoot.invoke(rootObject)!!
            var initialType = schema.getJavaType(queryRoot.type)
            if (initialType is GJavaNotNullType)
                initialType = initialType.innerType
            initialType as GJavaObjectType<CTX>

            return doExecuteQuery(op, coercedVariableValues, initialObject, initialType)
        }

        if (op.type === MUTATION) {
            val mutationRoot = schema.mutationRoot ?: throw QueryException("No mutations supported (yet)")
            val initialObject = mutationRoot.invoke(rootObject)!!
            var initialType = schema.getJavaType(mutationRoot.type)
            if (initialType is GJavaNotNullType)
                initialType = initialType.innerType
            initialType as GJavaObjectType<CTX>

            return doExecuteMutation(op, coercedVariableValues, initialObject, initialType)
        }

        throw QueryException("No subscriptions supported (yet)")
    }

    private suspend fun doExecuteQuery(op: OperationDefinition, variableValues: JsonObject, initialValue: Any, queryType: GJavaObjectType<CTX>): JsonObject? {
        return executeSelectionSet(op.selectionSet, queryType, initialValue, variableValues, FieldPath.root())
    }

    private suspend fun executeSelectionSet(selectionSet: List<Selection>, objectType: GJavaObjectType<CTX>, objectValue: Any, variableValues: JsonObject, parentPath: FieldPath): JsonObject? {
        val groupedFieldSet = collectFields(objectType, selectionSet, variableValues, null)

        val futures = ArrayList<Deferred<Pair<String?, Any?>>>()

        for ((responseKey, fields) in groupedFieldSet) {
            futures.add(async(Unconfined) {
                val fieldName = fields[0].fieldName
                val innerPath = parentPath.subField(fieldName)
                val theValue: Any
                val fieldMethod: FieldGetter<CTX>?
                val fieldType: GJavaType<CTX>?

                if (fieldName.startsWith("__")) {
                    if ("__typename" == fieldName) {
                        theValue = objectType.gqlType.name
                        fieldMethod = schema.INTRO_TYPENAME
                    } else if ("__schema" == fieldName && parentPath.isRoot) {
                        theValue = schema.introspector()
                        fieldMethod = schema.INTRO_SCHEMA
                    } else if ("__type" == fieldName && parentPath.isRoot) {
                        theValue = schema.introspector()
                        fieldMethod = schema.INTRO_TYPE
                    } else {
                        return@async Pair(null, null)
                    }
                } else {
                    theValue = objectValue
                    fieldMethod = objectType.fields[fieldName]
                }

                if (fieldMethod == null)
                    return@async Pair(null, null)

                try {
                    fieldType = schema.getJavaType(fieldMethod.publicType.sourceType)
                } catch (e: Throwable) {
                    e.printStackTrace()
                    throw e
                }

                var res: Any?
                try {
                    res = executeField(theValue, fields, fieldType, fieldMethod, variableValues, innerPath)
                } catch (e: Exception) {
                    logger.error("executeField failed", e)
                    res = null
                    handleException(e)
                }

                if (res == null && !fieldType.isNullAllowed())
                    throw FieldException("Couldn't follow schema due to child error", parentPath)

                Pair(responseKey, res)
            })
        }

        try {
            val results = awaitAll(futures)
            val json = JsonObject()
            for (p : Pair<String?, Any?>? in results) {
                if (p == null)
                    continue

                val key = p.first ?: continue
                val value = p.second
                if (value == null) {
                    json.putNull(key)
                } else {
                    json.put(key, transformForJson(value))
                }
            }
            return json
        } catch (e: Throwable) {
            handleException(e)
            return null
        }
    }

    private suspend fun executeSelectionSetSerially(selectionSet: List<Selection>, objectType: GJavaObjectType<CTX>, objectValue: Any, variableValues: JsonObject): JsonObject? {
        val fieldPath = FieldPath.root()

        val groupedFieldSet = collectFields(objectType, selectionSet, variableValues, null)
        val output = JsonObject()

        for ((responseKey, fields) in groupedFieldSet.entries) {
            try {
                val fieldName = fields[0].fieldName
                val fieldMethod = objectType.fields[fieldName] ?: continue
                val fieldType = schema.getJavaType(fieldMethod.publicType.sourceType)

                logger.trace("Field execution for {} starting", responseKey)

                var subRes: Any? = null
                try {
                    subRes = executeField(objectValue, fields, fieldType, fieldMethod, variableValues, fieldPath.subField(responseKey))
                } catch (e: Exception) {
                    handleException(e)
                }

                if (subRes != null) {
                    output.map.put(responseKey, transformForJson(subRes))
                } else
                if (fieldType.isNullAllowed()) {
                    output.map.put(responseKey, null)
                } else {
                    handleException(FieldException("Field $fieldName is supposed to be not null, which it can't be due to an error, so we're bailing", fieldPath))
                    return null
                }
            } catch (e: Exception) {
                if (e is FieldException) {
                    handleException(e)
                } else {
                    handleException(FieldException(e.message ?: "Unknown error", fieldPath.subField(responseKey), e))
                }
            }
        }

        return output
    }

    private fun extractMessage(e: Throwable): String? {
        if (e.message != null)
            return e.message!!
        if (e.cause != null && e.cause !== e)
            return extractMessage(e.cause!!)
        return null
    }

    private suspend fun executeField(objectValue: Any, fields: List<SelectionField>, fieldType: GJavaType<CTX>, fieldMethod: FieldGetter<CTX>, variableValues: JsonObject, fieldPath: FieldPath): Any? {
        try {
            val field = fields[0]
            val argumentValues = coerceArgumentValues(field, variableValues, fieldMethod, fieldPath)
            val resolvedValue = fieldMethod.invoke(objectValue, context, argumentValues)
            return completeValue(fieldType, fieldType.gqlType, fields, resolvedValue, variableValues, fieldPath)
        } catch (e: Throwable) {
            var err = extractMessage(e)
            if (err == null)
                err = "Unknown error " + e::class.simpleName

            throw FieldException(err, fieldPath, e)
        }
    }

    private suspend fun completeValue(fieldType: GJavaType<CTX>, gqlType: GType, fields: List<SelectionField>, result: Any?, variableValues: JsonObject, fieldPath: FieldPath): Any? {
        var fieldType = fieldType
        var gqlType = gqlType
        val kind = gqlType.kind

        if (kind == TypeKind.NON_NULL) {
            if (fieldType is GJavaNotNullType<*>)
                fieldType = (fieldType as GJavaNotNullType<CTX>).innerType

            gqlType = (gqlType as GNotNullType).wrappedType

            val subRes = completeValue(fieldType, gqlType, fields, result, variableValues, fieldPath)
            if (subRes != null)
                return subRes

            throw IllegalStateException("null received on a non-null field")
        } else if (result == null) {
            return null
        } else if (kind == TypeKind.LIST) {
            val listType = fieldType as GJavaListLikeType<CTX>
            val innerType = listType.elementType

            val iterator: Iterator<*>
            try {
                iterator = listType.getIterator(result)
            } catch (e: Throwable) {
                logger.error("Failed to create iterator for $innerType", e)
                throw IllegalStateException("Failed to create iterator", e)
            }

            val futures = ArrayList<Deferred<Any?>>()
            var index = 0
            while (iterator.hasNext()) {
                val el = iterator.next()
                val idx = index++
                val elPath = fieldPath.listElement(idx)

                futures.add(async(Unconfined) {
                    completeValue(innerType, innerType.gqlType, fields, el, variableValues, elPath)
                })
            }

            val results = awaitAll(futures)

            return JsonArray(results.map { transformForJson(it) })
        } else if (kind == TypeKind.SCALAR || kind == TypeKind.ENUM) {
            fieldType as GJavaScalarLikeType<CTX>
            return fieldType.toJson(result)
        } else {
            val objectType: GJavaObjectType<CTX>
            if (kind == TypeKind.UNION || kind == TypeKind.INTERFACE) {
                objectType = resolveAbstractType(fieldType as GJavaImplementableType<CTX>, result)
            } else {
                objectType = fieldType as GJavaObjectType<CTX>
            }

            val subSelectionSet = mergeSelectionSets(fields)

            return executeSelectionSet(subSelectionSet, objectType, result, variableValues, fieldPath)
        }
    }

    private fun mergeSelectionSets(fields: List<SelectionField>): List<Selection> {
        val selections = ArrayList<Selection>()

        for (field in fields)
            selections.addAll(field.subSelections)

        return selections
    }

    private fun resolveAbstractType(fieldType: GJavaImplementableType<CTX>, result: Any): GJavaObjectType<CTX> {
        val resultClass = result::class

        for (impl in fieldType.implementations) {
            if (impl.isSuperclassOf(resultClass)) {
                return schema.getJavaType(impl.createType(nullable=true)) as GJavaObjectType<CTX>
            }
        }
        throw IllegalStateException("result of $resultClass did not resolve to any known implementation")
    }

    private fun coerceArgumentValues(field: SelectionField, variableValues: JsonObject, fieldMethod: FieldGetter<CTX>, fieldPath: FieldPath): Map<String, Any?> {
        val coercedValues = LinkedHashMap<String, Any?>()
        val argumentValues = field.arguments

        for ((argumentName, paramInfo) in fieldMethod.publicParams) {
            val argumentType = schema.getJavaType(paramInfo.type.sourceType)
            val hasExplicitValue = argumentValues.containsKey(argumentName)
            val valueOrVar = argumentValues[argumentName]

            if (hasExplicitValue && valueOrVar is Variable) {
                val `var` = valueOrVar as Variable?
                val varName = `var`!!.name
                if (variableValues.containsKey(varName)) {
                    val varValue = variableValues.getValue(varName)
                    val javaType = schema.getJavaType(paramInfo.type.sourceType)
                    val value: Any?

                    if (varValue == null) {
                        if (javaType.isNullAllowed()) {
                            value = null
                        } else {
                            throw FieldException("Forbidden null value", fieldPath)
                        }
                    } else {
                        try {
                            value = javaType.getFromJson(varValue, inputVarParser)
                        } catch (e: Exception) {
                            throw FieldException("Failed to parse field: " + e.message, fieldPath, e)
                        }

                    }
                    coercedValues.put(argumentName, value)
                    continue
                }
            }

            if (!hasExplicitValue || valueOrVar is Variable) {
                if (paramInfo.parsedDefault != null) {
                    val `val`: Any
                    try {
                        `val` = argumentType.getFromJson(paramInfo.parsedDefault, inputVarParser)
                    } catch (e: Exception) {
                        throw FieldException("Couldn't use default value for argument " + argumentName + ": " + e.message, fieldPath, e)
                    }

                    coercedValues.put(argumentName, `val`)
                    continue
                } else if (argumentType is GJavaNotNullType<*>) {
                    throw FieldException("Missing value for not null argument " + argumentName, fieldPath)
                }
            }

            if (valueOrVar == null || valueOrVar is ValueNull) {
                if (argumentType is GJavaNotNullType<*>) {
                    throw FieldException("Argument $argumentName is not null, but null was provided as the value", fieldPath)
                } else {
                    coercedValues.put(argumentName, null)
                    continue
                }
            }

            // TODO: improve this conversion from (parsed) Value to (JavaType'd) value
            val jsonValue = (valueOrVar as Value).toJson()
            val valueType = schema.getJavaType(paramInfo.type.sourceType)

            val value: Any?
            if (jsonValue == null) {
                value = null
            } else {
                try {
                    value = valueType.getFromJson(jsonValue, inputVarParser)
                } catch (e: Exception) {
                    throw FieldException("Failed to parse value: " + e.message, fieldPath, e)
                }
            }
            if (value == null) {
                if (!valueType.isNullAllowed()) {
                    throw FieldException("Null value", fieldPath)
                }
            }
            coercedValues.put(argumentName, value)
        }

        return coercedValues
    }

    private suspend fun doExecuteMutation(op: OperationDefinition, variableValues: JsonObject, initialValue: Any, queryType: GJavaObjectType<CTX>): JsonObject? {
        return executeSelectionSetSerially(op.selectionSet, queryType, initialValue, variableValues)
    }

    fun collectFields(objectType: GJavaObjectType<CTX>, selectionSet: List<Selection>, variableValues: JsonObject, visitedFragments: HashSet<String>?): MutableMap<String, MutableList<SelectionField>> {
        var visitedFragments = visitedFragments ?: HashSet()

        val groupedFields = LinkedHashMap<String, MutableList<SelectionField>>()

        for (selection in selectionSet) {
            if (shouldSkip(selection, variableValues))
                continue

            if (selection is SelectionField) {
                val responseKey = selection.responseKey
                groupedFields.computeIfAbsent(responseKey) { _ -> ArrayList() }.add(selection)
            } else if (selection is SelectionFragmentSpread) {
                val fragmentSpreadName = selection.getFragmentName()
                if (visitedFragments.contains(fragmentSpreadName))
                    continue
                visitedFragments.add(fragmentSpreadName)

                val fragmentDef = fragmentsByName[fragmentSpreadName] ?: continue

                val fragmentTypeName = fragmentDef.type.name
                val fragmentType = schema.getGQLBaseType(fragmentTypeName)

                if (!doesFragmentTypeApply(objectType.gqlType, fragmentType))
                    continue

                val fragmentSelectionSet = fragmentDef.selectionSet
                val fragmentGroupedFieldSet = collectFields(objectType, fragmentSelectionSet, variableValues, visitedFragments)
                appendLists(groupedFields, fragmentGroupedFieldSet)
            } else if (selection is SelectionInlineFragment) {
                if (selection.typeConditionOpt != null) {
                    val fragmentTypeName = selection.typeConditionOpt.name
                    val fragmentType = schema.getGQLBaseType(fragmentTypeName)

                    if (!doesFragmentTypeApply(objectType.gqlType, fragmentType))
                        continue
                }
                val fragmentSelectionSet = selection.selectionSet
                val fragmentGroupedFieldSet = collectFields(objectType, fragmentSelectionSet, variableValues, visitedFragments)
                appendLists(groupedFields, fragmentGroupedFieldSet)
            }
        }

        return groupedFields
    }

    private fun doesFragmentTypeApply(objectType: GObjectType, fragmentType: GBaseType): Boolean {
        return when (fragmentType.kind) {
            TypeKind.OBJECT    -> objectType === fragmentType
            TypeKind.INTERFACE -> (fragmentType as GInterfaceType).implementations.contains(objectType)
            TypeKind.UNION     -> (fragmentType as GUnionType).members.contains(objectType)

            else -> throw IllegalStateException("Unexpected kind of type of a fragment: " + fragmentType.kind)
        }
    }

    private fun shouldSkip(selection: Selection, variableValues: JsonObject): Boolean {
        val skipDir = selection.findDirective("skip")
        if (skipDir != null) {
            val skip = extractBooleanValue(skipDir, "if", variableValues)
            if (skip != null && skip)
                return true
        }

        val includeDir = selection.findDirective("include")
        if (includeDir != null) {
            val include = extractBooleanValue(includeDir, "if", variableValues)
            if (include == null || !include)
                return true
        }

        return false
    }

    private fun extractBooleanValue(directive: Directive, arg: String, variableValues: JsonObject): Boolean? {
        val valueOrVar = directive.args[arg] ?: return null

        if (valueOrVar is Variable)
            return variableValues.getBoolean(valueOrVar.name)

        return (valueOrVar as? ValueBool)?.value
    }

    private fun coerceVariableValues(op: OperationDefinition): JsonObject {
        val coercedValues = JsonObject()

        for ((varName, value) in op.varDefs) {
            val varTypeDef = value.type
            val defaultValue = value.defaultValue

            if (rawVariables.containsKey(varName)) {
                val type = getType(varTypeDef)
                type.coerceValue(rawVariables, varName, coercedValues)
            } else if (defaultValue != null) {
                coercedValues.put(varName, defaultValue.toJson())
            } else {
                if (varTypeDef is NotNullType)
                    throw QueryException("Missing data for variable $$varName")
            }
        }

        return coercedValues
    }

    @Throws(QueryException::class)
    private fun getType(typeDef: TypeDef): GType {
        if (typeDef is NamedType) {
            val typeName = typeDef.name
            return schema.getGQLBaseType(typeName)
        } else return if (typeDef is NotNullType) {
            getType(typeDef.inner).notNull()
        } else if (typeDef is ListType) {
            getType(typeDef.inner).listOf()
        } else {
            throw Error("Unknown TypeDef type " + typeDef.javaClass)
        }
    }

    val operation: OperationDefinition
        @Throws(QueryException::class)
        get() = if (opName == null) {
            if (opsByName.size == 1) {
                opsByName.values.iterator().next()
            } else {
                throw QueryException("Missing operationName")
            }
        } else opsByName.get<String?, OperationDefinition>(opName) ?: throw QueryException("Operation ${opName} not found")

    private fun addError(message: String, fieldPath: FieldPath?, location: Token<*>?) {
        val error = JsonObject()
        error.put("message", message)
        if (fieldPath != null)
            error.put("path", fieldPath.toArray())
        if (location != null) {
            val loc = JsonObject()
            loc.put("line", location.row)
            loc.put("column", location.column)
            error.put("locations", JsonArray().add(loc))
        }

        errors.add(error)
    }

    private fun sendResponse(data: JsonObject?, errors: JsonArray?): JsonObject {
        val result = JsonObject()

        if (data != null && !data.isEmpty)
            result.put("data", data)

        if (errors != null && !errors.isEmpty)
            result.put("errors", errors)

        return result
    }


    private fun makeIndex() {
        // note we assume a valid query here, given the schema..
        // (so we don't check for duplicate names, etc. etc.)
        for (def in query!!.definitions) {
            if (def is OperationDefinition) {
                opsByName.put(def.name, def)
            } else if (def is FragmentDefinition) {
                fragmentsByName.put(def.name, def)
            }
        }
    }

    @Throws(ParseException::class)
    private fun parseQuery() {
        this.query = GraphQLParser.parseQueryDoc(rawQuery)
    }

    companion object : KLogging()
}


fun <T: Any> KClass<T>.findMethod(name: String): KCallable<*> {
    for (member in this.members) {
        if (member.name == name)
            return member
    }
    throw IllegalArgumentException("Couldn't find $name in $this")
}