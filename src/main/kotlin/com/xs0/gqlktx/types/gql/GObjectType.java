package com.xs0.gqlktx.types.gql;

import com.xs0.gqlktx.QueryException;
import com.xs0.gqlktx.schema.builder.TypeKind;
import com.xs0.gqlktx.schema.intro.GqlIntroType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;

public class GObjectType extends GFieldedType {
    private Set<GInterfaceType> interfaces = Collections.emptySet(); // lazy allocate

    public GObjectType(String name, Map<String, GField> fields) {
        super(name, fields);
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.OBJECT;
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
        sb.append("type ").append(getName());

        boolean first = true;
        for (GInterfaceType i : interfaces) {
            sb.append(first ? " implements " : ", ");
            first = false;
            sb.append(i.getName());
        }

        sb.append(" {\n");
        dumpFieldsToString(sb);
        sb.append("}\n");
    }

    void addInterface(GInterfaceType interfaceType) {
        if (interfaces.isEmpty())
            interfaces = new LinkedHashSet<>();

        interfaces.add(interfaceType);
    }

    public List<GqlIntroType> getInterfacesForIntrospection() {
        ArrayList<GqlIntroType> res = new ArrayList<>(interfaces.size());
        for (GInterfaceType inter : interfaces)
            res.add(inter.introspector());
        return res;
    }
}
