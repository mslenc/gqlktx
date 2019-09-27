package schema1

import com.xs0.gqlktx.ScalarCoercion
import com.xs0.gqlktx.dom.ValueBool
import com.xs0.gqlktx.dom.ValueOrNull
import com.xs0.gqlktx.exec.SimpleQueryExecutor
import com.xs0.gqlktx.schema.builder.AutoBuilder
import com.xs0.gqlktx.utils.QueryInput
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FirstTest {
    @Test
    fun aResultShouldBeReturned() {
        val builder = AutoBuilder(SchemaRoot::class, TestContextProvider::class)
        builder.setClassPathScanSpec("schema1")
        val schema = builder.build()
        val context = TestContextProvider("foo")
        val query = """
                        {
                            posts {
                                id
                                title
                                text
                                owner {
                                    id
                                    email
                                    fullName
                                }
                            }
                        }
                    """.trimIndent()

        val result: Map<String, Any?> = runBlocking {
            SimpleQueryExecutor.execute(schema, SchemaRoot, context, QueryInput(query, null, null, false))
        }

        assertNotNull(result)
        assertNull(result["errors"])
        assertNotNull(result["data"])
        assertEquals("{posts=[{id=w3BzdAEB, title=First post ever, text=null, owner={id=w3VzcgEB, email=mslenc@gmail.com, fullName=null}}, {id=w3BzdAED, title=Second post ever, text=With text this time :), owner={id=w3VzcgEC, email=john@example.com, fullName=John}}, {id=w3BzdAEI, title=Third post, text=Something old, something new, owner={id=w3VzcgED, email=mary@example.com, fullName=Mary Johnson}}, {id=w3BzdAEJ, title=Final post, text=Again, some text, owner={id=w3VzcgEB, email=mslenc@gmail.com, fullName=null}}]}", result["data"].toString())
    }

    @Test
    fun includeDirectiveShouldWork() {
        val builder = AutoBuilder(SchemaRoot::class, TestContextProvider::class)
        builder.setClassPathScanSpec("schema1")
        val schema = builder.build()
        val context = TestContextProvider("foo")
        val query = """
                        query SkipTest(${'$'}trueVar: Boolean, ${'$'}falseVar: Boolean) {
                            posts {
                                id @include(if: true)
                                title @include(if: false)
                                text @include(if: ${'$'}trueVar)
                                owner @include(if: ${'$'}falseVar) {
                                    id
                                    email
                                    fullName
                                }
                            }
                        }
                    """.trimIndent()

        val variables: Map<String, ValueOrNull> = mapOf(
            "trueVar" to ValueBool(true),
            "falseVar" to ValueBool(false)
        )

        val result: Map<String, Any?> = runBlocking {
            SimpleQueryExecutor.execute(schema, SchemaRoot, context, QueryInput(query, variables, "SkipTest", false))
        }

        assertNotNull(result)
        assertNull(result["errors"])
        assertNotNull(result["data"])
        assertEquals("{posts=[{id=w3BzdAEB, text=null}, {id=w3BzdAED, text=With text this time :)}, {id=w3BzdAEI, text=Something old, something new}, {id=w3BzdAEJ, text=Again, some text}]}", result["data"].toString())
    }

    @Test
    fun skipDirectiveShouldWork() {
        val builder = AutoBuilder(SchemaRoot::class, TestContextProvider::class)
        builder.setClassPathScanSpec("schema1")
        val schema = builder.build()
        val context = TestContextProvider("foo")
        val query = """
                        query SkipTest(${'$'}trueVar: Boolean, ${'$'}falseVar: Boolean) {
                            posts {
                                id @skip(if: false)
                                title @skip(if: true)
                                text @skip(if: ${'$'}falseVar)
                                owner @skip(if: ${'$'}trueVar) {
                                    id
                                    email
                                    fullName
                                }
                            }
                        }
                    """.trimIndent()

        val variables: Map<String, ValueOrNull> = mapOf(
                "trueVar" to ValueBool(true),
                "falseVar" to ValueBool(false)
        )

        val result: Map<String, Any?> = runBlocking {
            SimpleQueryExecutor.execute(schema, SchemaRoot, context, QueryInput(query, variables, "SkipTest", false))
        }

        assertNotNull(result)
        assertNull(result["errors"])
        assertNotNull(result["data"])
        assertEquals("{posts=[{id=w3BzdAEB, text=null}, {id=w3BzdAED, text=With text this time :)}, {id=w3BzdAEI, text=Something old, something new}, {id=w3BzdAEJ, text=Again, some text}]}", result["data"].toString())
    }

    @Test
    fun typeIntrospectionShouldWork() {
        val builder = AutoBuilder(SchemaRoot::class, TestContextProvider::class)
        builder.setClassPathScanSpec("schema1")
        val schema = builder.build()
        val context = TestContextProvider("foo")
        val query = """
                        {
                            postType: __type(name: "Post") {
                                kind
                                name
                                fields {
                                    name
                                    type {
                                        kind
                                        name
                                    }
                                }
                            }
                            
                            filterType: __type(name: "PostFilters") {
                                kind
                                name
                                inputFields {
                                    name
                                    type {
                                        name
                                    }
                                }
                            }
                        }
                    """.trimIndent()

        val result: Map<String, Any?> = runBlocking {
            SimpleQueryExecutor.execute(schema, SchemaRoot, context, QueryInput(query, null, null, false))
        }

        assertNotNull(result)
        assertNull(result["errors"])
        assertNotNull(result["data"])
        assertEquals("{postType={kind=OBJECT, name=Post, fields=[{name=id, type={kind=NON_NULL, name=null}}, {name=text, type={kind=SCALAR, name=String}}, {name=title, type={kind=NON_NULL, name=null}}, {name=tags, type={kind=NON_NULL, name=null}}, {name=owner, type={kind=NON_NULL, name=null}}]}, filterType={kind=INPUT_OBJECT, name=PostFilters, inputFields=[{name=titleWords, type={name=String}}, {name=keywords, type={name=String}}]}}", result["data"].toString())
    }

    @Test
    fun abstractClassInterfacesShouldWork() {
        val builder = AutoBuilder(SchemaRoot::class, TestContextProvider::class)
        builder.setClassPathScanSpec("schema1")
        val schema = builder.build()
        val context = TestContextProvider("foo")
        val query = """
                        {
                            orgs {
                                id
                                name
                                allEnums {
                                    id
                                    name
                                    ... on ClientRating {
                                        comments
                                    }
                                }
                            }
                        }
                    """.trimIndent()

        val result: Map<String, Any?> = runBlocking {
            SimpleQueryExecutor.execute(schema, SchemaRoot, context, QueryInput(query, null, null, false), ScalarCoercion.JSON)
        }

        assertNotNull(result)
        assertNull(result["errors"])
        assertNotNull(result["data"])
        assertEquals("{orgs=[{id=w29yZwEL, name=TheOrg, allEnums=[{id=w0lORAUp, name=TheIndustry}, {id=w0NPTQU3, name=Big}, {id=w0NMSQVC, name=Very good, comments=Used for very good clients}]}, {id=w29yZwEs, name=TheCompany, allEnums=[{id=w0lORAUp, name=The Other Industry}]}]}", result["data"].toString())
    }
}
