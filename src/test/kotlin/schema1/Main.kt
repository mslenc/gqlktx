package schema1

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
        builder.setClassPathScanSpec("schema1.*")
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
}
