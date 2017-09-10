package com.xs0.gqlktx.dom

import com.xs0.gqlktx.parser.Token

abstract class Definition

class OperationDefinition(
        val type: OpType,
        nameToken: Token<String>?,
        val varDefs: Map<String, VariableDefinition>,
        val directives: List<Directive>,
        val selectionSet: SelectionSet) : Definition() {

    val name: String? = nameToken?.value

    constructor(start: Token<*>, selectionSet: SelectionSet)
            : this(OpType.QUERY, null, // no name
            emptyMap(), // no variables
            emptyList(), // no directives
            selectionSet)

}

class FragmentDefinition(
        nameToken: Token<String>,
        val type: NamedType,
        val directives: List<Directive>,
        val selectionSet: SelectionSet) : Definition() {

    val name = nameToken?.value
}
