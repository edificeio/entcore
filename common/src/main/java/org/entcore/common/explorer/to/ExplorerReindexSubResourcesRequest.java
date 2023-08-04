package org.entcore.common.explorer.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.Set;

/**
 * Filter for sub resources to be reindexed in EUR.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class ExplorerReindexSubResourcesRequest {
    /** Earliest creation date of the sub-resources to reindex.*/
    private final Date from;
    /** Latest creation date of the sub-resources to reindex.*/
    private final Date to;
    /** Ids of the parent resources whose sub-resources should be reindexed.*/
    private final Set<String> parentIds;
    /** Ids of the sub-resources to reindex.*/
    private final Set<String> ids;

    @JsonCreator
    public ExplorerReindexSubResourcesRequest(@JsonProperty("from") final Date from,
                                              @JsonProperty("to") final Date to,
                                              @JsonProperty("parentIds") final Set<String> parentIds,
                                              @JsonProperty("ids") final Set<String> ids) {
        this.from = from;
        this.to = to;
        this.parentIds = parentIds;
        this.ids = ids;
    }

    public Date getFrom() {
        return from;
    }

    public Date getTo() {
        return to;
    }

    public Set<String> getParentIds() {
        return parentIds;
    }

    public Set<String> getIds() {
        return ids;
    }

    @Override
    public String toString() {
        return "ExplorerReindexSubResourcesRequest{" +
                "from=" + from +
                ", to=" + to +
                ", parentIds=" + parentIds +
                ", ids=" + ids +
                '}';
    }
}
