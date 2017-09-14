package com.xs0.gqlktx.utils

import io.vertx.core.json.JsonObject

data class QueryInput(val query: String, val variables: JsonObject?, val opName: String?, val allowMutations: Boolean)
