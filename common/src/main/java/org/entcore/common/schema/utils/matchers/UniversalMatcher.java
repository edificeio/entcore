package org.entcore.common.schema.utils.matchers;

import io.vertx.core.json.JsonObject;

public class UniversalMatcher extends Matcher
{
    public UniversalMatcher()
    {
        super();
    }

    public UniversalMatcher(Operation operation)
    {
        super(operation);
    }

    @Override
    protected String where()
    {
        return "true";
    }

    @Override
    public void addParams(JsonObject params)
    {
    }
}
