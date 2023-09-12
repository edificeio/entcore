package org.entcore.common.schema.utils.matchers;

import org.entcore.common.schema.Source;

import io.vertx.core.json.JsonObject;

import java.util.Set;
import java.util.HashSet;

public class SourceMatcher extends NodeMatcher
{
    private static final String FIELD_SOURCE = "source";

    private Set<Source> sources = new HashSet<Source>();

    public SourceMatcher(Source... sources)
    {
        this(null, null, sources);
    }

    public SourceMatcher(Operation operation, Source... sources)
    {
        this(operation, null, sources);
    }

    public SourceMatcher(String nodeName, Source... sources)
    {
        this(null, nodeName, sources);
    }

    public SourceMatcher(Operation operation, String nodeName, Source... sources)
    {
        super(operation, nodeName);

        if(sources == null || sources.length == 0)
            throw new IllegalArgumentException("No sources provided");

        for(Source s : sources)
            if(s == null)
                throw new IllegalArgumentException("Null source provided");
            else
                this.sources.add(s);
    }

    @Override
    protected String where()
    {
        String coal = "COALESCE(" + this.qualify(FIELD_SOURCE) + ", '" + Source.UNKNOWN + "')";

        return Matcher.equality(coal, this.parameterize(FIELD_SOURCE), this.sources);
    }

    @Override
    public void addParams(JsonObject params)
    {
        this.addParam(params, this.parameter(FIELD_SOURCE), this.sources);
    }
}
