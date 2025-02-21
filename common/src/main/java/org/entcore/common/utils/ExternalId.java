package org.entcore.common.utils;

public class ExternalId<O extends Object> extends Identifier<O, String>
{
    private final String DELIMITER = "-";
    private String prefix;

    public ExternalId()
    {
        this(null);
    }

    public ExternalId(String id)
    {
        this(null, id);
    }

    public ExternalId(String prefix, String id)
    {
        super(id);
        this.prefix = prefix == null ? null : prefix.endsWith(DELIMITER) ? prefix : prefix + DELIMITER;
    }

    @Override
    public String get()
    {
        return this.toString();
    }

    @Override
    public String toString()
    {
        return this.prefix == null ? this.id : this.prefix + this.id;
    }

    @Override
    public boolean equals(Object o)
    {
        return this.toString().equals(o);
    }
}
