package com.xs0.gqlktx.dom

import com.xs0.gqlktx.parser.Token

sealed class TypeDef

class NamedType(name: Token<String>) : TypeDef() {
    val name = name.value
}

sealed class WrapperType(val inner: TypeDef) : TypeDef()

class ListType(inner: TypeDef) : WrapperType(inner)

class NotNullType(inner: TypeDef) : WrapperType(inner)
