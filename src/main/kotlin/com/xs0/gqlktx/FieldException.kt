package com.xs0.gqlktx

import com.xs0.gqlktx.exec.FieldPath
import com.xs0.gqlktx.parser.Token
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

class FieldException(message: String, val path: FieldPath, val location: Token<*>?) : GraphQLException(message) {

    override fun toGraphQLError(): JsonObject {
        val error = super.toGraphQLError()

        error.put("path", path.toArray())

        if (location != null) {
            val loc = JsonObject()
            loc.put("line", location.row)
            loc.put("column", location.column)
            error.put("locations", JsonArray().add(loc))
        }

        return error
    }
}
