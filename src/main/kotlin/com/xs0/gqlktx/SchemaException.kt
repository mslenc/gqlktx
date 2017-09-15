package com.xs0.gqlktx

class SchemaException : RuntimeException {
    constructor(msg: String) : super(msg)

    constructor(cause: Throwable) : super(cause)
}
