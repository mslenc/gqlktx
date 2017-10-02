package com.xs0.gqlktx.schema.intro

import com.xs0.gqlktx.GqlObject
import com.xs0.gqlktx.dom.Value
import com.xs0.gqlktx.types.gql.GArgument
import com.xs0.gqlktx.types.gql.GField

@GqlObject("__InputValue")
class GqlIntroInputValue {
    val name: String
    val description: String?
    val type: GqlIntroType
    private val _defaultValue: Value?
    val defaultValue: String?
        get() = _defaultValue?.toString()

    constructor(name: String, description: String, type: GqlIntroType, _defaultValue: Value?) {
        this.name = name
        this.description = description
        this.type = type
        this._defaultValue = _defaultValue
    }

    constructor(arg: GArgument) {
        this.name = arg.name
        this.description = null // TODO
        this.type = arg.type.introspector
        this._defaultValue = arg.defaultValue
    }

    constructor(value: GField) {
        this.name = value.name
        this.description = null // TODO
        this.type = value.type.introspector
        this._defaultValue = null
    }
}
