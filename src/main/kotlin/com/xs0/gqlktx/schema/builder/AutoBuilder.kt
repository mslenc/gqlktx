package com.xs0.gqlktx.schema.builder

import com.github.mslenc.utils.getLogger
import com.xs0.gqlktx.*
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.schema.Schema
import com.xs0.gqlktx.schema.intro.GqlIntroSchema
import com.xs0.gqlktx.types.gql.*
import com.xs0.gqlktx.types.kotlin.*
import com.xs0.gqlktx.types.kotlin.lists.*
import com.xs0.gqlktx.types.kotlin.scalars.*
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult
import kotlin.reflect.KClass
import kotlin.reflect.KType
import java.util.*

import com.xs0.gqlktx.schema.builder.TypeKind.*
import com.xs0.gqlktx.utils.NodeId
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSuperclassOf

fun <T: Any> KClass<T>.nullableType(): KType {
    return createType(nullable = true)
}

fun <T: Any> KClass<T>.nonNullType(): KType {
    return createType(nullable = false)
}

class AutoBuilder<SCHEMA: Any, CTX: Any>(schema: KClass<SCHEMA>, contextType: KClass<CTX>) {

    private val schema = SchemaBuilder(schema, contextType)
    private val latentChecks = ArrayList<() -> Unit>()
    private var classPathScanSpec: Array<out String> = arrayOf()
    private var allowIntrospectionNames: Boolean = false
    private val contextTypes = findContextTypes(contextType)
    private val cachedOutputMethods = HashMap<KClass<*>, Map<String, FieldGetter<CTX>>>()

    private val scanResult by lazy<ScanResult> {
        FastClasspathScanner(*classPathScanSpec).scan()
    }

    init {
        setUpScalars()
        setUpIntrospectionTypes()
    }


    fun scanOutputMethods(klass: KClass<*>): Map<String, FieldGetter<CTX>> {
        return cachedOutputMethods.computeIfAbsent(klass) {
            findFields(it, contextTypes)
        }
    }

    fun setClassPathScanSpec(vararg paths: String) {
        this.classPathScanSpec = paths
    }

    protected fun add(javaType: GJavaType<CTX>) {
        schema.add(javaType)
    }

    protected fun <T : GBaseType> addBaseType(baseType: T): T {
        return schema.addBaseType(baseType)
    }

    protected fun getOrCreateScalarType(name: String, validator: (Value)->Any): GScalarType {
        val existingType = schema.getBaseType(name)
        return if (existingType != null) {
            existingType as? GScalarType ?: throw IllegalStateException("Type $name already exists, but is not a scalar type")
        } else schema.createScalarType(name, validator)

    }

    protected fun maybeAdd(javaType: GJavaType<CTX>): GJavaType<CTX> {
        val existing = schema.getJavaType(javaType.type)
        if (existing != null) {
            return existing
        } else {
            schema.add(javaType)
            return javaType
        }
    }

    protected fun setUpScalars() {
        val INT = getOrCreateScalarType("Int", ScalarUtils::validateInteger)
        val LONG = getOrCreateScalarType("Long", ScalarUtils::validateLong)
        val STRING = getOrCreateScalarType("String", ScalarUtils::validateString)
        val FLOAT = getOrCreateScalarType("Float", ScalarUtils::validateFloat)
        val BOOLEAN = getOrCreateScalarType("Boolean", ScalarUtils::validateBoolean)
        val ID = getOrCreateScalarType("ID", ScalarUtils::validateID)
        val BYTES = getOrCreateScalarType("Bytes", ScalarUtils::validateBytes)
        val DATE = getOrCreateScalarType("Date", ScalarUtils::validateDate)
        val TIME = getOrCreateScalarType("Time", ScalarUtils::validateTime)
        val DATETIME = getOrCreateScalarType("DateTime", ScalarUtils::validateDateTime)
        val INSTANT = getOrCreateScalarType("Instant", ScalarUtils::validateInstant)

        val javaByte = maybeAdd(GJavaByte(Byte::class.nonNullType(), INT.notNull()))
        val javaShort = maybeAdd(GJavaShort(Short::class.nonNullType(), INT.notNull()))
        val javaInt = maybeAdd(GJavaInteger(Int::class.nonNullType(), INT.notNull()))
        val javaLong = maybeAdd(GJavaLong(Long::class.nonNullType(), LONG.notNull()))
        val javaFloat = maybeAdd(GJavaFloat(Float::class.nonNullType(), FLOAT.notNull()))
        val javaDouble = maybeAdd(GJavaDouble(Double::class.nonNullType(), FLOAT.notNull()))
        val javaChar = maybeAdd(GJavaChar(Char::class.nonNullType(), STRING.notNull()))
        val javaBool = maybeAdd(GJavaBoolean(Boolean::class.nonNullType(), BOOLEAN.notNull()))

        maybeAdd(GJavaByte(Byte::class.nullableType(), INT))
        maybeAdd(GJavaShort(Short::class.nullableType(), INT))
        maybeAdd(GJavaInteger(Int::class.nullableType(), INT))
        maybeAdd(GJavaLong(Long::class.nullableType(), LONG))
        maybeAdd(GJavaFloat(Float::class.nullableType(), FLOAT))
        maybeAdd(GJavaDouble(Double::class.nullableType(), FLOAT))
        maybeAdd(GJavaChar(Char::class.nullableType(), STRING))
        maybeAdd(GJavaBoolean(Boolean::class.nullableType(), BOOLEAN))

        /* byte[] is treated specially, below */
        maybeAdd(GJavaShortArrayType(INT.notNull().listOf(), javaShort))
        maybeAdd(GJavaIntArrayType(INT.notNull().listOf(), javaInt))
        maybeAdd(GJavaLongArrayType(LONG.notNull().listOf(), javaLong))
        maybeAdd(GJavaFloatArrayType(FLOAT.notNull().listOf(), javaFloat))
        maybeAdd(GJavaDoubleArrayType(FLOAT.notNull().listOf(), javaDouble))
        maybeAdd(GJavaBooleanArrayType(BOOLEAN.notNull().listOf(), javaBool))

        maybeAdd(GJavaString(String::class.nullableType(), STRING))
        maybeAdd(GJavaString(String::class.nonNullType(), STRING.notNull()))

        maybeAdd(GJavaCharArray(CharArray::class.nullableType(), STRING))
        maybeAdd(GJavaCharArray(CharArray::class.nonNullType(), STRING.notNull()))

        maybeAdd(GJavaByteArray(ByteArray::class.nullableType(), BYTES))
        maybeAdd(GJavaByteArray(ByteArray::class.nonNullType(), BYTES.notNull()))

        maybeAdd(GJavaUUID(UUID::class.nullableType(), ID))
        maybeAdd(GJavaUUID(UUID::class.nonNullType(), ID.notNull()))

        maybeAdd(GJavaBigDecimal(BigDecimal::class.nullableType(), FLOAT))
        maybeAdd(GJavaBigDecimal(BigDecimal::class.nonNullType(), FLOAT.notNull()))

        maybeAdd(GJavaNodeId(NodeId::class.nullableType(), ID))
        maybeAdd(GJavaNodeId(NodeId::class.nonNullType(), ID.notNull()))

        maybeAdd(GJavaDate(LocalDate::class.nullableType(), DATE))
        maybeAdd(GJavaDate(LocalDate::class.nonNullType(), DATE.notNull()))

        maybeAdd(GJavaTime(LocalTime::class.nullableType(), TIME))
        maybeAdd(GJavaTime(LocalTime::class.nonNullType(), TIME.notNull()))

        maybeAdd(GJavaDateTime(LocalDateTime::class.nullableType(), DATETIME))
        maybeAdd(GJavaDateTime(LocalDateTime::class.nonNullType(), DATETIME.notNull()))

        maybeAdd(GJavaInstant(Instant::class.nullableType(), INSTANT))
        maybeAdd(GJavaInstant(Instant::class.nonNullType(), INSTANT.notNull()))
    }

    internal fun setUpIntrospectionTypes() {
        allowIntrospectionNames = true
        try {
            scanTypes(GqlIntroSchema::class.nonNullType(), false)
        } finally {
            allowIntrospectionNames = false
        }
    }

    fun build(): Schema<SCHEMA, CTX> {
        val schemaClass = schema.schemaClass

        val getQueryRoot = findRootMethod(schemaClass, GqlQueryRoot::class, "query", "getQuery") ?: throw IllegalArgumentException("Couldn't find getQuery in $schemaClass")

        val getMutationRoot = findRootMethod(schemaClass, GqlMutationRoot::class, "mutation", "getMutation")
        // (mutation is optional)

        scanTypes(getQueryRoot.type, false)
        if (getMutationRoot != null)
            scanTypes(getMutationRoot.type, false)

        validate()

        schema.setRoots(getQueryRoot, getMutationRoot)

        return schema.build()
    }

    protected fun validate() {
        for (check in latentChecks) {
            try {
                check()
            } catch (e: SchemaException) {
                throw e
            } catch (t: Throwable) {
                throw SchemaException(t)
            }

        }

        // TODO
    }

    private fun scanTypes(type: KType, isInput: Boolean) {
        if (schema.hasJavaType(type)) {
            val existing = schema.getJavaType(type)
            if (existing != null) {
                existing.checkUsage(isInput)
            } else {
                latentChecks.add({
                    schema.getJavaType(type)!!.checkUsage(isInput)
                })
            }
            return
        }

        // schema.markTypeAsBeingBuilt(type); // break resolution cycles

        val parsedType = SemiType.create(type) ?: throw IllegalArgumentException("Can't use type $type")

        val baseClass = parsedType.getBaseClass()

        var baseType: GJavaType<CTX>? = schema.getJavaType(baseClass)
        if (baseType == null)
            baseType = buildBaseType(baseClass, isInput)

        constructWrappedTypes(parsedType, baseType)
    }

    private fun constructWrappedTypes(parsedType: SemiType, baseType: GJavaType<CTX>): GJavaType<CTX> {
        val existing = schema.getJavaType(parsedType.sourceType)
        if (existing != null)
            return existing

        if (parsedType.isNotNull) {
            val nullableType = parsedType.nullableType()
            val innerType = constructWrappedTypes(nullableType, baseType)
            val result = GJavaNotNullType(parsedType.sourceType, innerType, innerType.gqlType.notNull())
            add(result)
            return result
        }

        if (parsedType.kind == SemiTypeKind.MAYBE) {
            val innerType = constructWrappedTypes(parsedType.inner!!, baseType)
            val result = GJavaMaybeType(parsedType.sourceType, innerType, innerType.gqlType)
            add(result)
            return result
        }

        if (parsedType.kind == SemiTypeKind.OBJECT || parsedType.kind == SemiTypeKind.PRIMITIVE_ARRAY)
            return baseType

        val innerType = constructWrappedTypes(parsedType.inner!!, baseType)
        val result: GJavaType<CTX>
        if (parsedType.kind == SemiTypeKind.ARRAY_OF) {
            result = GJavaArrayType(parsedType.sourceType, innerType, innerType.gqlType.listOf())
        } else
        if (parsedType.kind == SemiTypeKind.COLLECTION_OF) {
            result = GJavaCollectionType(parsedType.sourceType, innerType, innerType.gqlType.listOf())
        } else {
            throw AssertionError("??? " + parsedType.kind)
        }

        add(result)
        return result
    }

    private fun buildBaseType(baseClass: KClass<*>, isInput: Boolean): GJavaType<CTX> {
        val kind = determineKind(baseClass, isInput)

        when (kind) {
            SCALAR, ENUM -> {
                // ok
            }

            INPUT_OBJECT -> if (!isInput) {
                throw SchemaException("An input object type can only be used for input")
            }

            INTERFACE, OBJECT, UNION -> if (isInput) {
                throw SchemaException("An object, interface or union type can't be used for input")
            }

            else -> throw IllegalStateException("Unexpected kind $kind for a base type")
        }

        when (kind) {
            SCALAR -> return buildScalar(baseClass)
            ENUM -> return buildEnum(baseClass)
            OBJECT -> return buildObject(baseClass)
            INPUT_OBJECT -> return buildInputObject(baseClass)
            INTERFACE -> return buildInterface(baseClass)
            UNION -> return buildUnion(baseClass)

            else -> throw IllegalStateException("Unexpected kind $kind for a base type")
        }
    }

    private fun validGraphQLName(name: String): Boolean {
        return validGraphQLName(name, allowIntrospectionNames)
    }

    private fun buildInterface(baseClass: KClass<*>): GJavaInterfaceType<CTX> {
        val ann = baseClass.findAnnotation<GqlInterface>()

        val name: String = ann?.name.trimToNull() ?: resolveName(baseClass)
        if (!validGraphQLName(name))
            throw IllegalStateException("Name of $baseClass was determined to be $name, but that is not a valid GraphQL name")

        val fields = scanOutputMethods(baseClass)
        val gqlFields = LinkedHashMap<String, GField>()

        val gtype = GInterfaceType(name, gqlFields)
        addBaseType(gtype)

        val impls: Array<KClass<*>>
        if (ann != null && ann.implementedBy.isNotEmpty()) {
            impls = ann.implementedBy
        } else {
            impls = findImplementations(baseClass)
        }

        val result = GJavaInterfaceType<CTX>(baseClass.nullableType(), gtype, impls)
        add(result)

        continueScanningFields(baseClass, fields)

        for (impl in impls)
            scanTypes(impl.nullableType(), false)

        latentChecks.add({
            val gqlImpls = HashSet<GObjectType>()
            for (impl in impls) {
                val gqlType = schema.getJavaType(impl)?.gqlType ?: throw IllegalStateException("Missing info for $impl")
                if (gqlType is GObjectType) {
                    gqlImpls.add(gqlType)
                } else {
                    throw SchemaException("An implementation of " + gtype.gqlTypeString + " (" + impl + ") does not have an object type, but is " + gqlType.gqlTypeString)
                }
            }
            gtype.setImpls(gqlImpls)
        })

        latentChecks.add({ fillInFields(fields, gqlFields) })

        return result
    }

    private fun continueScanningFields(baseClass: KClass<*>, fields: Map<String, FieldGetter<CTX>>) {
        for (field in fields.values) {
            try {
                scanTypes(field.publicType.sourceType, false)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to scan type of ${field.name} in $baseClass", e)
            }

            for (pp in field.publicParams.values) {
                scanTypes(pp.type.sourceType, true)
            }
        }
    }

    private fun buildUnion(baseClass: KClass<*>): GJavaUnionType<CTX> {
        val ann = baseClass.findAnnotation<GqlUnion>()

        val name: String = ann?.name.trimToNull() ?: resolveName(baseClass)
        check(validGraphQLName(name)) { "Name of $baseClass was determined to be $name, but that is not a valid GraphQL name" }

        val result = GUnionType(name)
        addBaseType(result)


        val impls: Array<KClass<*>>
        if (ann != null && ann.implementedBy.isNotEmpty()) {
            impls = ann.implementedBy
        } else {
            impls = findImplementations(baseClass)
        }

        val javaType = GJavaUnionType<CTX>(baseClass.nullableType(), result, impls)
        add(javaType)

        for (klass in impls)
            scanTypes(klass.nullableType(), false)

        latentChecks += {
            val gqlImpls = HashSet<GObjectType>()
            for (impl in impls) {
                val gqlType = schema.getJavaType(impl.nullableType())?.gqlType
                if (gqlType is GObjectType) {
                    gqlImpls.add(gqlType)
                } else {
                    throw SchemaException("A member of " + result.gqlTypeString + " (" + impl + ") does not have an object type, but is " + gqlType?.gqlTypeString)
                }
            }
            result.setMembers(gqlImpls)
        }

        return javaType
    }

    private fun findImplementations(baseClass: KClass<*>): Array<KClass<*>> {
        val scanResult = scanResult

        val toCheck = ArrayList<String>()
        if (baseClass.java.isInterface) {
            toCheck.addAll(scanResult.getNamesOfClassesImplementing(baseClass.java))
            toCheck.addAll(scanResult.getNamesOfSubinterfacesOf(baseClass.java))
        } else {
            toCheck.addAll(scanResult.getNamesOfSubclassesOf(baseClass.java))
        }

        val classList = HashSet(scanResult.classNamesToClassRefs(toCheck))

        val posibs = scanResult.getNamesOfClassesWithAnnotation(GqlObject::class.java)
        for (posib in scanResult.classNamesToClassRefs(posibs)) {
            val ann = posib.getAnnotation(GqlObject::class.java)
            if (ann != null && ann.implements.isNotEmpty()) {
                for (i in ann.implements)
                    if (i == baseClass)
                        classList.add(posib)
            }
        }

        return classList.map { it.kotlin }.toTypedArray()
    }



    private fun buildInputObject(baseClass: KClass<*>): GJavaInputObjectType<CTX> {
        val ann = baseClass.findAnnotation<GqlInput>()

        val name: String = ann?.name.trimToNull() ?: resolveName(baseClass)
        if (!validGraphQLName(name))
            throw IllegalStateException("Name of $baseClass was determined to be $name, but that is not a valid GraphQL name")

        val reflected = try {
            reflectInputObject(baseClass)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to reflect on $baseClass", e)
        }

        val gqlFields = LinkedHashMap<String, GField>()

        val gtype = GInputObjType(name, gqlFields)
        addBaseType(gtype)
        val result = GJavaInputObjectType<CTX>(baseClass.nullableType(), gtype, reflected)
        add(result)

        for (propInfo in reflected.props)
            scanTypes(propInfo.type.sourceType, true)

        latentChecks.add {
            for (propInfo in reflected.props) {
                val type = schema.getJavaType(propInfo.type.sourceType)?.gqlType ?: throw IllegalStateException("Didn't find it")

                gqlFields[propInfo.name] = GField(propInfo.name, type, emptyMap())
            }
        }

        return result
    }

    private fun buildObject(baseClass: KClass<*>): GJavaObjectType<CTX> {
        val ann = baseClass.findAnnotation<GqlObject>()

        val name: String = ann?.name.trimToNull() ?: resolveName(baseClass)
        if (!validGraphQLName(name))
            throw IllegalStateException("Name of $baseClass was determined to be $name, but that is not a valid GraphQL name")

        val javaFields = scanOutputMethods(baseClass)

        val gqlFields = LinkedHashMap<String, GField>()
        val gtype = GObjectType(name, gqlFields)
        addBaseType(gtype)

        val result = GJavaObjectType(baseClass.nullableType(), gtype, javaFields)
        add(result)

        continueScanningFields(baseClass, javaFields)

        latentChecks.add({ fillInFields(javaFields, gqlFields) })

        return result
    }

    private fun fillInFields(javaFields: Map<String, FieldGetter<CTX>>, gqlFields: MutableMap<String, GField>) {
        for ((key, omi) in javaFields) {
            val type = schema.getJavaType(omi.publicType.sourceType)?.gqlType ?: throw IllegalStateException("Didn't find it")

            val arguments = LinkedHashMap<String, GArgument>()
            for ((_, p) in omi.publicParams) {
                val argType = schema.getJavaType(p.type.sourceType) ?: throw IllegalStateException("Couldn't find it")

                val arg = GArgument(p.name, argType.gqlType, p.defaultValue)
                arguments[arg.name] = arg
            }

            gqlFields[key] = GField(key, type, arguments)
        }
    }

    private fun buildEnum(baseClass: KClass<*>): GJavaType<CTX> {
        val ann = baseClass.findAnnotation<GqlEnum>()

        val name: String = ann?.name.trimToNull() ?: resolveName(baseClass)
        if (!validGraphQLName(name))
            throw IllegalStateException("Name of $baseClass was determined to be $name, but that is not a valid GraphQL name")

        if (Enum::class.isSuperclassOf(baseClass)) {
            if (baseClass == Enum::class) {
                // you sneaky user!
                throw IllegalStateException("Can't use the abstract java.lang.Enum class")
            }

            return buildStandardEnum(name, baseClass)
        } else {
            throw UnsupportedOperationException("Custom enum-like classes are not supported yet")
        }
    }

    protected fun buildStandardEnum(name: String, enumClass: KClass<*>): GJavaStandardEnum<CTX> {
        val values = getEnumValues(enumClass)

        val valuesByName = values.associateBy { it.name }

        val enumType = GEnumType(name, LinkedHashSet(valuesByName.keys))
        addBaseType(enumType)

        val result = GJavaStandardEnum<CTX>(enumClass.nullableType(), enumType, valuesByName)
        add(result)
        return result
    }

    protected fun buildScalar(baseClass: KClass<*>): GJavaType<CTX> {
        throw UnsupportedOperationException("Custom scalars are not supported yet")
    }

    protected fun determineKind(baseClass: KClass<*>, isInput: Boolean): TypeKind {
        if (baseClass.java.isAnnotation || baseClass.java.isPrimitive)
            throw IllegalStateException("Can't process annotations or primitive types")

        val posibs = HashSet<TypeKind>()

        for (kind in TypeKind.values())
            if (kind.explicitAnnotation != null && baseClass.annotations.firstOrNull { kind.explicitAnnotation.isInstance(it) } != null)
                posibs.add(kind)

        if (posibs.size > 1)
            throw IllegalArgumentException("$baseClass is marked with too many kinds: $posibs")

        if (posibs.size == 1)
            return posibs.first()

        if (Enum::class.isSuperclassOf(baseClass))
            return ENUM

        if (isInput)
            return INPUT_OBJECT

        val outputMethods = scanOutputMethods(baseClass)

        return if (outputMethods.isEmpty()) {
            UNION
        } else {
            OBJECT
        }
    }

    companion object {
        val logger = getLogger<AutoBuilder<*,*>>()

        internal fun resolveName(klass: KClass<*>): String {
            val simpleName = klass.simpleName ?: throw IllegalArgumentException("Can't use anonymous classes")

            if (klass.java.isMemberClass) {
                return resolveName(klass.java.declaringClass.kotlin) + simpleName
            } else {
                return simpleName
            }
        }

        protected fun getEnumValues(enumClass: KClass<*>): Array<out Enum<*>> {
            @Suppress("UNCHECKED_CAST")
            return enumClass.java.getMethod("values").invoke(null) as Array<out Enum<*>>
        }

        fun <SCHEMA: Any, CTX: Any> build(schema: KClass<SCHEMA>, contextType: KClass<CTX>): Schema<SCHEMA, CTX> {
            return AutoBuilder(schema, contextType).build()
        }
    }
}
