package com.xs0.gqlktx.parser

import com.xs0.gqlktx.ParseException
import com.xs0.gqlktx.dom.*
import org.junit.Test

import org.junit.Assert.*

class GraphQLParserTest {

    @Test
    fun testParsingTypes() {
        checkType(parseType("SomeType"), "SomeType")
        checkType(parseType("NotNull!"), "NotNull", NotNullType::class.java)
        checkType(parseType("[ListEl]"), "ListEl", ListType::class.java)
        checkType(parseType("[[[Abc!]]!]!"), "Abc", NotNullType::class.java, ListType::class.java, NotNullType::class.java, ListType::class.java, ListType::class.java, NotNullType::class.java)
    }

    @Test
    fun testParseValueConst() {
        // we won't obsess too much with different values here, as they're already tested in the tokenizer..

        checkScalarValue(123, parseConst("123"), ValueInt::class.java)
        checkScalarValue(-50, parseConst("-50"), ValueInt::class.java)

        checkScalarValue(123.0, parseConst("123.0"), ValueFloat::class.java)
        checkScalarValue(158.553E-3, parseConst("158.553E-3"), ValueFloat::class.java)
        checkScalarValue(-55E6, parseConst("-55E6"), ValueFloat::class.java)

        checkScalarValue("a b c???", parseConst("\"a b c???\""), ValueString::class.java)

        checkScalarValue(true, parseConst("true"), ValueBool::class.java)
        checkScalarValue(false, parseConst("false"), ValueBool::class.java)

        checkScalarValue(null, parseConst("null"), ValueNull::class.java)

        checkScalarValue("BUBU", parseConst("BUBU"), ValueEnum::class.java)

        val complex = parseConst("[ 666, { foo: bar bar :false, baz: [ 5, 4, 3 ] } true ]")
        assertTrue(complex is ValueList)

        val outerList = (complex as ValueList).elements
        assertEquals(3, outerList.size.toLong())
        checkScalarValue(666, outerList[0], ValueInt::class.java)
        assertTrue(outerList[1] is ValueObject)
        checkScalarValue(true, outerList[2], ValueBool::class.java)

        val obj = outerList[1] as ValueObject
        val objVals = obj.elements
        assertEquals(3, objVals.size.toLong())
        checkScalarValue("bar", objVals["foo"]!!, ValueEnum::class.java)
        checkScalarValue(false, objVals["bar"]!!, ValueBool::class.java)
        assertTrue(objVals["baz"] is ValueList)

        val innerList = (objVals["baz"] as ValueList).elements
        assertEquals(3, innerList.size.toLong())
        checkScalarValue(5, innerList[0], ValueInt::class.java)
        checkScalarValue(4, innerList[1], ValueInt::class.java)
        checkScalarValue(3, innerList[2], ValueInt::class.java)

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
        checkScalarValue(123, parseValue("123"), ValueInt::class.java)
        checkScalarValue(-50, parseValue("-50"), ValueInt::class.java)

        checkScalarValue(123.0, parseValue("123.0"), ValueFloat::class.java)
        checkScalarValue(158.553E-3, parseValue("158.553E-3"), ValueFloat::class.java)
        checkScalarValue(-55E6, parseValue("-55E6"), ValueFloat::class.java)

        checkScalarValue("a b c???", parseValue("\"a b c???\""), ValueString::class.java)

        checkScalarValue(true, parseValue("true"), ValueBool::class.java)
        checkScalarValue(false, parseValue("false"), ValueBool::class.java)

        checkScalarValue(null, parseValue("null"), ValueNull::class.java)

        checkScalarValue("BUBU", parseValue("BUBU"), ValueEnum::class.java)

        val complex = parseValue("[ 666, { foo: bar bar : \$var, baz: [ 5, \$boo, 3 ] } true ]")
        assertTrue(complex is ValueList)

        val outerList = (complex as ValueList).elements
        assertEquals(3, outerList.size.toLong())
        checkScalarValue(666, outerList[0], ValueInt::class.java)
        assertTrue(outerList[1] is ValueObject)
        checkScalarValue(true, outerList[2], ValueBool::class.java)

        val obj = outerList[1] as ValueObject
        val objVals = obj.elements
        assertEquals(3, objVals.size.toLong())
        checkScalarValue("bar", objVals["foo"]!!, ValueEnum::class.java)
        assertEquals("var", (objVals["bar"] as Variable).name)
        assertTrue(objVals["baz"] is ValueList)

        val innerList = (objVals["baz"] as ValueList).elements
        assertEquals(3, innerList.size.toLong())
        checkScalarValue(5, innerList[0], ValueInt::class.java)
        assertEquals("boo", (innerList[1] as Variable).name)
        checkScalarValue(3, innerList[2], ValueInt::class.java)

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
        internal fun parseConst(string: String): Value {
            val res = parser(string).parseValue(false)
            assertTrue(res is Value)
            return res as Value
        }

        @SafeVarargs
        internal fun checkType(type: TypeDef, baseName: String, vararg wrappers: Class<out WrapperType>) {
            var type = type
            assertNotNull(type)
            for (wrapper in wrappers) {
                assertTrue(wrapper.isAssignableFrom(type.javaClass))
                type = (type as WrapperType).inner
            }
            assertTrue(type is NamedType)
            assertEquals(baseName, (type as NamedType).name)
        }

        internal fun <T: Any> checkScalarValue(expect: T?, parsed: ValueOrVar, type: Class<out ValueScalar<T>>) {
            assert(type.isAssignableFrom(parsed.javaClass))
            assertEquals(expect, type.cast(parsed).value)
        }
    }
}