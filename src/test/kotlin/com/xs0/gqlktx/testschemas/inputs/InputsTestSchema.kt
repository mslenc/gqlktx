package com.xs0.gqlktx.testschemas.inputs

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.xs0.gqlktx.*
import com.xs0.gqlktx.utils.Maybe
import java.lang.StringBuilder
import java.time.LocalTime

enum class Status {
    PREP,
    ACTIVE,
    DELETED
}

enum class Case {
    AS_IS,
    LOWERCASE,
    UPPERCASE
}

data class RequiredInfo(
    val name: String,
    val status: Status
)

data class RequiredInput(
    val time: LocalTime,
    val main: RequiredInfo,
    val others: List<RequiredInfo>
)

data class ItemUpdateInput(
    val itemId: String,
    val name: String?,
    val description: Maybe<String?>?
)

data class GenericThing(
    val type: String,
    val id: String,
    val props: ObjectNode
)

data class GenericThingInput(
    val type: String,
    val id: String,
    val props: ObjectNode
)

class QueryRoot {
    @GqlField
    fun getDumpRequired(
        @GqlParam("name") input: RequiredInput,
        @GqlParam(defaultsTo = "[ AS_IS, UPPERCASE ]") cases: List<Case>?
    ): List<String> {
        val inputString = input.toString()
        return (cases ?: emptyList()).map {
            when (it) {
                Case.AS_IS -> inputString
                Case.LOWERCASE -> inputString.lowercase()
                Case.UPPERCASE -> inputString.uppercase()
            }
        }
    }

    @GqlField
    fun getItemUpdate(
        @GqlParam input: ItemUpdateInput
    ): String {
        val sb = StringBuilder()

        sb.append(input.itemId)

        if (input.name != null) {
            sb.append(",").append(input.name)
        } else {
            sb.append(",-")
        }

        if (input.description != null) {
            if (input.description.value != null) {
                sb.append(",").append(input.description.value)
            } else {
                sb.append(",!")
            }
        } else {
            sb.append(",-")
        }

        return sb.toString()
    }

    @GqlField
    fun getConcat(a: String?, b: String?): String {
        return "$a-$b"
    }

    @GqlField
    fun getGenericThingUpdate(input: GenericThingInput): GenericThing {
        val updated = input.props.deepCopy()
        updated.set<JsonNode>("updated", BooleanNode.TRUE)
        return GenericThing(input.type, input.id, updated)
    }
}

@GqlSchema
object InputsTestSchema {
    @GqlQueryRoot
    fun getQueryRoot(): QueryRoot {
        return QueryRoot()
    }
}