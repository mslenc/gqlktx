package com.xs0.gqlktx

open class QueryException : GraphQLException {
    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
}
