package com.xs0.gqlktx.schema.intro

import com.xs0.gqlktx.ann.GQLArg
import com.xs0.gqlktx.ann.GraphQLObject
import com.xs0.gqlktx.schema.builder.TypeKind
import com.xs0.gqlktx.types.gql.*

@GraphQLObject("__Type")
class GqlIntroType(private val type: GType) {

    val kind: TypeKind
        get() = type.kind

    val name: String?
        get() = (type as? GBaseType)?.name

    // TODO
    val description: String?
        get() = null

    fun getFields(@GQLArg(defaultsTo = "false") includeDeprecated: Boolean): List<GqlIntroField>? {
        return (type as? GFieldedType)?.getFieldsForIntrospection(includeDeprecated)

    }

    val interfaces: List<GqlIntroType>?
        get() = (type as? GObjectType)?.interfacesForIntrospection

    val possibleTypes: List<GqlIntroType>?
        get() {
            if (type is GUnionType) {
                return type.membersForIntrospection
            }
            if (type is GInterfaceType) {
                return type.implsForIntrospection
            }
            return null
        }

    fun getEnumValues(@GQLArg(defaultsTo = "false") includeDeprecated: Boolean): List<GqlIntroEnumValue>? {
        return (type as? GEnumType)?.getValuesForIntrospection(includeDeprecated)
    }

    val inputFields: List<GqlIntroInputValue>?
        get() = if (type.kind != TypeKind.INPUT_OBJECT) null else (type as GInputObjType).inputFieldsForIntrospection

    val ofType: GqlIntroType?
        get() = (type as? GWrappingType)?.wrappedType?.introspector
}
