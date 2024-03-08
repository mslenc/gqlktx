package com.xs0.gqlktx.types.kotlin

import com.xs0.gqlktx.*
import com.xs0.gqlktx.codegen.*
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.dom.ValueObject
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.schema.builder.ResolvedName
import com.xs0.gqlktx.types.gql.GInputObjType
import com.xs0.gqlktx.utils.Maybe
import kotlin.reflect.KType

data class GJavaInputObjectType<CTX: Any>(override val name: ResolvedName, override val type: KType, override val gqlType: GInputObjType, internal val info: ReflectedInput) : GJavaType<CTX>() {
    override fun checkUsage(isInput: Boolean) {
        if (!isInput)
            throw IllegalStateException("$type is used as both input and output")
    }

    override fun baselineType(): GJavaType<CTX> {
        return this
    }

    override fun inputElementType(): GJavaType<CTX>? {
        return null
    }

    override fun hasSubSelections(): Boolean {
        return false
    }

    override fun getFromJson(value: Value, inputVarParser: InputVarParser<CTX>): Any {
        val inputObj = value as? ValueObject ?: throw ValidationException("Expected a JSON object, but got something else")

        if (!info.propIndex.keys.containsAll(inputObj.elements.keys)) {
            val unknown = inputObj.elements.keys - info.propIndex.keys
            throw IllegalArgumentException("Field(s) " + unknown.joinToString() + " not recognized")
        }

        val consParams = arrayOfNulls<Any?>(info.props.size)

        for (i in info.props.indices) {
            val prop = info.props[i]

            val inInput = prop.name in inputObj.elements.keys

            val propValue = if (inInput) inputObj.elements.getValue(prop.name) else prop.defaultValue

            if (propValue == null) {
                if (prop.propMode == PropMode.REQUIRED)
                    throw IllegalArgumentException("Missing value for ${prop.name}")

                continue
            }

            if (prop.propMode == PropMode.MAYBE) {
                val imported = inputVarParser.parseVar(propValue, prop.type.inner!!.sourceType)
                consParams[i] = Maybe(imported)
            } else {
                val imported = inputVarParser.parseVar(propValue, prop.type.sourceType)
                consParams[i] = imported
            }
        }

        return info.constructor.call(*consParams)
    }

    override fun inputParseInfo(gen: CodeGen<*, CTX>): InputParseCodeGenInfo {
        val packageName = type.packageName() ?: "Missing package for $type"
        return InputParseCodeGenInfo(
            kind = InputParseKind.INPUT_OBJECT,
            funName = gqlType.gqlTypeString,
            funReturnType = name.codeGenType,
            funCreateType = name.codeGenTypeNN,
            outPackageName = packageName,
            exprTemplate = "parse" + gqlType.gqlTypeString + "(VALUE, variables)",
            importsForGen = name.imports,
            importsForUse = setOf(packageName to "parse" + gqlType.gqlTypeString),
        )
    }

    override fun outputExportInfo(gen: CodeGen<*, CTX>): OutputExportCodeGenInfo {
        throw IllegalStateException("outputExportInfo() called on an input object type")
    }

    override fun anythingSuspends(gen: CodeGen<*, CTX>): Boolean {
        return info.props.any { gen.schema.getJavaType(it.type.sourceType).suspendingOutput }
    }
}
