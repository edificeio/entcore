package org.entcore.common.utils;

public class Id<O extends IdObject, T> extends Identifier<O, T>
{
    public Id()
    {
        this(null);
    }

    public Id(T id)
    {
        super(id);
    }
}
