package com.xs0.gqlktx.codegen

import com.xs0.gqlktx.codegen.OutputExportKind.*
import com.xs0.gqlktx.schema.Schema
import com.xs0.gqlktx.schema.builder.AutoBuilder
import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.schema.builder.nonNullType
import com.xs0.gqlktx.schema.builder.nullableType
import com.xs0.gqlktx.schema.intro.GqlIntroEnumValue
import com.xs0.gqlktx.types.gql.*
import com.xs0.gqlktx.types.kotlin.*
import com.xs0.gqlktx.types.kotlin.lists.GJavaArrayType
import com.xs0.gqlktx.types.kotlin.lists.GJavaCollectionType
import com.xs0.gqlktx.types.kotlin.scalars.GJavaStandardEnum
import io.pebbletemplates.pebble.PebbleEngine
import io.pebbletemplates.pebble.loader.ClasspathLoader
import io.pebbletemplates.pebble.template.PebbleTemplate
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.reflect.KType

const val MAX_FIELDS_PER_FUNCTION = 80

fun stateClassName(schemaSource: KClass<*>): Pair<String, String> {
    val stateType = "GqlState" + schemaSource.simpleName
    val statePackage = schemaSource.packageName()!!

    return statePackage to stateType
}

fun executorClassName(schemaSource: KClass<*>): Pair<String, String> {
    return stateClassName(schemaSource).first to "GqlExecutor" + schemaSource.simpleName
}

class CodeGen<SCHEMA: Any, CTX: Any> private constructor(val schemaSource: KClass<SCHEMA>, val contextType: KClass<CTX>, val rootDir: Path) {
    lateinit var schema: Schema<SCHEMA, CTX>
    lateinit var stateType: String
    lateinit var statePackage: String

    val engine = PebbleEngine.Builder().newLineTrimming(false).
                                        defaultEscapingStrategy("js").
                                        autoEscaping(false).
                                        strictVariables(true).
                                        loader(ClasspathLoader().apply { prefix = "codegen/"; suffix = ".pebble" }).
                                        build()

    val templateCache = HashMap<String, PebbleTemplate>()
    val superTypes = HashMap<String, HashSet<String>>()

    fun CodeWriter.varContext(includeContext: Boolean): HashMap<String, Any?> {
        if (includeContext) {
            addImport(statePackage, stateType)
            addImport(contextType)
        }

        val context = HashMap<String, Any?>()
        if (includeContext) {
            context["stateType"] = stateType
            context["contextType"] = contextType.simpleName
        }

        return context
    }

    fun build() {
        schema = AutoBuilder(schemaSource, contextType).build()

        determineSuspendings()

        for (type in schema.allBaseTypes) {
            if (type is GInterfaceType) {
                type.implementations.forEach { impl ->
                    superTypes.getOrPut(impl.name) { HashSet() }.add(type.name)
                }
            } else
            if (type is GObjectType) {
                superTypes.getOrPut(type.name) { HashSet() }.add(type.name)
            }
        }

        stateClassName(schemaSource).let {
            statePackage = it.first
            stateType = it.second
        }

        val introDataType = "IntroData" + schemaSource.simpleName
        val queryType = schema.getJavaType(schema.queryRoot.type)
        val mutType = schema.mutationRoot?.type?.let { schema.getJavaType(it) }

        outputKotlin(schemaSource.nonNullType(), "gqlktxDefs" + schemaSource.simpleName).use { w ->
            val context = w.varContext(true)
            val iContext = w.varContext(false)

            w.addImport(schemaSource)
            context["rootType"] = schemaSource.simpleName
            context["executorName"] = executorClassName(schemaSource).second

            val getQueryExpr = schema.queryRoot.codeGen("rootObject")
            val queryInfo = queryType.outputExportInfo(this)

            context["queryExpr"] = queryInfo.exprTemplate.replace("SUBOBJ", getQueryExpr).
                                                          replace("SUBPATH", "FieldPath.root()").
                                                          replace("SUBSEL", "selectionSet").
                                                          replace("state", "this")

            val getMutExpr = schema.mutationRoot?.codeGen("rootObject")
            if (getMutExpr != null && mutType != null) {
                val mutInfo = mutType.outputExportInfo(this)

                context["mutationExpr"] = mutInfo.exprTemplate.replace("SUBOBJ", getMutExpr).
                                                               replace("SUBPATH", "FieldPath.root()").
                                                               replace("SUBSEL", "selectionSet").
                                                               replace("state", "this")
            } else {
                context["mutationExpr"] = """throw QueryException("Mutations not supported.")"""
            }

            context["introDataType"] = introDataType

            val introDataPackage = schemaSource.packageName() ?: statePackage
            iContext["introDataType"] = introDataType
            iContext["queryTypeName"] = queryType.gqlType.baseType.gqlTypeString
            iContext["mutationTypeName"] = mutType?.gqlType?.baseType?.gqlTypeString
            iContext["packageName"] = introDataPackage
            iContext["schemaName"] = schemaSource.simpleName

            val jsonTypeKinds = LinkedHashMap<String, TypeKind>()
            val jsonEnumValues = LinkedHashMap<String, List<EnumValueIntroData>>()
            val jsonFields = LinkedHashMap<String, List<FieldIntroData>>()
            val jsonInputFields = LinkedHashMap<String, List<InputValueIntroData>>()
            val jsonInterfaces = LinkedHashMap<String, List<String>>()
            val jsonPossibleTypes = LinkedHashMap<String, List<String>>()
            val jsonDescriptions = LinkedHashMap<String, String>()

            for (type in schema.allBaseTypes.sortedBy { it.name }) {
                if (type.name.startsWith("__"))
                    continue

                jsonTypeKinds[type.name] = type.kind

                if (type.description != null) {
                    jsonDescriptions[type.name] = type.description
                }

                if (type is GFieldedType) {
                    jsonFields[type.name] = type.fields.values.map {
                        FieldIntroData(
                            name = it.name,
                            typeName = it.type.gqlTypeString,
                            args = it.arguments.values.map { arg ->
                                InputValueIntroData(
                                    name = arg.name,
                                    typeName = arg.type.gqlTypeString,
                                    description = arg.description,
                                    defaultValue = arg.defaultValue?.toString(),
                                )
                            },
                            description = it.description,
                            isDeprecated = it.deprecated,
                            deprecationReason = it.deprecationReason,
                        )
                    }

                    if (type.getInterfaces().isNotEmpty()) {
                        jsonInterfaces[type.name] = type.getInterfaces().map { it.gqlTypeString }
                    }
                }

                when (type) {
                    is GInterfaceType -> {
                        jsonPossibleTypes[type.name] = type.implementations.map { it.gqlTypeString }
                    }
                    is GUnionType -> {
                        jsonPossibleTypes[type.name] = type.members.map { it.gqlTypeString }
                    }
                    is GInputObjType -> {
                        jsonInputFields[type.name] = type.inputFields.values.map {
                            InputValueIntroData(
                                name = it.name,
                                typeName = it.type.gqlTypeString,
                                description = it.description,
                                defaultValue = it.defaultValue?.toString(),
                            )
                        }
                    }
                    is GEnumType -> {
                        jsonEnumValues[type.name] = type.enumValues.map {
                            EnumValueIntroData(
                                it.name,
                                it.isDeprecated,
                                it.description,
                                it.deprecationReason
                            )
                        }
                    }
                }
            }

            writeJson(introDataPackage, "introData_" + schemaSource.simpleName + "_typeKinds.json",     jsonTypeKinds)
            writeJson(introDataPackage, "introData_" + schemaSource.simpleName + "_enumValues.json",    jsonEnumValues)
            writeJson(introDataPackage, "introData_" + schemaSource.simpleName + "_fields.json",        jsonFields)
            writeJson(introDataPackage, "introData_" + schemaSource.simpleName + "_inputFields.json",   jsonInputFields)
            writeJson(introDataPackage, "introData_" + schemaSource.simpleName + "_interfaces.json",    jsonInterfaces)
            writeJson(introDataPackage, "introData_" + schemaSource.simpleName + "_possibleTypes.json", jsonPossibleTypes)
            writeJson(introDataPackage, "introData_" + schemaSource.simpleName + "_descriptions.json",  jsonDescriptions)

            template("common/queryState").evaluate(w, context)
            template("common/executor").evaluate(w, context)
            template("common/introData").evaluate(w, iContext)
        }

        val inputGroups = HashMap<KType, HashSet<GJavaType<CTX>>>()
        for (type in findActualInputs()) {
            val inputBase = type.deepestInputElementType()
            val set = inputGroups.getOrPut(inputBase.type) { HashSet() }
            var curr = type
            while (true) {
                set.add(curr)
                curr = curr.inputElementType() ?: break
            }
        }

        for ((baseType, group) in inputGroups) {
            val imports = HashSet<Pair<String, String>>()

            val types = group.mapNotNull { t ->
                val info = t.inputParseInfo(this)
                val tc = HashMap<String, Any?>()
                tc["kind"] = info.kind.name
                tc["funName"] = info.funName
                tc["funReturnType"] = info.funReturnType
                tc["funCreateType"] = info.funCreateType
                tc["gqlName"] = t.name.gqlName
                imports.addAll(info.importsForGen)

                when (info.kind) {
                    InputParseKind.BASELINE -> {
                        return@mapNotNull null
                    }
                    InputParseKind.MAYBE -> {
                        t as GJavaMaybeType<CTX>
                        val pi = t.innerType.inputParseInfo(this)
                        imports.addAll(pi.importsForUse)
                        tc["innerExpr"] = pi.exprTemplate.replace("VALUE", "value")
                    }
                    InputParseKind.NOT_NULL -> {
                        t as GJavaNotNullType<CTX>
                        val pi = t.innerType.inputParseInfo(this)
                        imports.addAll(pi.importsForUse)
                        tc["innerExpr"] = pi.exprTemplate.replace("VALUE", "value")
                    }
                    InputParseKind.COLLECTION_OF  -> {
                        t as GJavaCollectionType<CTX>
                        val pi = t.elementType.inputParseInfo(this)
                        imports.addAll(pi.importsForUse)
                        tc["innerExpr"] = pi.exprTemplate.replace("VALUE", "el")
                    }
                    InputParseKind.ARRAY_OF -> {
                        t as GJavaArrayType<CTX>
                        val pi = t.elementType.inputParseInfo(this)
                        imports.addAll(pi.importsForUse)
                        tc["innerExpr"] = pi.exprTemplate.replace("VALUE", "value.elements[i]")
                    }
                    InputParseKind.INPUT_OBJECT -> {
                        t as GJavaInputObjectType<CTX>
                        tc["params"] = t.info.props.map { prop ->
                            val pi = schema.types.getValue(prop.type.sourceType).inputParseInfo(this)
                            imports.addAll(pi.importsForUse)

                            mapOf(
                                "name" to prop.name,
                                "mode" to prop.propMode.name,
                                "hasDefault" to (prop.defaultValue != null),
                                "defaultValue" to prop.defaultValue?.codeGen(),
                                "parseExpr" to pi.exprTemplate.replace("VALUE", "v")
                            )
                        }
                    }
                    InputParseKind.ENUM -> {
                        t as GJavaStandardEnum<CTX>
                        tc["enumValues"] = t.values.buildCodeGenInfo(imports)
                    }
                }

                tc
            }

            if (types.isEmpty())
                continue

            outputKotlin(baseType.packageName()?.excludeSystemPackages(this) ?: statePackage, "inputParser" + schema.types.getValue(baseType).inputParseInfo(this).funName).use { w ->
                val context = w.varContext(false)
                for ((packageName, name) in imports)
                    w.addImport(packageName, name)
                w.addImport("com.xs0.gqlktx.dom", "*")
                w.addImport("com.xs0.gqlktx", "ValidationException")

                context["types"] = types

                template("input/varParser").evaluate(w, context)
            }
        }


        val groups = HashMap<KType, HashSet<GJavaType<CTX>>>()

        for (type in findActualOutputs()) {
            val outputBase = type.deepestInputElementType()
            val set = groups.getOrPut(outputBase.type) { HashSet() }
            var curr = type
            while (true) {
                set.add(curr)
                curr = curr.inputElementType() ?: break
            }
        }

        for ((_, group) in groups) {
            val base = group.first().deepestInputElementType()

            when {
                base is GJavaObjectType -> codeGenObject(base, group, base == queryType.unwrapNotNull(), base == mutType?.unwrapNotNull())
                base is GJavaInputObjectType -> codeGenInput(base, group)
                base is GJavaInterfaceType -> codeGenInterface(base, group)
                base is GJavaUnionType -> codeGenInterface(base, group)
                base is GJavaScalarLikeType -> codeGenScalar(base, group)
                else -> { println("Unknown: $base") }
            }
        }
    }

    private fun GJavaType<CTX>.unwrapNotNull(): GJavaType<CTX> {
        return when (this) {
            is GJavaNotNullType -> this.innerType
            else -> this
        }
    }

    fun findActualInputs(): Set<GJavaType<CTX>> {
        val set = HashSet<GJavaType<CTX>>()

        for ((_, type) in schema.types) {
            if (type.gqlType.kind != TypeKind.OBJECT)
                continue

            type as GJavaObjectType

            for (field in type.fields) {
                for (param in field.value.publicParams) {
                    var paramType = schema.types.getValue(param.value.type.sourceType)
                    findActualInputs(set, paramType)
                }
            }
        }

        return set
    }

    fun findActualInputs(set: HashSet<GJavaType<CTX>>, type: GJavaType<CTX>) {
        if (set.add(type)) {
            type.inputElementType()?.let { findActualInputs(set, it) }
            if (type is GJavaInputObjectType) {
                type.info.props.forEach { prop ->
                    findActualInputs(set, schema.getJavaType(prop.type.sourceType))
                }
            }
        }
    }

    fun findActualOutputs(): Set<GJavaType<CTX>> {
        val set = HashSet<GJavaType<CTX>>()

        schema.queryRoot.type.let { findActualOutputs(set, schema.getJavaType(it)) }
        schema.mutationRoot?.type?.let { findActualOutputs(set, schema.getJavaType(it)) }

        if (System.currentTimeMillis() < 0) {
            for ((_, type) in schema.types) {
                if (type is GJavaObjectType) {
                    findActualOutputs(set, type)
                }
            }
        }

        return set
    }

    fun findActualOutputs(set: HashSet<GJavaType<CTX>>, type: GJavaType<CTX>) {
        if (set.add(type)) {
            type.inputElementType()?.let { findActualOutputs(set, it) }
            if (type is GJavaObjectType) {
                for ((_, field) in type.fields) {
                    val retType = schema.getJavaType(field.publicType.sourceType)
                    findActualOutputs(set, retType)
                }
            } else
            if (type is GJavaImplementableType) {
                for (impl in type.implementations) {
                    val subType = schema.getJavaType(impl.nullableType())
                    findActualOutputs(set, subType)
                }
            }
        }
    }

    fun template(name: String): PebbleTemplate {
        return templateCache.getOrPut(name) { engine.getTemplate(name) }
    }

    fun outputResource(packageName: String, filename: String): Writer {
        var res = rootDir.resolve("resources")
        for (dir in packageName.split("."))
            res = res.resolve(dir)

        Files.createDirectories(res)

        return Files.newBufferedWriter(res.resolve(filename), Charsets.UTF_8)
    }

    fun writeJson(packageName: String, filename: String, value: Any) {
        outputResource(packageName, filename).use { w ->
            INTRO_JSON_MAPPER.writeValue(w, value)
        }
    }

    fun outputKotlin(packageName: String, filename: String): CodeWriter {
        var res = rootDir.resolve("kotlin")
        for (dir in packageName.split("."))
            res = res.resolve(dir)

        Files.createDirectories(res)

        val fw = Files.newBufferedWriter(res.resolve("$filename.kt"), Charsets.UTF_8)
        return CodeWriter(fw, packageName).start()
    }

    fun outputKotlin(baseClass: KType, filename: String): CodeWriter {
        val cl = baseClass.classifier

        when (cl) {
            is KClass<*> -> {
                val packageName = cl.packageName() ?: statePackage
                return outputKotlin(packageName, filename)
            }
            else -> throw IllegalStateException("Invalid type of type classifier.")
        }
    }

    fun outputKotlinFor(type: GJavaType<*>): CodeWriter {
        return outputKotlin(type.type, "gql" + type.gqlType.gqlTypeString)
    }

    fun codeGenObject(base: GJavaObjectType<CTX>, group: Set<GJavaType<CTX>>, isQueryRoot: Boolean, isMutationRoot: Boolean) {
        val imports = HashSet<Pair<String, String>>()

        val name = base.gqlType.name

        val types = group.map { t ->
            val info = t.outputExportInfo(this)

            val concurrent = info.funIsSuspending && !isMutationRoot
            val tc = HashMap<String, Any?>()
            tc["typeName"] = t.name.codeGenTypeEx
            tc["typeNameNN"] = t.name.codeGenTypeExNN
            tc["kind"] = info.kind.name
            tc["funName"] = info.funName
            tc["funReturnType"] = info.funReturnType
            tc["gqlName"] = t.name.gqlName
            tc["suspending"] = info.funIsSuspending
            tc["concurrent"] = concurrent
            tc["isQueryRoot"] = info.kind == OBJECT && isQueryRoot
            tc["anyNeedsCoercion"] = false
            tc["superTypeNames"] = superTypes[name] ?: throw IllegalStateException("Missing super types")
            imports.addAll(info.importsForGen)

            when (info.kind) {
                BASELINE -> {
                    throw IllegalStateException()
                }
                NOT_NULL -> {
                    t as GJavaNotNullType
                    val subInfo = t.innerType.outputExportInfo(this)
                    imports.addAll(subInfo.importsForUse)

                    tc["innerExpr"] = subInfo.exprTemplate.replace("SUBOBJ", "obj").
                                                           replace("SUBPATH", "parentPath").
                                                           replace("SUBSEL", "selectionSet")
                }
                ARRAY_OF -> {
                    t as GJavaArrayType
                    val subInfo = t.elementType.outputExportInfo(this)
                    imports.addAll(subInfo.importsForUse)

                    tc["innerExpr"] = subInfo.exprTemplate.replace("SUBOBJ", "el").
                                                           replace("SUBPATH", "parentPath.listElement(i)").
                                                           replace("SUBSEL", "selectionSet")
                }
                COLLECTION_OF -> {
                    t as GJavaCollectionType
                    val subInfo = t.elementType.outputExportInfo(this)
                    imports.addAll(subInfo.importsForUse)

                    tc["innerExpr"] = subInfo.exprTemplate.replace("SUBOBJ", "el").
                                                           replace("SUBPATH", "parentPath.listElement(i)").
                                                           replace("SUBSEL", "selectionSet")
                }
                ENUM -> {
                    throw IllegalStateException()
                }
                OBJECT -> {
                    val fields = base.fields.entries.sortedBy { it.key }.map { (name, getter) ->
                        val field = HashMap<String, Any?>()
                        val subType = schema.getJavaType(getter.publicType.sourceType)
                        val subInfo = subType.outputExportInfo(this)

                        imports.addAll(subInfo.importsForUse)

                        field["name"] = name
                        field["nullable"] = getter.publicType.nullable
                        field["params"] = getter.publicParams.map { (_, info) ->
                            val paramType = schema.types.getValue(info.type.sourceType)
                            val paramInfo = paramType.inputParseInfo(this)
                            imports.addAll(paramInfo.importsForUse)

                            val paramCtx = HashMap<String, Any?>()
                            paramCtx["name"] = info.name
                            paramCtx["varName"] = "_" + info.name
                            paramCtx["hasDefault"] = info.defaultValue != null
                            paramCtx["defaultExpr"] = info.defaultValue?.codeGen()
                            paramCtx["required"] = !paramInfo.nullable
                            paramCtx["paramExpr"] = paramInfo.exprTemplate.replace("VALUE", "it")
                            paramCtx
                        }
                        field["getterMode"] = getter.mode.name
                        field["getterCall"] = getter.codeGenCall("obj", "state.context")

                        field["hasSubSelections"] = subType.hasSubSelections()

                        if (subType.hasSubSelections()) {
                            field["exportExpr"] = subInfo.exprTemplate.
                                                          replace("SUBOBJ", "rawValue").
                                                          replace("SUBPATH", "parentPath.subField(fieldName)").
                                                          replace("SUBSEL", "subSelection")
                        } else {
                            field["exportExpr"] = subInfo.exprTemplate.replace("VALUE", "rawValue")
                            tc["anyNeedsCoercion"] = true
                        }

                        field
                    }

                    if (fields.size > MAX_FIELDS_PER_FUNCTION) {
                        val chunks = ArrayList<Map<String, Any?>>()

                        fun makeChunks(start: Int, end: Int) {
                            if (end - start > MAX_FIELDS_PER_FUNCTION) {
                                val mid = start + (end - start) / 2
                                makeChunks(start, mid)
                                makeChunks(mid, end)
                            } else {
                                chunks += mapOf(
                                    "i" to chunks.size,
                                    "last" to fields[end - 1]["name"],
                                    "fields" to fields.subList(start, end)
                                )
                            }
                        }
                        makeChunks(0, fields.size)

                        tc["splitFields"] = true
                        tc["fieldChunks"] = chunks
                    } else {
                        tc["splitFields"] = false
                        tc["fields"] = fields
                    }
                }
                INTERFACE -> {
                    throw IllegalStateException()
                }
            }

            tc
        }

        outputKotlinFor(base).use { w ->
            val context = w.varContext(true)

            for (i in imports)
                w.addImport(i.first, i.second)

            context["types"] = types

            template("output/object").evaluate(w, context)
        }
    }

    fun codeGenInput(base: GJavaInputObjectType<CTX>, group: Set<GJavaType<CTX>>) {
        // nothing
    }

    fun codeGenInterface(base: GJavaImplementableType<CTX>, group: Set<GJavaType<CTX>>) {
        val imports = HashSet<Pair<String, String>>()

        val types = group.map { t ->
            val info = t.outputExportInfo(this)

            val concurrent = info.funIsSuspending
            val tc = HashMap<String, Any?>()
            tc["typeName"] = t.name.codeGenTypeEx
            tc["kind"] = info.kind.name
            tc["funName"] = info.funName
            tc["funReturnType"] = info.funReturnType
            tc["gqlName"] = t.name.gqlName
            tc["suspending"] = info.funIsSuspending
            tc["concurrent"] = concurrent
            tc["isQueryRoot"] = false
            tc["anyNeedsCoercion"] = false
            imports.addAll(info.importsForGen)

            when (info.kind) {
                BASELINE -> {
                    throw IllegalStateException()
                }
                NOT_NULL -> {
                    t as GJavaNotNullType
                    val subInfo = t.innerType.outputExportInfo(this)
                    imports.addAll(subInfo.importsForUse)

                    tc["innerExpr"] = subInfo.exprTemplate.replace("SUBOBJ", "obj").
                                                           replace("SUBPATH", "parentPath").
                                                           replace("SUBSEL", "selectionSet")
                }
                ARRAY_OF -> {
                    t as GJavaArrayType
                    val subInfo = t.elementType.outputExportInfo(this)
                    imports.addAll(subInfo.importsForUse)

                    tc["innerExpr"] = subInfo.exprTemplate.replace("SUBOBJ", "el").
                                                           replace("SUBPATH", "parentPath.listElement(i)").
                                                           replace("SUBSEL", "selectionSet")
                }
                COLLECTION_OF -> {
                    t as GJavaCollectionType
                    val subInfo = t.elementType.outputExportInfo(this)
                    imports.addAll(subInfo.importsForUse)

                    tc["innerExpr"] = subInfo.exprTemplate.replace("SUBOBJ", "el").
                                                           replace("SUBPATH", "parentPath.listElement(i)").
                                                           replace("SUBSEL", "selectionSet")
                }
                ENUM -> {
                    throw IllegalStateException()
                }
                OBJECT -> {
                    throw IllegalStateException()
                }
                INTERFACE -> {
                    t as GJavaImplementableType

                    tc["subTypes"] = t.implementations.map { subT ->
                        val subType = schema.types.getValue(subT.nullableType())
                        val subInfo = subType.outputExportInfo(this)

                        imports.addAll(subInfo.importsForUse)
                        imports.addAll(subType.name.imports)

                        val subCtx = HashMap<String, Any?>()
                        subCtx["type"] = subType.name.codeGenTypeNN
                        subCtx["innerExpr"] = subInfo.exprTemplate.
                                                      replace("SUBOBJ", "obj").
                                                      replace("SUBPATH", "parentPath").
                                                      replace("SUBSEL", "selectionSet")
                        subCtx
                    }
                }
            }

            tc
        }

        outputKotlinFor(base).use { w ->
            val context = w.varContext(true)

            for (i in imports)
                w.addImport(i.first, i.second)

            context["types"] = types

            template("output/interface").evaluate(w, context)
        }
    }

    fun codeGenScalar(base: GJavaScalarLikeType<CTX>, group: Set<GJavaType<CTX>>) {
        val imports = HashSet<Pair<String, String>>()

        val types = group.mapNotNull { t ->


            val info = t.outputExportInfo(this)

            val tc = HashMap<String, Any?>()
            tc["nullableValue"] = t.name.isNullableType
            tc["typeName"] = t.name.codeGenTypeEx
            tc["typeNameNN"] = t.name.codeGenTypeExNN
            tc["kind"] = info.kind.name
            tc["funName"] = info.funName
            tc["funReturnType"] = info.funReturnType
            tc["gqlName"] = t.name.gqlName
            imports.addAll(info.importsForGen)

            when (info.kind) {
                BASELINE -> {
                    return@mapNotNull null
                }
                NOT_NULL -> {
                    t as GJavaNotNullType
                    val subInfo = t.innerType.outputExportInfo(this)
                    imports.addAll(subInfo.importsForUse)
                    tc["innerExpr"] = subInfo.exprTemplate.replace("VALUE", "value")
                }
                ARRAY_OF -> {
                    t as GJavaArrayType
                    val subInfo = t.elementType.outputExportInfo(this)
                    imports.addAll(subInfo.importsForUse)
                    tc["innerExpr"] = subInfo.exprTemplate.replace("VALUE", "v")
                }
                COLLECTION_OF -> {
                    t as GJavaCollectionType
                    val subInfo = t.elementType.outputExportInfo(this)
                    imports.addAll(subInfo.importsForUse)
                    tc["innerExpr"] = subInfo.exprTemplate.replace("VALUE", "v")
                }
                ENUM -> {
                    t as GJavaStandardEnum
                    tc["enumValues"] = t.values.buildCodeGenInfo(imports)
                }
                OBJECT -> {
                    throw IllegalStateException()
                }
                INTERFACE -> {
                    throw IllegalStateException()
                }
            }

            tc
        }

        val ei = base.outputExportInfo(this)
        outputKotlin(ei.outPackageName, "export" + ei.funName).use { w ->
            val context = w.varContext(false)

            for (i in imports)
                w.addImport(i.first, i.second)

            context["types"] = types

            template("output/scalar").evaluate(w, context)
        }
    }

    fun determineSuspendings() {
        val allTypes = schema.types.values.toList()

        var any = true
        while (any) {
            any = false
            for (type in allTypes) {
                if (type.processSuspendingDetermination(this)) {
                    any = true
                }
            }
        }
    }

    companion object {
        fun <SCHEMA: Any, CTX: Any> build(schema: KClass<SCHEMA>, contextType: KClass<CTX>, outputRootDir: Path) {
            CodeGen(schema, contextType, outputRootDir).build()
        }
    }
}

fun List<GqlIntroEnumValue>.buildCodeGenInfo(imports: HashSet<Pair<String, String>>): List<Map<String, Any?>> {
    val name = AutoBuilder.resolveName(first().sourceClass, null)

    imports.addAll(name.imports)

    return map { enum ->
        mapOf(
            "publicName" to enum.name,
            "value" to name.codeGenTypeNN + "." + enum.rawValue.name
        )
    }
}