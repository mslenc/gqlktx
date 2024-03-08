package com.xs0.gqlktx.codegen

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.xs0.gqlktx.types.kotlin.GJavaType
import kotlin.reflect.KClass
import kotlin.reflect.KType

internal val INTRO_JSON_MAPPER = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerModule(KotlinModule.Builder().build())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.INDENT_OUTPUT, true)
    setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
}

fun String.excludeSystemPackages(gen: CodeGen<*, *>): String {
    return when {
        isSystemPackage() -> gen.statePackage
        else -> this
    }
}

fun String.isSystemPackage(): Boolean {
    return when {
        startsWith("java.") -> true
        startsWith("javax.") -> true
        startsWith("kotlin.") -> true
        startsWith("kotlinx.") -> true
        else -> false
    }
}

fun KClass<*>.packageName(): String? {
    return java.getPackage()?.name
}

fun GJavaType<*>.packageName(): String? {
    return type.packageName()
}

fun KType.packageName(): String? {
    val cl = classifier

    when (cl) {
        is KClass<*> -> return cl.packageName()
        else -> throw IllegalStateException("Invalid type of type classifier.")
    }
}

fun KType.simpleClassName(): String {
    val cl = classifier

    when (cl) {
        is KClass<*> -> return cl.simpleName ?: throw IllegalStateException("Missing simpleName for $this")
        else -> throw IllegalStateException("Invalid type of type classifier.")
    }
}

fun <CTX: Any> GJavaType<CTX>.deepestInputElementType(): GJavaType<CTX> {
    return inputElementType()?.deepestInputElementType() ?: this
}