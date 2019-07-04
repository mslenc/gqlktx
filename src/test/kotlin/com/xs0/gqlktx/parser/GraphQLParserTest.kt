package com.xs0.gqlktx.parser

import com.xs0.gqlktx.ParseException
import com.xs0.gqlktx.dom.*
import org.junit.Test

import org.junit.Assert.*
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

class GraphQLParserTest {

    @Test
    fun testParsingTypes() {
        checkType(parseType("SomeType"), "SomeType")
        checkType(parseType("NotNull!"), "NotNull", NotNullType::class)
        checkType(parseType("[ListEl]"), "ListEl", ListType::class)
        checkType(parseType("[[[Abc!]]!]!"), "Abc", NotNullType::class, ListType::class, NotNullType::class, ListType::class, ListType::class, NotNullType::class)
    }

    @Test
    fun testParseValueConst() {
        // we won't obsess too much with different values here, as they're already tested in the tokenizer..

        checkScalarValue("123", parseConst("123"), ValueNumber::class)
        checkScalarValue("234", parseConst("+234"), ValueNumber::class)
        checkScalarValue("-50", parseConst("-50"), ValueNumber::class)

        checkScalarValue("123.0", parseConst("123.0"), ValueNumber::class)
        checkScalarValue("158.553E-3", parseConst("158.553E-3"), ValueNumber::class)
        checkScalarValue("-55E6", parseConst("-55E6"), ValueNumber::class)

        checkScalarValue("a b c???", parseConst("\"a b c???\""), ValueString::class)

        checkScalarValue(true, parseConst("true"), ValueBool::class)
        checkScalarValue(false, parseConst("false"), ValueBool::class)

        assertTrue(parseConst("null") is ValueNull)

        checkScalarValue("BUBU", parseConst("BUBU"), ValueEnum::class)

        val complex = parseConst("[ 666, { foo: bar bar :false, baz: [ 5, 4, 3 ] } true ]")
        assertTrue(complex is ValueList)

        val outerList = (complex as ValueList).elements
        assertEquals(3, outerList.size.toLong())
        checkScalarValue("666", outerList[0], ValueNumber::class)
        assertTrue(outerList[1] is ValueObject)
        checkScalarValue(true, outerList[2], ValueBool::class)

        val obj = outerList[1] as ValueObject
        val objVals = obj.elements
        assertEquals(3, objVals.size.toLong())
        checkScalarValue("bar", objVals["foo"]!!, ValueEnum::class)
        checkScalarValue(false, objVals["bar"]!!, ValueBool::class)
        assertTrue(objVals["baz"] is ValueList)

        val innerList = (objVals["baz"] as ValueList).elements
        assertEquals(3, innerList.size.toLong())
        checkScalarValue("5", innerList[0], ValueNumber::class)
        checkScalarValue("4", innerList[1], ValueNumber::class)
        checkScalarValue("3", innerList[2], ValueNumber::class)

        val fails = arrayOf("\$name", "[ 1, 2, \$foo, 4 ]", "{ a:b, c:\$d }")
        for (s in fails) {
            try {
                parseConst(s)
                fail("Should've failed")
            } catch (e: ParseException) {
                assertEquals("Variables not allowed here", e.message)
            }

        }
    }

    @Test
    @Throws(ParseException::class)
    fun testParseValueWithVariables() {
        checkScalarValue("123", parseValue("123"), ValueNumber::class)
        checkScalarValue("-50", parseValue("-50"), ValueNumber::class)

        checkScalarValue("123.0", parseValue("123.0"), ValueNumber::class)
        checkScalarValue("158.553E-3", parseValue("158.553E-3"), ValueNumber::class)
        checkScalarValue("-55E6", parseValue("-55E6"), ValueNumber::class)

        checkScalarValue("a b c???", parseValue("\"a b c???\""), ValueString::class)

        checkScalarValue(true, parseValue("true"), ValueBool::class)
        checkScalarValue(false, parseValue("false"), ValueBool::class)

        checkScalarValue("BUBU", parseValue("BUBU"), ValueEnum::class)

        val complex = parseValue("[ 666, { foo: bar bar : \$var, baz: [ 5, \$boo, 3 ] } true ]")
        assertTrue(complex is ValueList)

        val outerList = (complex as ValueList).elements
        assertEquals(3, outerList.size.toLong())
        checkScalarValue("666", outerList[0], ValueNumber::class)
        assertTrue(outerList[1] is ValueObject)
        checkScalarValue(true, outerList[2], ValueBool::class)

        val obj = outerList[1] as ValueObject
        val objVals = obj.elements
        assertEquals(3, objVals.size.toLong())
        checkScalarValue("bar", objVals["foo"]!!, ValueEnum::class)
        assertEquals("var", (objVals["bar"] as Variable).name)
        assertTrue(objVals["baz"] is ValueList)

        val innerList = (objVals["baz"] as ValueList).elements
        assertEquals(3, innerList.size.toLong())
        checkScalarValue("5", innerList[0], ValueNumber::class)
        assertEquals("boo", (innerList[1] as Variable).name)
        checkScalarValue("3", innerList[2], ValueNumber::class)

        val constFails = arrayOf("\$name", "[ 1, 2, \$foo, 4 ]", "{ a:b, c:\$d }")
        for (s in constFails) {
            parseValue(s) // shouldn't throw, unlike in const case
        }
    }

    companion object {
        internal fun parser(string: String): GraphQLParser {
            return GraphQLParser(GraphQLTokenizer(CharStream(string)))
        }

        @Throws(ParseException::class)
        internal fun parseType(string: String): TypeDef {
            return parser(string).parseType()
        }

        @Throws(ParseException::class)
        internal fun parseValue(string: String): ValueOrVar {
            return parser(string).parseValue(true)
        }

        @Throws(ParseException::class)
        internal fun parseConst(string: String): ValueOrNull {
            val res = parser(string).parseValue(false)
            assertTrue(res is ValueOrNull)
            return res as ValueOrNull
        }

        @SafeVarargs
        internal fun checkType(type: TypeDef, baseName: String, vararg wrappers: KClass<out WrapperType>) {
            var type = type
            assertNotNull(type)
            for (wrapper in wrappers) {
                assertTrue(wrapper.isInstance(type))
                type = (type as WrapperType).inner
            }
            assertTrue(type is NamedType)
            assertEquals(baseName, (type as NamedType).name)
        }

        internal fun <T: Any> checkScalarValue(expect: T?, parsed: ValueOrVar, type: KClass<out ValueScalar<T>>) {
            assert(type.isInstance(parsed))
            assertEquals(expect, type.cast(parsed).value)
        }
    }
}