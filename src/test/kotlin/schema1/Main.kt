package schema1

import com.xs0.dbktx.conn.DbConn
import com.xs0.dbktx.conn.DbConnectorImpl
import io.vertx.core.*
import io.vertx.core.json.JsonObject
import io.vertx.ext.asyncsql.MySQLClient
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.reflect.*
import kotlin.reflect.full.*

fun main(args: Array<String>) {
    if (System.currentTimeMillis() > 0) {
        findFields(TestClass::class, findContextTypes(ContextProvider::class))
        return
    }

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

    server.listen(8888)
}



class ContextProvider(
    val vertx: Vertx,
    var verticle: Verticle?
) {

    fun db(): DbConn {
        throw UnsupportedOperationException("Not implemented yet")
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
}









class TestClass {
    fun normalFunc(): String {
        return "normalFunc"
    }

    suspend fun suspendFunc(vertx: Vertx): String {
        return suspendCoroutine { cont ->
            vertx.setTimer(100, { timerId ->
                cont.resume("suspendFunc " + timerId)
            })
        }
    }

    fun asyncHandlerFunc(vertx: Vertx, handler: Handler<AsyncResult<String>>) {
        vertx.setTimer(150, { timerId ->
            handler.handle(Future.succeededFuture("asyncHandlerFunc " + timerId))
        })
    }

    fun futureFunc(vertx: Vertx): Future<String> {
        val res = Future.future<String>()
        vertx.setTimer(50, { timerId ->
            res.complete("futureFunc " + timerId)
        })
        return res
    }

    val normalProp: String
        get() = "normalProp"
}