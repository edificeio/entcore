package org.entcore.common.schema.utils.matchers;

public abstract class NodeMatcher<T> extends Matcher
{
    private String nodeName;

    public NodeMatcher(String nodeName)
    {
        this(null, nodeName);
    }

    public NodeMatcher(Operation operation, String nodeName)
    {
        super(operation);
        this.nodeName = nodeName;
    }

    public void setNodeName(String nodeName)
    {
        this.nodeName = nodeName;
    }

    public String getNodeName()
    {
        return this.nodeName;
    }

    protected String qualify(String field)
    {
        this.checkNodeName();
        return this.nodeName + "." + field;
    }

    protected String parameter(String field)
    {
        this.checkNodeName();
        return this.nodeName + "_" + field;
    }

    protected String parameterize(String field)
    {
        this.checkNodeName();
        return "{" + this.parameter(field) + "}";
    }

    private void checkNodeName()
    {
        if(this.nodeName == null)
            throw new IllegalStateException("Node name is not defined");
    }
}
