package com.xs0.gqlktx.exec

import com.github.mslenc.utils.getLogger
import com.xs0.gqlktx.*
import com.xs0.gqlktx.dom.*
import com.xs0.gqlktx.parser.GraphQLParser
import com.xs0.gqlktx.parser.Token
import com.xs0.gqlktx.schema.Schema
import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.types.gql.*
import com.xs0.gqlktx.types.kotlin.*
import com.xs0.gqlktx.utils.QueryInput
import kotlin.reflect.KClass

import java.util.*

import com.xs0.gqlktx.appendLists
import com.xs0.gqlktx.dom.OpType.MUTATION
import com.xs0.gqlktx.dom.OpType.QUERY
import com.xs0.gqlktx.types.kotlin.lists.GJavaBooleanArrayType
import kotlinx.coroutines.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.reflect.KCallable
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSuperclassOf

interface QueryExecutor {
    suspend fun <SCHEMA: Any, CTX: Any>
    execute(schema: Schema<SCHEMA, CTX>, rootObject: SCHEMA, context: CTX, queryInput: QueryInput, scalarCoercion: ScalarCoercion = ScalarCoercion.JSON): Map<String, Any?>
}

object SimpleQueryExecutor : QueryExecutor {
    private val log = getLogger<SimpleQueryExecutor>()

    override suspend fun <SCHEMA: Any, CTX: Any>
    execute(schema: Schema<SCHEMA, CTX>, rootObject: SCHEMA, context: CTX, queryInput: QueryInput, scalarCoercion: ScalarCoercion): Map<String, Any?> {
        val startedAt = System.currentTimeMillis()
        val result = SimpleQueryState(schema, rootObject, context, scalarCoercion, queryInput).executeRequest()

        if (log.isInfoEnabled) {
            val endedAt = System.currentTimeMillis()
            log.info("Query took ${ endedAt - startedAt } ms")
        }

        return result
    }
}

internal class SimpleQueryState<SCHEMA: Any, CTX: Any>(
        private val schema: Schema<SCHEMA, CTX>,
        private val rootObject: SCHEMA,
        private val context: CTX,
        private val scalarCoercion: ScalarCoercion,
        queryInput: QueryInput) {

    private val rawQuery: String = queryInput.query
    private val opName: String? = queryInput.opName
    private val rawVariables: Map<String, ValueOrNull> = queryInput.variables ?: emptyMap()
    private val mutationsAllowed = queryInput.allowMutations

    private val fragmentsByName = HashMap<String, FragmentDefinition>()
    private val opsByName = HashMap<String?, OperationDefinition>()

    private val errors = ArrayList<Any?>()

    private lateinit var inputVarParser: InputVarParser<CTX>
    private var query: Document? = null

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

    private fun handleException(e: Throwable) {
        when (e) {
            is QueryException -> addError(e.message ?: return, null, null)
            is FieldException -> addError(e.message ?: "Unknown error", e.path, null)
            is Error -> addError("An error occurred: $e", null, null)
            is CancellationException -> {
                // ignore
            }
            else -> addError("An unexpected exception occurred: $e", null, null)
        }
    }

    suspend fun doExecuteRequest(): Map<String, Any?>? {
        parseQuery()
        makeIndex()
        val op = operation

        inputVarParser = InputVarParser(coerceVariableValues(op), schema)

        if (op.type === QUERY) {
            val queryRoot = schema.queryRoot
            val initialObject = queryRoot.invoke(rootObject)!!
            var initialType = schema.getJavaType(queryRoot.type)
            if (initialType is GJavaNotNullType)
                initialType = initialType.innerType
            initialType as GJavaObjectType<CTX>

            return doExecuteQuery(op, initialObject, initialType)
        }

        if (op.type === MUTATION) {
            if (!mutationsAllowed)
                throw QueryException("Mutations not allowed")

            val mutationRoot = schema.mutationRoot ?: throw QueryException("No mutations supported (yet)")
            val initialObject = mutationRoot.invoke(rootObject)!!
            var initialType = schema.getJavaType(mutationRoot.type)
            if (initialType is GJavaNotNullType)
                initialType = initialType.innerType
            initialType as GJavaObjectType<CTX>

            return doExecuteMutation(op, initialObject, initialType)
        }

        throw QueryException("No subscriptions supported (yet)")
    }

    private suspend fun doExecuteQuery(op: OperationDefinition, initialValue: Any, queryType: GJavaObjectType<CTX>): Map<String, Any?>? {
        return executeSelectionSet(op.selectionSet, queryType, initialValue, FieldPath.root())
    }

    private suspend fun doExecuteMutation(op: OperationDefinition, initialValue: Any, queryType: GJavaObjectType<CTX>): Map<String, Any?>? {
        return executeSelectionSet(op.selectionSet, queryType, initialValue, FieldPath.root(), concurrent=false)
    }

    private suspend fun executeSelectionSet(selectionSet: List<Selection>, objectType: GJavaObjectType<CTX>, objectValue: Any, parentPath: FieldPath, concurrent: Boolean = true): Map<String, Any?>? = supervisorScope {
        val groupedFieldSet = collectFields(objectType, selectionSet, HashSet())

        val jsonResult = LinkedHashMap<String, Any?>()

        val futures: ArrayList<Deferred<Pair<String, Any?>?>>?
        if (concurrent) {
            futures = ArrayList()
        } else {
            futures = null
        }

        nextField@
        for ((responseKey, fields) in groupedFieldSet) {
            val fieldName = fields[0].fieldName
            val innerPath = parentPath.subField(fieldName)
            val theValue: Any
            val fieldMethod: FieldGetter<CTX>?
            val fieldType: GJavaType<CTX>?

            if (fieldName.startsWith("__")) {
                when {
                    fieldName == "__typename" -> {
                        theValue = objectType.gqlType.name
                        fieldMethod = schema.INTRO_TYPENAME
                    }

                    fieldName == "__schema" && parentPath.isRoot -> {
                        theValue = schema.introspector()
                        fieldMethod = schema.INTRO_SCHEMA
                    }

                    fieldName == "__type"&& parentPath.isRoot -> {
                        theValue = schema.introspector()
                        fieldMethod = schema.INTRO_TYPE
                    }

                    else -> continue@nextField
                }
            } else {
                theValue = objectValue
                fieldMethod = objectType.fields[fieldName]
            }

            if (fieldMethod == null)
                continue

            try {
                fieldType = schema.getJavaType(fieldMethod.publicType.sourceType)
            } catch (e: Throwable) {
                logger.error("Failed to obtain java type", e)
                throw e
            }

            if (concurrent) {
                futures!!.add(async(start = CoroutineStart.UNDISPATCHED) {
                    var res: Any?
                    try {
                        res = executeField(theValue, fields, fieldType, fieldMethod, innerPath)
                    } catch (e: Exception) {
                        logger.error("executeField failed", e)
                        res = null
                        handleException(e)
                    }

                    if (res == null && !fieldType.isNullAllowed())
                        throw FieldException("Couldn't follow schema due to child error", parentPath)

                    Pair(responseKey, res)
                })
            } else {
                var res: Any?
                try {
                    res = executeField(theValue, fields, fieldType, fieldMethod, innerPath)
                } catch (e: Exception) {
                    logger.error("executeField failed", e)
                    res = null
                    handleException(e)
                }

                if (res == null && !fieldType.isNullAllowed())
                    throw FieldException("Couldn't follow schema due to child error", parentPath)

                jsonResult[responseKey] = res
            }
        }

        if (futures != null) {
            assert(concurrent)
            try {
                val results = futures.awaitAll()
                for ((key, value) in results.filterNotNull()) {
                    jsonResult[key] = value
                }
            } catch (e: Throwable) {
                handleException(e)
                return@supervisorScope null
            }
        }

        jsonResult
    }

    private fun extractMessage(e: Throwable): String? {
        if (e.message != null)
            return e.message!!
        if (e.cause != null && e.cause !== e)
            return extractMessage(e.cause!!)
        return null
    }

    private suspend fun executeField(objectValue: Any, fields: List<SelectionField>, fieldType: GJavaType<CTX>, fieldMethod: FieldGetter<CTX>, fieldPath: FieldPath): Any? {
        try {
            val field = fields[0]
            val argumentValues = coerceArgumentValues(field, fieldMethod, fieldPath)
            val resolvedValue = fieldMethod.invoke(objectValue, context, argumentValues)
            return completeValue(fieldType, fieldType.gqlType, fields, resolvedValue, fieldPath)
        } catch (e: Throwable) {
            var err = extractMessage(e)
            if (err == null)
                err = "Unknown error " + e::class.simpleName

            throw FieldException(err, fieldPath, e)
        }
    }

    private suspend fun completeValue(fieldType: GJavaType<CTX>, gqlType: GType, fields: List<SelectionField>, result: Any?, fieldPath: FieldPath): Any? = supervisorScope {
        var fieldType = fieldType
        var gqlType = gqlType
        val kind = gqlType.kind

        if (kind == TypeKind.NON_NULL) {
            if (fieldType is GJavaNotNullType<*>)
                fieldType = (fieldType as GJavaNotNullType<CTX>).innerType

            gqlType = (gqlType as GNotNullType).wrappedType

            val subRes = completeValue(fieldType, gqlType, fields, result, fieldPath)
            if (subRes != null)
                return@supervisorScope subRes

            throw IllegalStateException("null received on a non-null field (path = $fieldPath)")
        } else if (result == null) {
            return@supervisorScope null
        } else if (kind == TypeKind.LIST) {
            val listType = fieldType as GJavaListLikeType<CTX>
            val innerType = listType.elementType

            val iterator: Iterator<*>
            try {
                iterator = listType.getIterator(result)
            } catch (e: Throwable) {
                logger.error("Failed to create iterator for $innerType", e)
                throw IllegalStateException("Failed to create iterator (path = $fieldPath)", e)
            }

            val futures = ArrayList<Deferred<Any?>>()
            var index = 0
            while (iterator.hasNext()) {
                val el = iterator.next()
                val idx = index++
                val elPath = fieldPath.listElement(idx)

                futures.add(async(start = CoroutineStart.UNDISPATCHED) {
                    completeValue(innerType, innerType.gqlType, fields, el, elPath)
                })
            }

            val results = futures.awaitAll()

            return@supervisorScope results
        } else if (kind == TypeKind.SCALAR || kind == TypeKind.ENUM) {
            fieldType as GJavaScalarLikeType<CTX>
            return@supervisorScope fieldType.toJson(result, scalarCoercion)
        } else {
            val objectType: GJavaObjectType<CTX>
            if (kind == TypeKind.UNION || kind == TypeKind.INTERFACE) {
                objectType = resolveAbstractType(fieldType as GJavaImplementableType<CTX>, result)
            } else {
                objectType = fieldType as GJavaObjectType<CTX>
            }

            val subSelectionSet = mergeSelectionSets(fields)

            return@supervisorScope executeSelectionSet(subSelectionSet, objectType, result, fieldPath)
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

    private fun coerceArgumentValues(field: SelectionField, fieldMethod: FieldGetter<CTX>, fieldPath: FieldPath): Map<String, Any?> {
        val coercedValues = LinkedHashMap<String, Any?>()
        val argumentValues = field.arguments

        for ((argumentName, paramInfo) in fieldMethod.publicParams) {
            val argumentType = schema.getJavaType(paramInfo.type.sourceType)
            val hasExplicitValue = argumentValues.containsKey(argumentName)
            val valueOrVar = argumentValues[argumentName]

            if (hasExplicitValue && valueOrVar is Variable) {
                val imported = try {
                    inputVarParser.parseVar(valueOrVar, paramInfo.type.sourceType)
                } catch (e: Exception) {
                    throw FieldException("Failed to parse field: " + e.message, fieldPath, e)
                }

                coercedValues[argumentName] = imported
                continue
            }

            if (!hasExplicitValue || valueOrVar is Variable) {
                if (paramInfo.defaultValue != null) {
                    val value: Any
                    try {
                        value = argumentType.getFromJson(paramInfo.defaultValue, inputVarParser)
                    } catch (e: Exception) {
                        throw FieldException("Couldn't use default value for argument " + argumentName + ": " + e.message, fieldPath, e)
                    }

                    coercedValues[argumentName] = value
                    continue
                } else if (argumentType is GJavaNotNullType<*>) {
                    throw FieldException("Missing value for not null argument $argumentName", fieldPath)
                }
            }

            if (valueOrVar == null || valueOrVar is ValueNull) {
                if (argumentType is GJavaNotNullType<*>) {
                    throw FieldException("Argument $argumentName is not null, but null was provided as the value", fieldPath)
                } else {
                    coercedValues[argumentName] = null
                    continue
                }
            }

            valueOrVar as Value

            val parsed = try {
                inputVarParser.parseVar(valueOrVar, paramInfo.type.sourceType)
            } catch (e: Exception) {
                throw FieldException("Failed to parse value: " + e.message, fieldPath, e)
            }

            coercedValues[argumentName] = parsed
        }

        return coercedValues
    }

    fun collectFields(objectType: GJavaObjectType<CTX>, selectionSet: List<Selection>, visitedFragments: MutableSet<String>): MutableMap<String, MutableList<SelectionField>> {
        val groupedFields = LinkedHashMap<String, MutableList<SelectionField>>()

        for (selection in selectionSet) {
            if (shouldSkip(selection))
                continue

            if (selection is SelectionField) {
                val responseKey = selection.responseKey
                groupedFields.getOrPut(responseKey) { ArrayList() }.add(selection)
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
                val fragmentGroupedFieldSet = collectFields(objectType, fragmentSelectionSet, visitedFragments)
                appendLists(groupedFields, fragmentGroupedFieldSet)
            } else if (selection is SelectionInlineFragment) {
                if (selection.typeConditionOpt != null) {
                    val fragmentTypeName = selection.typeConditionOpt.name
                    val fragmentType = schema.getGQLBaseType(fragmentTypeName)

                    if (!doesFragmentTypeApply(objectType.gqlType, fragmentType))
                        continue
                }
                val fragmentSelectionSet = selection.selectionSet
                val fragmentGroupedFieldSet = collectFields(objectType, fragmentSelectionSet, visitedFragments)
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

    private fun shouldSkip(selection: Selection): Boolean {
        val skipDir = selection.findDirective("skip")
        if (skipDir != null) {
            val skip = extractBooleanValue(skipDir, "if")
            if (skip != null && skip)
                return true
        }

        val includeDir = selection.findDirective("include")
        if (includeDir != null) {
            val include = extractBooleanValue(includeDir, "if")
            if (include == null || !include)
                return true
        }

        return false
    }

    private fun extractBooleanValue(directive: Directive, arg: String): Boolean? {
        val valueOrVar = directive.args[arg] ?: return null

        val importedValue = inputVarParser.parseVar(valueOrVar, GJavaBooleanArrayType.NULLABLE_BOOL_TYPE)

        return when {
            importedValue is Boolean -> importedValue
            importedValue != null -> throw ValidationException("Variable $$arg should be a boolean")
            else -> null
        }
    }

    private fun coerceVariableValues(op: OperationDefinition): Map<String, ValueOrNull> {
        val coercedValues = LinkedHashMap<String, ValueOrNull>()

        for ((varName, value) in op.varDefs) {
            val varTypeDef = value.type
            val defaultValue = value.defaultValue

            if (rawVariables.containsKey(varName)) {
                val type = getType(varTypeDef)

                when (val rawVal = rawVariables.getValue(varName)) {
                    is Value -> coercedValues[varName] = type.coerceValue(rawVal)
                    is ValueNull -> coercedValues[varName] = rawVal
                }
            } else if (defaultValue != null) {
                coercedValues[varName] = defaultValue
            } else {
                if (varTypeDef is NotNullType)
                    throw QueryException("Missing data for variable $$varName")
            }
        }

        return coercedValues
    }

    private fun getType(typeDef: TypeDef): GType {
        return when (typeDef) {
            is NamedType -> {
                val typeName = typeDef.name
                schema.getGQLBaseType(typeName)
            }
            is NotNullType -> getType(typeDef.inner).notNull()
            is ListType -> getType(typeDef.inner).listOf()
        }
    }

    val operation: OperationDefinition
        get() = if (opName == null) {
            if (opsByName.size == 1) {
                opsByName.values.iterator().next()
            } else {
                throw QueryException("Missing operationName")
            }
        } else opsByName.get<String?, OperationDefinition>(opName) ?: throw QueryException("Operation $opName not found")

    private fun addError(message: String, fieldPath: FieldPath?, location: Token<*>?) {
        val error = mutableMapOf<String, Any?>("message" to message)

        if (fieldPath != null)
            error["path"] = fieldPath.toArray()

        if (location != null) {
            error["locations"] = arrayOf(mapOf(
                "line" to location.row,
                "column" to location.column
            ))
        }

        errors.add(error)
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
        // note we assume a valid query here, given the schema..
        // (so we don't check for duplicate names, etc. etc.)
        for (def in query!!.definitions) {
            when (def) {
                is OperationDefinition -> opsByName[def.name] = def
                is FragmentDefinition -> fragmentsByName[def.name] = def
            }
        }
    }

    private fun parseQuery() {
        this.query = GraphQLParser.parseQueryDoc(rawQuery)
    }

    companion object {
        val logger = getLogger<SimpleQueryState<*,*>>()
    }
}


fun <T: Any> KClass<T>.findMethod(name: String): KCallable<*> {
    for (member in this.members) {
        if (member.name == name)
            return member
    }
    throw IllegalArgumentException("Couldn't find $name in $this")
}