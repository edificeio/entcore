package org.entcore.common.utils;

import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;

import org.entcore.common.json.JSONTransform;

public class Identifier<O extends Object, T> implements JSONTransform
{
    protected T id;

    public Identifier()
    {
        this(null);
    }

    public Identifier(T id)
    {
        this.id = id;
    }

    public T get()
    {
        return this.id;
    }

    @Override
    public Object toJson()
    {
        return this.id;
    }

    @Override
    public void fromJson(Object jsonValue, Type declaredType)
    {
        if(declaredType instanceof ParameterizedType)
            this.id = (T) __convertGeneric(jsonValue, (ParameterizedType) declaredType, 1);
        else
            this.id = (T) jsonValue;
    }

    public boolean equals(Identifier o)
    {
        if(o == null)
            return false;
        else
            return this.id == null ? o.id == null : this.id.equals(o.id);
    }

    @Override
    public String toString()
    {
        return this.id != null ? this.id.toString() : "null";
    }
}
