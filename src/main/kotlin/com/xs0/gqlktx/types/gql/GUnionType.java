package com.xs0.gqlktx.types.gql;

import com.xs0.gqlktx.QueryException;
import com.xs0.gqlktx.schema.builder.TypeKind;
import com.xs0.gqlktx.schema.intro.GqlIntroType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GUnionType extends GBaseType {
    private Set<GObjectType> members;

    public GUnionType(String name) {
        super(name);
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.UNION;
    }

    public void setMembers(Set<GObjectType> members) {
        this.members = members;
    }

    public Set<GObjectType> getMembers() {
        return members;
    }

    @Override
    public boolean validAsQueryFieldType() {
        return true;
    }

    @Override
    public boolean validAsArgumentType() {
        return false;
    }

    @Override
    public void coerceValue(JsonObject raw, String key, JsonObject out) throws QueryException {
        throw new QueryException("Union type " + getName() + " can't be used as a variable");
    }

    @Override
    public void coerceValue(JsonArray raw, int index, JsonArray out) throws QueryException {
        throw new QueryException("Union type " + getName() + " can't be used as a variable");
    }

    @Override
    public void toString(StringBuilder sb) {
        sb.append("union ").append(getName());

        boolean first = true;
        for (GObjectType member : members) {
            sb.append(first ? " = " : " | ");
            first = false;
            sb.append(member.getName());
        }
        sb.append("\n");
    }

    public List<GqlIntroType> getMembersForIntrospection() {
        ArrayList<GqlIntroType> res = new ArrayList<>(members.size());
        for (GObjectType posib : members)
            res.add(posib.introspector());
        return res;
    }
}
