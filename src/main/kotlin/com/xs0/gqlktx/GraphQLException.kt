package com.xs0.gqlktx

abstract class GraphQLException : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)

    open fun toGraphQLError(): Map<String, Any?> {
        return mapOf("message" to message)
    }
}
