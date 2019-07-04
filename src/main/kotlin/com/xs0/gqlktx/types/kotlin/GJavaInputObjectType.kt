package com.xs0.gqlktx.types.kotlin

import com.xs0.gqlktx.*
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.dom.ValueObject
import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GInputObjType
import com.xs0.gqlktx.utils.Maybe
import kotlin.reflect.KType

class GJavaInputObjectType<CTX>(type: KType, gqlType: GInputObjType, private val info: ReflectedInput) : GJavaType<CTX>(type, gqlType) {
    override fun checkUsage(isInput: Boolean) {
        if (!isInput)
            throw IllegalStateException("$type is used as both input and output")
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
}
