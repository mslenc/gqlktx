package com.xs0.gqlktx.schema.intro;

import com.xs0.gqlktx.ann.GQLArg;
import com.xs0.gqlktx.ann.GraphQLObject;
import com.xs0.gqlktx.schema.builder.TypeKind;
import com.xs0.gqlktx.types.gql.*;

import java.util.List;

@GraphQLObject("__Type")
public class GqlIntroType {
    private final GType type;

    public GqlIntroType(GType type) {
        this.type = type;
    }

    public TypeKind getKind() {
        return type.getKind();
    }

    public String getName() {
        if (!(type instanceof GBaseType))
            return null;

        return ((GBaseType)type).getName();
    }

    public String getDescription() {
        return null; // TODO
    }

    public List<GqlIntroField> getFields(@GQLArg(defaultsTo = "false") boolean includeDeprecated) {
        if (!(type instanceof GFieldedType))
            return null;

        return ((GFieldedType)type).getFieldsForIntrospection(includeDeprecated);
    }

    public List<GqlIntroType> getInterfaces() {
        if (type.getKind() != TypeKind.OBJECT)
            return null;

        return ((GObjectType) type).getInterfacesForIntrospection();
    }

    public List<GqlIntroType> getPossibleTypes() {
        if (type.getKind() != TypeKind.UNION)
            return null;

        return ((GUnionType) type).getMembersForIntrospection();
    }

    public List<GqlIntroEnumValue> getEnumValues(@GQLArg(defaultsTo = "false") boolean includeDeprecated) {
        if (type.getKind() != TypeKind.ENUM)
            return null;

        return ((GEnumType)type).getValuesForIntrospection(includeDeprecated);
    }

    public List<GqlIntroInputValue> getInputFields() {
        if (type.getKind() != TypeKind.INPUT_OBJECT)
            return null;

        return ((GInputObjType)type).getInputFieldsForIntrospection();
    }

    public GqlIntroType getOfType() {
        if (!(type instanceof GWrappingType))
            return null;

        return ((GWrappingType)type).getWrappedType().introspector();
    }
}
