package com.xs0.gqlktx

import com.xs0.gqlktx.parser.Token
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

class ParseException(message: String, val row: Int, val column: Int) : QueryException(message) {

    constructor(message: String, token: Token<*>) : this(message, token.row, token.column)

    override fun toGraphQLError(): JsonObject {
        val location = JsonObject()
        location.put("line", row)
        location.put("column", column)

        val locations = JsonArray()
        locations.add(location)

        val error = super.toGraphQLError()
        error.put("locations", locations)

        return error
    }
}
