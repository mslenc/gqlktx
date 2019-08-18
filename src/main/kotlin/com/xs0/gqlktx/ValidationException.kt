package com.xs0.gqlktx

class ValidationException : QueryException {
    constructor(msg: String) : super(msg)
    constructor(cause: Throwable) : super(cause)
    constructor(msg: String, cause: Throwable) : super(msg, cause)
}
