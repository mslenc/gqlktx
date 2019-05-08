package com.xs0.gqlktx

import com.xs0.gqlktx.exec.FieldPath

class FieldException: GraphQLException {
    val path: FieldPath

    constructor(message: String, path: FieldPath) : super(message) {
        this.path = path
    }

    constructor(message: String, path: FieldPath, cause: Throwable) : super(message, cause) {
        this.path = path
    }

    override fun toGraphQLError(): Map<String, Any?> {
        return super.toGraphQLError() + ("path" to path.toArray())

//        if (location != null) {
//            val loc = JsonObject()
//            loc.put("line", location.row)
//            loc.put("column", location.column)
//            error.put("locations", JsonArray().add(loc))
//        }
    }
}
