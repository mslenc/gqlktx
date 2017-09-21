package com.xs0.gqlktx

import com.xs0.gqlktx.exec.FieldPath
import com.xs0.gqlktx.parser.Token
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

class FieldException: GraphQLException {
    val path: FieldPath

    constructor(message: String, path: FieldPath) : super(message) {
        this.path = path
    }

    constructor(message: String, path: FieldPath, cause: Throwable) : super(message, cause) {
        this.path = path
    }

    override fun toGraphQLError(): JsonObject {
        val error = super.toGraphQLError()

        error.put("path", path.toArray())

//        if (location != null) {
//            val loc = JsonObject()
//            loc.put("line", location.row)
//            loc.put("column", location.column)
//            error.put("locations", JsonArray().add(loc))
//        }

        return error
    }
}
