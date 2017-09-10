package com.xs0.gqlktx.types.gql;

import com.xs0.gqlktx.schema.builder.TypeKind;
import com.xs0.gqlktx.schema.intro.GqlIntroField;
import com.xs0.gqlktx.schema.intro.GqlIntroInputValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class GFieldedType extends GBaseType {
    protected final Map<String, GField> fields;

    protected GFieldedType(String name, Map<String, GField> fields) {
        super(name);

        this.fields = fields; // can't check isEmpty() here, because it's populated later
    }

    public Map<String, GField> getFields() {
        return fields;
    }

    @Override
    public boolean validAsQueryFieldType() {
        return getKind() == TypeKind.INPUT_OBJECT;
    }

    protected void dumpFieldsToString(StringBuilder sb) {
        for (Map.Entry<String, GField> entry : fields.entrySet()) {
            sb.append("  ");
            entry.getValue().toString(sb);
            sb.append("\n");
        }
    }

    public List<GqlIntroField> getFieldsForIntrospection(boolean includeDeprecated) {
        if (getKind() == TypeKind.INPUT_OBJECT)
            return null;

        ArrayList<GqlIntroField> res = new ArrayList<>(fields.size());
        for (Map.Entry<String, GField> entry : fields.entrySet())
            res.add(new GqlIntroField(entry.getValue()));

        return res;
    }

    public List<GqlIntroInputValue> getInputFieldsForIntrospection() {
        if (getKind() != TypeKind.INPUT_OBJECT)
            return null;

        ArrayList<GqlIntroInputValue> res = new ArrayList<>(fields.size());
        for (Map.Entry<String, GField> entry : fields.entrySet())
            res.add(new GqlIntroInputValue(entry.getValue()));

        return res;
    }
}
