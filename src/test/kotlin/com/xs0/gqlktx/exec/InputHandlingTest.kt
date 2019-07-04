package com.xs0.gqlktx.exec

import com.xs0.gqlktx.dom.*
import com.xs0.gqlktx.schema.builder.AutoBuilder
import com.xs0.gqlktx.testschemas.inputs.InputsTestSchema
import com.xs0.gqlktx.utils.QueryInput
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*


@Suppress("UNCHECKED_CAST")
class InputHandlingTest {
    @Test
    fun testRequiredInputsFullyInQuery() {
        val query = """
            query Query1 {
                dumpRequired(input: { time: "12:54:11", main: { name: "Mitja", status: PREP }, others: [ { name: "Matt", status: ACTIVE } ] }, cases: [ AS_IS ])
            }
        """

        val schema = AutoBuilder.build(InputsTestSchema::class, Unit::class)

        val result = runBlocking {
            SimpleQueryExecutor.execute(schema, InputsTestSchema, Unit, QueryInput(query, emptyMap(), null, false))
        }

        val data = result.getValue("data") as Map<String, Any?>
        val dumpRequired = data.getValue("dumpRequired") as List<String>

        assertEquals(1, dumpRequired.size)
        assertEquals("RequiredInput(time=12:54:11, main=RequiredInfo(name=Mitja, status=PREP), others=[RequiredInfo(name=Matt, status=ACTIVE)])", dumpRequired[0])
    }

    @Test
    fun testMaybeWorks() {
        val query = """
            query ItemUpdate(${'$'}input: ItemUpdateInput!) {
                itemUpdate(input: ${'$'}input)
            }
        """.trimIndent()

        val schema = AutoBuilder.build(InputsTestSchema::class, Unit::class)

        val inputVariables: List<Map<String, Value>?> = listOf(
            null,

            mapOf<String, Value>("input" to ValueObject(emptyMap())),

            mapOf<String, Value>(
                "input" to ValueObject(mapOf(
                    "itemId" to ValueString("654321")
                ))
            ),

            mapOf<String, Value>(
                "input" to ValueObject(mapOf(
                    "itemId" to ValueString("654321"),
                    "name" to ValueNull()
                ))
            ),

            mapOf<String, Value>(
                "input" to ValueObject(mapOf(
                    "itemId" to ValueString("654321"),
                    "name" to ValueString("someName")
                ))
            ),

            mapOf<String, Value>(
                "input" to ValueObject(mapOf(
                    "itemId" to ValueString("654321"),
                    "description" to ValueNull()
                ))
            ),

            mapOf<String, Value>(
                "input" to ValueObject(mapOf(
                    "itemId" to ValueString("654321"),
                    "description" to ValueString("Description")
                ))
            ),

            mapOf<String, Value>(
                "input" to ValueObject(mapOf(
                    "itemId" to ValueString("654321"),
                    "name" to ValueString("The Item"),
                    "description" to ValueString("Description")
                ))
            )
        )

        val expectedResults = listOf(
            null,
            null,
            "654321,-,-",
            "654321,-,-",
            "654321,someName,-",
            "654321,-,!",
            "654321,-,Description",
            "654321,The Item,Description"
        )

        for (i in inputVariables.indices) {
            val vars = inputVariables[i]

            val result = runBlocking {
                SimpleQueryExecutor.execute(schema, InputsTestSchema, Unit, QueryInput(query, vars, "ItemUpdate", false))
            }

            if (vars == null) {
                val errors = result.getValue("errors") as List<Map<String, Any?>>
                assertEquals(1, errors.size)
                assertEquals("Missing data for variable \$input", errors[0].getValue("message"))
            } else
            if ((vars["input"] as? ValueObject)?.elements?.get("itemId") == null) {
                val errors = result.getValue("errors") as List<Map<String, Any?>>
                assertEquals(2, errors.size)
                assertEquals("Failed to parse field: Missing value for itemId", errors[0].getValue("message"))
            } else {
                val data = result.getValue("data") as Map<String, Any?>
                val opResult = data.getValue("itemUpdate") as String

                assertEquals(expectedResults[i], opResult)
            }
        }
    }

    @Test
    fun testRequiredInputsMixed() {
        val query = """
            query Query2(${'$'}time: Time!, ${'$'}mitjaStatus: Status!)  {
                dumpRequired(input: { time: ${'$'}time, main: { name: "Mitja", status: ${'$'}mitjaStatus }, others: [ { name: "Matt", status: ACTIVE } ] }, cases: [ UPPERCASE ] )
            }
        """

        val schema = AutoBuilder.build(InputsTestSchema::class, Unit::class)

        val variables = mapOf(
            "time" to ValueString("21:10:09"),
            "mitjaStatus" to ValueString("DELETED")
        )

        val result = runBlocking {
            SimpleQueryExecutor.execute(schema, InputsTestSchema, Unit, QueryInput(query, variables, null, false))
        }

        val data = result.getValue("data") as Map<String, Any?>
        val dumpRequired = data.getValue("dumpRequired") as List<String>

        assertEquals(1, dumpRequired.size)
        assertEquals("RequiredInput(time=21:10:09, main=RequiredInfo(name=Mitja, status=DELETED), others=[RequiredInfo(name=Matt, status=ACTIVE)])".toUpperCase(), dumpRequired[0])
    }

    @Test
    fun testRequiredInputsMixed2() {
        val query = """
            query Query3(${'$'}time: Time!, ${'$'}mitjaStatus: Status = ACTIVE)  {
                dumpRequired(input: { time: ${'$'}time, main: { name: "Mitja", status: ${'$'}mitjaStatus }, others: [ { name: "Matt", status: ACTIVE } ] }, cases: [ UPPERCASE ] )
            }
        """

        val schema = AutoBuilder.build(InputsTestSchema::class, Unit::class)

        val variables = mapOf(
                "time" to ValueString("21:10:09")
        )

        val result = runBlocking {
            SimpleQueryExecutor.execute(schema, InputsTestSchema, Unit, QueryInput(query, variables, null, false))
        }

        val data = result.getValue("data") as Map<String, Any?>
        val dumpRequired = data.getValue("dumpRequired") as List<String>

        assertEquals(1, dumpRequired.size)
        assertEquals("RequiredInput(time=21:10:09, main=RequiredInfo(name=Mitja, status=ACTIVE), others=[RequiredInfo(name=Matt, status=ACTIVE)])".toUpperCase(), dumpRequired[0])
    }

    @Test
    fun testNullsHandling() {
        val query = """
            query Query4(${'$'}a: String, ${'$'}b: String = "defVal")  {
                concat(a: ${'$'}a, b: ${'$'}b)
            }
        """

        val schema = AutoBuilder.build(InputsTestSchema::class, Unit::class)

        val variablePosibs: List<Map<String, ValueOrNull>?> = listOf(
            null,
            emptyMap(),
            mapOf("a" to ValueNull()),
            mapOf("a" to ValueString("AAA")),
            mapOf("b" to ValueNull()),
            mapOf("b" to ValueString("BBB")),
            mapOf("a" to ValueNull(), "b" to ValueNull()),
            mapOf("a" to ValueNull(), "b" to ValueString("BBB2")),
            mapOf("a" to ValueString("AAA2"), "b" to ValueNull()),
            mapOf("a" to ValueString("AAA3"), "b" to ValueString("BBB3"))
        )

        val expectedResults = listOf(
            "null-defVal",
            "null-defVal",
            "null-defVal",
            "AAA-defVal",
            "null-null",
            "null-BBB",
            "null-null",
            "null-BBB2",
            "AAA2-null",
            "AAA3-BBB3"
        )

        for (i in variablePosibs.indices) {
            val variables = variablePosibs[i]
            val expected = expectedResults[i]

            val result = runBlocking {
                SimpleQueryExecutor.execute(schema, InputsTestSchema, Unit, QueryInput(query, variables, null, false))
            }

            val data = result.getValue("data") as Map<String, Any?>
            val concat = data.getValue("concat") as String

            assertEquals(expected, concat)
        }
    }
}