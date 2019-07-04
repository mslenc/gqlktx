package com.xs0.gqlktx.utils

import com.xs0.gqlktx.dom.ValueOrNull

data class QueryInput(val query: String, val variables: Map<String, ValueOrNull>?, val opName: String?, val allowMutations: Boolean)
