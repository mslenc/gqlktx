package schema1

import com.xs0.gqlktx.exec.SimpleQueryExecutor
import com.xs0.gqlktx.schema.builder.AutoBuilder
import com.xs0.gqlktx.utils.QueryInput
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldNotBe
import io.kotlintest.specs.StringSpec
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.runBlocking

class FirstTest : StringSpec() {
    init {
        "result should be returned" {
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

            val result: JsonObject = runBlocking {
                SimpleQueryExecutor.execute(schema, SchemaRoot, context, QueryInput(query, null, null, false))
            }

            result shouldNotBe null
            result.getJsonArray("errors") shouldBe null
            result.getJsonObject("data") shouldNotBe null
            val data = result.getJsonObject("data")

            println(data.encodePrettily())
        }
    }
}
