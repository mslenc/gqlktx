package com.xs0.gqlktx.parser

import com.xs0.gqlktx.ParseException
import com.xs0.gqlktx.dom.*

import java.util.*

import com.xs0.gqlktx.parser.Token.Type.*

class GraphQLParser internal constructor(private val tokens: GraphQLTokenizer) {

    internal fun parseQueryDoc(): Document {
        val defs = ArrayList<Definition>()

        while (true) {
            if (tokens.peek<Any>().type === EOF)
                return Document(defs)

            when (tokens.peek<Any>().rawValue) {
                "fragment" -> defs.add(parseFragmentDefinition())

                "query" -> defs.add(parseFullOperationDefinition(OpType.QUERY))

                "mutation" -> defs.add(parseFullOperationDefinition(OpType.MUTATION))

                "subscription" -> defs.add(parseFullOperationDefinition(OpType.SUBSCRIPTION))

                "{" -> defs.add(OperationDefinition(tokens.peek<Any>(), parseSelectionSet()))

                else -> throw ParseException("Expected one of [ fragment, query, mutation, subscription, '{' ] ", tokens.peek<Any>())
            }
        }
    }

    internal fun parseFullOperationDefinition(opType: OpType): Definition {
        expect<Any>(NAME) // "query"/"mutation"/"subscription"..

        val name = maybe<String>(NAME)

        val varDefs = parseVariableDefsOpt()
        val directives = parseDirectives()
        val selections = parseSelectionSet()

        return OperationDefinition(opType, name, varDefs, directives, selections)
    }

    internal fun parseVariableDefsOpt(): Map<String, VariableDefinition> {
        if (!consume(LPAREN))
            return emptyMap()
        forbidden(RPAREN, "Expected one or more variable definitions")

        val res = LinkedHashMap<String, VariableDefinition>()

        while (!consume(RPAREN))
            parseVariableDef(res)

        return res
    }

    internal fun parseVariableDef(out: MutableMap<String, VariableDefinition>) {
        expect<Any>(DOLLAR)
        val name = expect<String>(NAME)
        if (out.containsKey(name.value))
            throw ParseException("Name " + name.value + " has already been defined", name)
        expect<Any>(COLON)
        val typeDef = parseType()
        val defaultVal: ValueOrNull?
        if (consume(EQ)) {
            defaultVal = parseValue(false) as ValueOrNull
        } else {
            defaultVal = null
        }
        out[name.value] = VariableDefinition(name, typeDef, defaultVal)
    }

    internal fun parseType(): TypeDef {
        val type: TypeDef

        if (consume(LBRACK)) {
            type = ListType(parseType())
            expect<Any>(RBRACK)
        } else {
            type = NamedType(expect(NAME))
        }

        return if (consume(EXCL)) {
            NotNullType(type)
        } else {
            type
        }
    }

    internal fun parseSelectionSet(): List<Selection> {
        expect<Any>(LCURLY)
        forbidden(RCURLY, "Expected one or more selections")

        val res = ArrayList<Selection>()

        while (!consume(RCURLY))
            res.add(parseSelection())

        return res
    }

    internal fun parseSelection(): Selection {
        if (consume(SPREAD)) {
            if (tokens.peek<Any>().type === NAME) {
                val fragmentName = expect<String>(NAME)
                if (fragmentName.`is`("on")) {
                    val typeCondition = expect<String>(NAME)
                    val directives = parseDirectives()
                    val selections = parseSelectionSet()
                    return SelectionInlineFragment(NamedType(typeCondition), directives, selections)
                } else {
                    val directives = parseDirectives()
                    return SelectionFragmentSpread(fragmentName, directives)
                }
            } else {
                val directives = parseDirectives()
                val selections = parseSelectionSet()
                return SelectionInlineFragment(null, directives, selections)
            }
        } else {
            var name = expect<String>(NAME)
            val alias: Token<String>?
            if (consume(COLON)) {
                alias = name
                name = expect(NAME)
            } else {
                alias = null
            }

            val arguments = parseArgumentsOpt()
            val directives = parseDirectives()
            val selectionSet: List<Selection>
            if (tokens.peek<Any>().type === LCURLY) {
                selectionSet = parseSelectionSet()
            } else {
                selectionSet = emptyList()
            }

            return SelectionField(alias, name, arguments, directives, selectionSet)
        }
    }

    internal fun parseFragmentDefinition(): FragmentDefinition {
        expectName("fragment")

        val name = expect<String>(NAME)
        expectName("on")
        val type = expect<String>(NAME)

        val directives = parseDirectives()

        val selections = parseSelectionSet()

        return FragmentDefinition(name, NamedType(type), directives, selections)
    }

    internal fun parseDirectives(): List<Directive> {
        if (tokens.peek<Any>().type !== AT)
            return emptyList()

        val res = ArrayList<Directive>()
        while (tokens.peek<Any>().type === AT) {
            res.add(parseDirective())
        }
        return res
    }

    internal fun parseDirective(): Directive {
        expect<Any>(AT)
        val name = expect<String>(NAME)
        val args = parseArgumentsOpt()
        return Directive(name, args)
    }

    internal fun parseArgumentsOpt(): Map<String, ValueOrVar> {
        if (!consume(LPAREN))
            return emptyMap()
        forbidden(RPAREN, "Expected one or more arguments")

        val res = LinkedHashMap<String, ValueOrVar>()

        while (!consume(RPAREN)) {
            val name = expect<Any>(NAME)
            expect<Any>(COLON)
            val value = parseValue(true)

            res[name.value as String] = value
        }

        return res
    }

    inline fun <reified T: Any> cast(token: Token<Any>): Token<T> {
        if (token.value is T) {
            @Suppress("UNCHECKED_CAST")
            return token as Token<T>
        } else {
            throw IllegalStateException("Token should be a ${T::class}, but is not")
        }
    }

    internal fun parseValue(allowVars: Boolean): ValueOrVar {
        val token = tokens.next<Any>()
        when (token.type) {
            NUMBER -> return ValueNumber(token.value.toString(), cast(token))
            STRING -> return ValueString(token.value.toString(), cast(token))
            NAME -> when (token.rawValue) {
                "null" -> return ValueNull(cast(token))
                "true" -> return ValueBool(true, cast(token))
                "false" -> return ValueBool(false, cast(token))
                else -> return ValueEnum(token.value.toString(), cast(token))
            }
            LBRACK -> return ValueList(parseRestOfList(allowVars))
            LCURLY -> return ValueObject(parseRestOfObject(allowVars))
            DOLLAR -> return if (allowVars) {
                Variable(expect(NAME))
            } else {
                throw ParseException("Variables not allowed here", token)
            }

            else -> throw ParseException("Expected a value but found $token instead", token)
        }
    }

    internal fun parseRestOfList(allowVars: Boolean): List<ValueOrVar> {
        if (consume(RBRACK))
            return emptyList()

        val res = ArrayList<ValueOrVar>()
        while (!consume(RBRACK))
            res.add(parseValue(allowVars))
        return res
    }

    internal fun parseRestOfObject(allowVars: Boolean): Map<String, ValueOrVar> {
        if (consume(RCURLY))
            return emptyMap()

        val res = LinkedHashMap<String, ValueOrVar>()
        while (!consume(RCURLY)) {
            val name = expect<Any>(NAME)
            expect<Any>(COLON)
            val value = parseValue(allowVars)

            res[name.value as String] = value
        }

        return res
    }

    private inline fun <reified T: Any> expect(type: Token.Type): Token<T> {
        val token = tokens.next<T>()
        if (token.type == type)
            return token
        throw ParseException("Expected $type instead of $token", token)
    }

    private fun expectName(name: String): Token<String> {
        val token = expect<String>(NAME)
        if (!token.`is`(name))
            throw ParseException("Expected $name instead of $token", token)
        return token
    }

    private fun consume(type: Token.Type): Boolean {
        return maybe<Any>(type) != null
    }

    private fun <T: Any> maybe(type: Token.Type): Token<T>? {
        return if (tokens.peek<Any>().type === type) {
            tokens.next<Any>() as Token<T>
        } else {
            null
        }
    }

    private fun forbidden(type: Token.Type, message: String) {
        val token = maybe<Any>(type)
        if (token != null)
            throw ParseException(message, token)
    }

    companion object {
        fun parseQueryDoc(src: String): Document {
            return GraphQLParser(GraphQLTokenizer(CharStream(src))).parseQueryDoc()
        }

        fun parseValue(src: String): Value {
            return GraphQLParser(GraphQLTokenizer(CharStream(src))).parseValue(false) as Value
        }
    }
}
