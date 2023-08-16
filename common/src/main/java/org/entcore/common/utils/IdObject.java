package org.entcore.common.utils;

public interface IdObject
{
    public Id getId();

    public default boolean equals(IdObject o)
    {
        if(o == null)
            return false;
        else if(this.getClass().equals(o.getClass()))
            return this.getId() == null ? o.getId() == null : this.getId().equals(o.getId());
        else
            return false;
    }
}
