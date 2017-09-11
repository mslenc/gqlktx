package com.xs0.gqlktx.types.kotlin

import com.xs0.gqlktx.*
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GInputObjType
import io.vertx.core.json.JsonObject
import kotlin.reflect.KType

class GJavaInputObjectType<CTX>(type: KType, gqlType: GInputObjType, private val info: ReflectedInput) : GJavaType<CTX>(type, gqlType) {
    override fun checkUsage(isInput: Boolean) {
        if (!isInput)
            throw IllegalStateException(type.toString() + " is used as both input and output")
    }

    override fun getFromJson(value: Any, inputVarParser: InputVarParser<CTX>): Any {
        val inputJson: JsonObject = value as? JsonObject ?: throw ValidationException("Expected a JSON object, but got something else")

        // stage 1: convert input json into internal types, using defaults where they exist

        if (!info.propTypes.keys.containsAll(inputJson.fieldNames())) {
            val unknown = inputJson.fieldNames() - info.propTypes.keys
            throw IllegalArgumentException("Field(s) " + unknown.joinToString() + " not recognized")
        }

        val values = HashMap<String, Any?>()

        for ((name, propInfo) in info.propTypes) {
            val inInput = name in inputJson.fieldNames()
            val jsonVal = if (inInput) inputJson.getValue(name) else propInfo.defaultValue
            if (jsonVal == null && propInfo.alwaysRequired)
                throw IllegalArgumentException("Missing value for $name")
            if (jsonVal != null || inInput) {
                if (jsonVal != null) {
                    values.put(name, inputVarParser.getCoercedVar(jsonVal, propInfo.type.sourceType))
                } else {
                    values.put(name, null)
                }
            }
        }

        // stage 2: find a mode that matches the input

        var matchingMode: ReflectedInputMode? = null
        val consParams = ArrayList<Any?>()
        val setters = ArrayList<Pair<FieldSetter, Any?>>()

        nextMode@
        for (mode in info.modes) {
            consParams.clear()
            setters.clear()

            for (prop in mode.props) {
                val isPresent = prop.name in values.keys
                val propVal = if (isPresent) values[prop.name] else null

                when (prop.propMode) {
                    PropMode.REQUIRED -> {
                        if (propVal == null)
                            continue@nextMode
                    }
                    PropMode.FORBIDDEN -> {
                        if (isPresent)
                            continue@nextMode
                    }
                    PropMode.OPTIONAL -> {
                        // ok..
                    }
                }
                if (prop.setter == null) {
                    consParams.add(propVal)
                } else {
                    if (isPresent)
                        setters.add(Pair(prop.setter, propVal))
                }
            }

            matchingMode = mode
            break
        }

        if (matchingMode == null)
            throw IllegalArgumentException(formatModeError(values))

        // stage 3: construct the object and set properties

        val result: Any = matchingMode.factory.call(*consParams.toArray())
        for ((setter, propVal) in setters)
            setter.invoke(result, propVal)

        return result
    }

    private fun formatModeError(values: Map<String, Any?>): String {
        if (info.modes.size == 1) {
            // the only way to get here is miss a required value, I think?
            val missing: Set<String> =
                    info.propTypes.
                        filter{ (key, info) -> info.alwaysRequired }.
                        map{ (key, info) -> key }.
                        toSet()

            if (missing.size == 1)
                return "Missing required parameter ${missing.first()}"

            return "Missing these required parameters: " + missing.joinToString()
        } else {
            val sb = StringBuilder("None of these modes matched input:")
            info.modes.forEach {
                sb.append("-")
                formatMode(it, sb)
                sb.appendln()
            }
            return sb.toString()
        }
    }

    private fun formatMode(it: ReflectedInputMode, sb: StringBuilder) {
        it.props.groupBy { it.propMode }.forEach { (mode, props) ->
            sb.append(" ").append(mode)
            props.joinTo(sb, prefix = "(", postfix = ")") { it.name }
        }
    }
}
