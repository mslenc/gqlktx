package com.xs0.gqlktx.schema.builder;

import com.xs0.gqlktx.ann.GqlIgnore;
import com.xs0.gqlktx.ann.GraphQLInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;

import static com.xs0.gqlktx.UtilsKt.getNullValue;
import static com.xs0.gqlktx.UtilsKt.setterName;
import static com.xs0.gqlktx.UtilsKt.validGraphQLName;

public class InputMethodInfo<CTX> {
    private static final Logger log = LoggerFactory.getLogger(InputMethodInfo.class);

    private final Method method;
    private final String name;
    private final ParamInfo[] params;
    private final Type type;

    public InputMethodInfo(Method method, String name, ParamInfo[] params, Type type) {
        this.method = method;
        this.name = name;
        this.params = params;
        this.type = type;
    }

    /**
     * Returns the actual method this object describes.
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Returns the (public) name to be used for this method (i.e. the GraphQL field name).
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the list of parameters. There will be exactly one PUBLIC parameter, zero
     * or one QUERY_CTX parameter, and zero or more IGNORED parameters.
     */
    public ParamInfo[] getParams() {
        return params;
    }

    /**
     * Returns the type of this field, that is, the type of the value this method accepts
     * (remember, it's a setter).
     */
    public Type getType() {
        return type;
    }

    /**
     * Produces the method information, if the method is of a supported type.
     * Otherwise, returns null.
     *
     * @param method   the method to analyze
     * @param queryCtx the query context class
     */
    public static <CTX> InputMethodInfo<CTX> create(Method method, Class<CTX> queryCtx) {
        log.trace("Examining {} with queryCtx={}", method, queryCtx);

        int nParams = method.getParameterCount();
        ParamInfo[] params = new ParamInfo[nParams];
        ParamInfo valueParam = null;
        ParamInfo queryCtxParam = null;

        Parameter[] rawParams = method.getParameters();

        for (int i = 0; i < nParams; i++) {
            Parameter raw = rawParams[i];
            Type type = raw.getParameterizedType();

            if (raw.getAnnotation(GqlIgnore.class) != null) {
                params[i] = ParamInfo.ignored(getNullValue(type));
                continue;
            }

            if (type == queryCtx) {
                if (queryCtxParam != null) {
                    log.debug("Found two query context parameters, so skipping the method");
                    return null;
                }

                params[i] = queryCtxParam = ParamInfo.queryCtx();
                continue;
            }

            if (valueParam != null) {
                log.debug("Two value parameters found, so skipping the method");
                return null;
            }

            valueParam = ParamInfo.valueForSetter(type);
            params[i] = valueParam;
        }

        if (valueParam == null) {
            log.debug("Didn't find any value accepting parameter, so skipping the method");
            return null;
        }

        String name = null;
        boolean nameWasExplicit = false;
        GraphQLInput ann = method.getAnnotation(GraphQLInput.class);
        if (ann != null && !ann.value().isEmpty()) {
            name = ann.value();
            nameWasExplicit = true;
        } else {
            String setterName = setterName(method.getName());
            if (setterName != null) {
                name = setterName;
            } else {
                if (ann != null) {
                    name = method.getName();
                    nameWasExplicit = true;
                }
            }
        }

        if (!validGraphQLName(name, false)) {
            if (nameWasExplicit) {
                throw new IllegalStateException("Invalid name " + name + " for method " + method);
            } else {
                log.debug("Name {} is not a valid GraphQL name, so skipping method", name);
                return null;
            }
        }

        return new InputMethodInfo<>(method, name, params, valueParam.getType());
    }

    public void execute(CTX context, Object obj, Object value) throws Exception {
        int nArgs = params.length;
        Object[] args = new Object[nArgs];
        for (int a = 0; a < nArgs; a++) {
            ParamInfo param = params[a];
            switch (param.getKind()) {
                case PUBLIC:
                    args[a] = value;
                    break;

                case IGNORED:
                    args[a] = param.getDefaultValue();
                    break;

                case QUERY_CTX:
                    args[a] = context;
                    break;

                case ASYNC_HANDLER:
                    throw new IllegalStateException("internal error: async handler argument in setter method");
            }
        }

        method.invoke(obj, args);
    }
}