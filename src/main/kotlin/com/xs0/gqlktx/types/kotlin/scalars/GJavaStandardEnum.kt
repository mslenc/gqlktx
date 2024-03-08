package com.xs0.gqlktx.types.kotlin.scalars

import com.xs0.gqlktx.ScalarCoercion
import com.xs0.gqlktx.ValidationException
import com.xs0.gqlktx.codegen.*
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.dom.ValueEnum
import com.xs0.gqlktx.dom.ValueString
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.schema.builder.ResolvedName
import com.xs0.gqlktx.schema.intro.GqlIntroEnumValue
import com.xs0.gqlktx.types.gql.GEnumType
import com.xs0.gqlktx.types.kotlin.GJavaScalarLikeType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSuperclassOf

data class GJavaStandardEnum<CTX: Any>(override val name: ResolvedName, override val type: KType, override val gqlType: GEnumType, internal val values: List<GqlIntroEnumValue>) : GJavaScalarLikeType<CTX>() {
    private val valuesByPublicName = values.associateBy { it.name }
    private val valuesByCodeName = values.associateBy { it.rawValue.name }

    init {
        checkGqlType()

        if (!Enum::class.isSuperclassOf(type.classifier as? KClass<*> ?: String::class))
            throw IllegalArgumentException("Not an enum type: $type")

        if (valuesByPublicName.size != valuesByCodeName.size)
            throw IllegalStateException("A public name repeats in $type")
    }

    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): Enum<*> {
        // So when we parse GQL, we produce ValueEnum.. but when we get variables from JSON, it becomes just a regular string..
        // Luckily, we can tell the two apart by the presence of the parser token ..
        val s = when (value) {
            is ValueEnum -> value.value
            is ValueString -> if (value.token == null) value.value else throw ValidationException("The value should be an enum keyword, not a string")
            else -> throw ValidationException("Expected a string for enum, but got something else")
        }

        return valuesByPublicName[s]?.rawValue ?: throw ValidationException("Unrecognized value " + s + " for enum " + gqlType.gqlTypeString)
    }

    override fun toJson(result: Any, coercion: ScalarCoercion): Any {
        return when(coercion) {
            ScalarCoercion.STRICT_JSON,
            ScalarCoercion.JSON,
            ScalarCoercion.SPREADSHEET ->
                valuesByCodeName.getValue((result as Enum<*>).name).name

            ScalarCoercion.NONE ->
                result
        }
    }

    override fun inputParseInfo(gen: CodeGen<*, CTX>): InputParseCodeGenInfo {
        val packageName = type.packageName()!!.excludeSystemPackages(gen)
        val funName = "parse" + name.gqlName

        return InputParseCodeGenInfo(
            kind = InputParseKind.ENUM,
            funName = name.gqlName,
            funReturnType = name.codeGenType,
            funCreateType = name.codeGenTypeNN,
            outPackageName = packageName,
            exprTemplate = funName + "(VALUE, variables)",
            importsForGen = name.imports,
            importsForUse = setOf(packageName to funName),
        )
    }

    override fun outputExportInfo(gen: CodeGen<*, CTX>): OutputExportCodeGenInfo {
        val packageName = type.packageName()!!.excludeSystemPackages(gen)
        val funName = "export" + name.gqlName

        return OutputExportCodeGenInfo(
            OutputExportKind.ENUM,
            name.gqlName,
            funReturnType = "Any?",
            funReturnTypeNN = "Any",
            funIsSuspending = false,
            packageName,
            funName + "(VALUE, coercion)",
            name.imports,
            setOf(packageName to funName)
        )
    }
}
