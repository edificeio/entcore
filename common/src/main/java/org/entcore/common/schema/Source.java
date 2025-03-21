package org.entcore.common.schema;

public enum Source
{
    UNKNOWN,
    MANUAL,
    AAF,
    AAF1D,
    CSV,
    EDT;

    public static Source fromString(String src)
    {
        return src == null || "".equals(src) ? UNKNOWN : Source.valueOf(src);
    }
}
