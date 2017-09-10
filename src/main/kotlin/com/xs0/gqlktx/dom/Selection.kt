package com.xs0.gqlktx.dom

import com.xs0.gqlktx.parser.Token

typealias SelectionSet = List<Selection>

abstract class Selection internal constructor(val directives: List<Directive>) {
    fun findDirective(name: String): Directive? =
            directives.firstOrNull { it.name == name }
}

class SelectionInlineFragment(
        val typeConditionOpt: NamedType?,
        directives: List<Directive>,
        val selectionSet: SelectionSet) : Selection(directives)

class SelectionFragmentSpread(
        private val fragmentName: Token<String>,
        directives: List<Directive>) : Selection(directives) {

    fun getFragmentName(): String {
        return fragmentName.value
    }
}

class SelectionField(alias: Token<String>?,
                     fieldName: Token<String>,
                     val arguments: Map<String, ValueOrVar>,
                     directives: List<Directive>,
                     val subSelections: SelectionSet) : Selection(directives) {

    val alias: String? = alias?.value
    val fieldName: String = fieldName.value
    val responseKey: String
        get() = alias ?: fieldName
}
