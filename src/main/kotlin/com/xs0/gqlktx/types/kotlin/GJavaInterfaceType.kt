package com.xs0.gqlktx.types.kotlin

import com.xs0.gqlktx.types.gql.GInterfaceType
import kotlin.reflect.KClass
import kotlin.reflect.KType

class GJavaInterfaceType<CTX>(type: KType, gqlType: GInterfaceType, implementations: Array<KClass<*>>) : GJavaImplementableType<CTX>(type, gqlType, implementations) {
    override fun checkUsage(isInput: Boolean) {
        if (isInput)
            throw IllegalStateException("Interface $type is used as input")
    }
}
