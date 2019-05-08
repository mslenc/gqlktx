package com.xs0.gqlktx.utils

data class QueryInput(val query: String, val variables: Map<String, Any?>?, val opName: String?, val allowMutations: Boolean)
