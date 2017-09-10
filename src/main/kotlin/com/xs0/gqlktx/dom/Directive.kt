package com.xs0.gqlktx.dom

import com.xs0.gqlktx.parser.Token

class Directive(name: Token<String>, val args: Map<String, ValueOrVar>) {
    val name: String = name.value
}
