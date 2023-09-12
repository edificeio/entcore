package org.entcore.common.schema.utils.matchers;

import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.ArrayList;

public class CompoundMatcher extends Matcher
{
    public static enum Operator
    {
        AND, OR, XOR;

        protected String linker;

        Operator()
        {
            this.linker = " " + this.name() + " ";
        }
    };

    private final Operator operator;

    protected List<Matcher> matchers = new ArrayList<Matcher>();

    public CompoundMatcher(Matcher... matchers)
    {
        this(null, null, matchers);
    }

    public CompoundMatcher(Operator operator, Matcher... matchers)
    {
        this(null, operator, matchers);
    }

    public CompoundMatcher(Operation operation, Matcher... matchers)
    {
        this(operation, null, matchers);
    }

    public CompoundMatcher(Operation operation, Operator operator, Matcher... matchers)
    {
        super(operation);
        this.operator = operator != null ? operator : Operator.AND;

        for(Matcher m : matchers)
        {
            if(m instanceof UniversalMatcher) // Optimise (for db) away universal matchers
            {
                switch(this.operator)
                {
                    case AND:
                        continue;
                    case OR:
                        this.matchers = new ArrayList<Matcher>();
                        this.matchers.add(m);
                        break;
                    default:
                        // Do not optimise
                }
            }
            this.matchers.add(m);
        }
    }

    @Override
    protected String where()
    {
        String where = "";
        boolean first = true;

        for(Matcher m : this.matchers)
        {
            if(first == true)
            {
                where = m.match();
                first = false;
            }
            else
                where += this.operator.linker + m.match();
        }

        return where;
    }

    @Override
    public void addParams(JsonObject params)
    {
        for(Matcher m : this.matchers)
            m.addParams(params);
    }
}
