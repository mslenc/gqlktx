package com.xs0.gqlktx.exec

import com.xs0.gqlktx.dom.*
import com.xs0.gqlktx.schema.Schema
import com.xs0.gqlktx.types.kotlin.GJavaType

import kotlin.reflect.KType

class InputVarParser<CTX>(
        private val inputVariables: Map<String, ValueOrNull>,
        private val schema: Schema<*, CTX>) {

    fun parseVar(valueOrVar: ValueOrVar, type: KType): Any? {
        return parseVar(valueOrVar, schema.getJavaType(type))
    }

    fun parseVar(valueOrVar: ValueOrVar, javaType: GJavaType<CTX>): Any? {
        return when (valueOrVar) {
            is ValueNull -> {
                if (javaType.isNullAllowed()) {
                    null
                } else {
                    throw IllegalArgumentException("Forbidden null")
                }
            }

            is Variable -> {
                val repl = inputVariables[valueOrVar.name] ?: (
                    if (javaType.isNullAllowed()) {
                        return null
                    } else {
                        throw IllegalArgumentException("Variable $valueOrVar not defined")
                    }
                )

                return parseVar(repl, javaType)
            }

            is Value -> {
                javaType.getFromJson(valueOrVar, this)
            }
        }
    }
}
