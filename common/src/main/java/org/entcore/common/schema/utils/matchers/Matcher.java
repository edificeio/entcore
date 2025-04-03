package org.entcore.common.schema.utils.matchers;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

import java.util.Collection;
import java.util.List;

public abstract class Matcher
{
    public static enum Operation { INCLUDE, EXCLUDE };

    private Operation operation;

    public Matcher()
    {
        this(null);
    }

    public Matcher(Operation operation)
    {
        this.operation = operation != null ? operation : Operation.INCLUDE;
    }

    protected abstract String where();
    public abstract void addParams(JsonObject params);

    public final String match()
    {
        switch(this.operation)
        {
            case INCLUDE:
                return "(" + this.where() + ")";
            case EXCLUDE:
                return "NOT(" + this.where() + ")";
            default:
                throw new UnsupportedOperationException("Unknown operation " + this.operation);
        }
    }

    protected final void addParam(JsonObject params, String key, Object value)
    {
        params.put(key, paramValue(value));
    }

    private static final Object paramValue(Object value)
    {
        if(value instanceof Collection)
        {
            if(value instanceof List)
                return new JsonArray((List) value);
            else
            {
                JsonArray aVal = new JsonArray();
                for(Object o : (Collection) value)
                    aVal.add(paramValue(o));
                return aVal;
            }
        }
        else if(value instanceof Enum)
            return value.toString();
        else
            return value;
    }

    protected static final String equality(String expression, String valueStr, Collection<?> values)
    {
        if(values == null || values.isEmpty())
            return expression + " IS NULL";
        else if(values.size() == 1)
            return expression + " = " + valueStr;
        else
            return expression + " IN " + valueStr;
    }

    protected static final Object getCollectionOrOnlyElement(Collection c)
    {
        if(c.size() == 1)
        {
            for(Object o : c)
                return o;
            throw new RuntimeException("Impossible error in getCollectionOrOnlyElement");
        }
        else
            return c;
    }

    @Override
    public String toString()
    {
        return this.match();
    }
}
