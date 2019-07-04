package com.xs0.gqlktx.testschemas.inputs

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
                Case.LOWERCASE -> inputString.toLowerCase()
                Case.UPPERCASE -> inputString.toUpperCase()
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
}

@GqlSchema
object InputsTestSchema {
    @GqlQueryRoot
    fun getQueryRoot(): QueryRoot {
        return QueryRoot()
    }
}