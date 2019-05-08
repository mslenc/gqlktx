package com.xs0.gqlktx

import com.xs0.gqlktx.parser.Token

class ParseException(message: String, val row: Int, val column: Int) : QueryException(message) {

    constructor(message: String, token: Token<*>) : this(message, token.row, token.column)

    override fun toGraphQLError(): Map<String, Any?> {
        val error = super.toGraphQLError().toMutableMap()
        error["locations"] = arrayOf(mapOf(
                "line" to row,
                "column" to column
        ))
        return error
    }
}
