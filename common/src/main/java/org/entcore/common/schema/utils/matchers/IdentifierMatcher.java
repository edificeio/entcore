package org.entcore.common.schema.utils.matchers;

import org.entcore.common.utils.Identifier;
import org.entcore.common.utils.Id;
import org.entcore.common.utils.ExternalId;

import io.vertx.core.json.JsonObject;

import java.util.Collection;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;

public class IdentifierMatcher<T> extends NodeMatcher<T>
{
    private static final String FIELD_IDS = "id";
    private static final String FIELD_EXTERNAL_IDS = "externalId";

    private Set ids = new HashSet();
    private Set<String> externalIds = new HashSet<String>();

    public IdentifierMatcher(Identifier<T, ?>... identifiers)
    {
        this(Arrays.asList(identifiers));
    }

    public IdentifierMatcher(Collection<? extends Identifier<T, ?>> identifiers)
    {
        this(null, null, identifiers);
    }

    public IdentifierMatcher(Operation operation, Identifier<T, ?>... identifiers)
    {
        this(operation, Arrays.asList(identifiers));
    }

    public IdentifierMatcher(Operation operation, Collection<? extends Identifier<T, ?>> identifiers)
    {
        this(operation, null, identifiers);
    }

    public IdentifierMatcher(String nodeName, Identifier<T, ?>... identifiers)
    {
        this(nodeName, Arrays.asList(identifiers));
    }

    public IdentifierMatcher(String nodeName, Collection<? extends Identifier<T, ?>> identifiers)
    {
        this(null, nodeName, identifiers);
    }

    public IdentifierMatcher(Operation operation, String nodeName, Identifier<T, ?>... identifiers)
    {
        this(operation, nodeName, Arrays.asList(identifiers));
    }

    public IdentifierMatcher(Operation operation, String nodeName, Collection<? extends Identifier<T, ?>> identifiers)
    {
        super(operation, nodeName);

        for(Identifier<?, ?> id : identifiers)
        {
            if(id == null)
                continue;

            if(id instanceof Id)
                this.ids.add(id.get());
            else if(id instanceof ExternalId)
                this.externalIds.add(((ExternalId) id).get());
            else
                throw new UnsupportedOperationException("Unsupported Identifier class: " + id.getClass());
        }

        if(this.ids.size() == 0 && this.externalIds.size() == 0)
        {
            new Exception().printStackTrace(System.out);
            throw new IllegalArgumentException("IdentifierMatcher would match all nodes");
        }
    }

    @Override
    protected String where()
    {
        String match = "";

        if(this.ids.size() > 0)
        {
            match += Matcher.equality(this.qualify(FIELD_IDS), this.parameterize(FIELD_IDS), this.ids);

            if(this.externalIds.size() > 0)
                match += " OR ";
        }

        if(this.externalIds.size() > 0)
            match += Matcher.equality(this.qualify(FIELD_EXTERNAL_IDS), this.parameterize(FIELD_EXTERNAL_IDS), this.externalIds);

        return match;
    }

    @Override
    public void addParams(JsonObject params)
    {
        this.addParam(params, this.parameter(FIELD_IDS), Matcher.getCollectionOrOnlyElement(this.ids));
        this.addParam(params, this.parameter(FIELD_EXTERNAL_IDS), Matcher.getCollectionOrOnlyElement(this.externalIds));
    }
}
