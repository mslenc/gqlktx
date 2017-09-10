package schema1

import com.xs0.gqlktx.ann.GqlField
import com.xs0.gqlktx.findContextTypes
import com.xs0.gqlktx.findFields
import io.vertx.core.*
import io.vertx.core.json.JsonObject
import io.vertx.ext.asyncsql.MySQLClient
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import java.math.BigDecimal
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.reflect.*
import kotlin.reflect.full.*

fun main(args: Array<String>) {
    if (System.currentTimeMillis() > 0) {
        findContextTypes(ContextProvider::class).forEach { (klass, invokable) ->
            println("${klass} ${invokable.name}")
        }

        findFields(TestClass::class, findContextTypes(ContextProvider::class)).forEach { (name, field) ->
            println("${name} -> ${field.name} ${field.isAsync} ${field.publicType} ${field::class}")
        }
        return
    }
/*
    val vertx = Vertx.vertx()
    val server = vertx.createHttpServer()
    println("${TestSchema.numberOfTables} tables initialized")

    val mySQLClientConfig =
            JsonObject(mapOf(
                "host" to "127.0.0.1",
                "port" to 3306,
                "username" to "eteam",
                "password" to "eteam",
                "database" to "eteam"
            ))

    val mySqlClient = MySQLClient.createShared(vertx, mySQLClientConfig, "test")

    val dbConnector = DbConnectorImpl(mySqlClient)

    server.requestHandler({ request ->
        launch(Unconfined) {
            val start = System.currentTimeMillis()
            var response = "!!!"

            try {
                dbConnector.connect().use { db ->
                    val sb = StringBuilder()

                    val mitja = db.load(TestSchema.PEOPLE, 1)
                    val irena = db.load(TestSchema.PEOPLE, 2)

                    for (person in arrayOf(mitja, irena)) {
                        sb.append(person.firstName + " " + person.lastName + "\n")
                    }

                    response = sb.toString()
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                response = "Error: " + t
            }

            val res = request.response()
            res.putHeader("content-type", "text/plain; charset=UTF-8")
            res.end(response + "Hello World!")

            println("Finished in ${System.currentTimeMillis() - start}ms")
        }
    })

    server.listen(8888)*/
}


class SomeInput(
    val firstName: String,
    var lastName: String,
    age: Int?,
    ageAgain: Int
) {
    val ageFourth: Int = age ?: 10

    val ageThird: Int = age ?: 99

    val allAges34: Int
        get() = ageFourth + ageThird

    var someOtherThing: String? = "abc"

    var bubu: BigDecimal
        get() = BigDecimal.TEN
        set(value) { lastName += value.toPlainString() }
}


class ContextProvider(
    val vertx: Vertx,
    var verticle: Verticle?
) {
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
}









class TestClass {
    @GqlField
    fun normalFunc(): String {
        return "normalFunc"
    }

    @GqlField
    suspend fun suspendFunc(vertx: Vertx): String {
        return suspendCoroutine { cont ->
            vertx.setTimer(100, { timerId ->
                cont.resume("suspendFunc " + timerId)
            })
        }
    }

    @GqlField
    fun asyncHandlerFunc(vertx: Vertx, handler: Handler<AsyncResult<List<Set<Array<String?>>?>>>) {
        vertx.setTimer(150, { timerId ->
            handler.handle(Future.succeededFuture(listOf(setOf(arrayOf<String?>("asyncHandlerFunc " + timerId)))))
        })
    }

    @GqlField
    fun futureFunc(vertx: Vertx): Future<String> {
        val res = Future.future<String>()
        vertx.setTimer(50, { timerId ->
            res.complete("futureFunc " + timerId)
        })
        return res
    }

    @GqlField
    val normalProp: String
        get() = "normalProp"
}