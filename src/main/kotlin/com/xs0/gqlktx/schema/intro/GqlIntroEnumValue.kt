package com.xs0.gqlktx.schema.intro

import com.xs0.gqlktx.GqlObject
import kotlin.reflect.KClass

@GqlObject("__EnumValue")
data class GqlIntroEnumValue(
    val name: String,
    val description: String?,
    val isDeprecated: Boolean,
    val deprecationReason: String?,
    internal val rawValue: Enum<*>,
    internal val sourceClass: KClass<*>,
)