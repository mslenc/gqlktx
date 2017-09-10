package com.xs0.gqlktx.dom

import com.xs0.gqlktx.parser.Token

class VariableDefinition(name: Token<String>,
                         val type: TypeDef,
                         val defaultValue: Value?) {
    val name: String = name.value
}
