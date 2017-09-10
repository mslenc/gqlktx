package com.xs0.gqlktx

import io.vertx.core.json.JsonObject

abstract class GraphQLException : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)

    open fun toGraphQLError(): JsonObject {
        val error = JsonObject()
        error.put("message", message)
        return error
    }
}
