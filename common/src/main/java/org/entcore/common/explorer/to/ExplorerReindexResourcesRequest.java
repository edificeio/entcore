package org.entcore.common.explorer.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.Set;

/**
 * Filter for resources to be reindexed in EUR.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class ExplorerReindexResourcesRequest {
    /** Earliest creation date of the sub-resources to reindex.*/
    private final Date from;
    /** Latest creation date of the sub-resources to reindex.*/
    private final Date to;
    /** Identifier of the apps of the resources to reindex.*/
    private final Set<String> apps;
    /** {@code true} if folders should also be reindexed.*/
    private final boolean includeFolders;
    /** Ids of the resources to reindex.*/
    private final Set<String> ids;
    /** Ingest state of resources to reindex.*/
    private final Set<String> states;


    @JsonCreator
    public ExplorerReindexResourcesRequest(@JsonProperty("from") final Date from,
                                           @JsonProperty("to") final Date to,
                                           @JsonProperty("apps") final Set<String> apps,
                                           @JsonProperty("includeFolders") final boolean includeFolders,
                                           @JsonProperty("ids") final Set<String> ids,
                                           @JsonProperty("states") final Set<String> states) {
        this.from = from;
        this.to = to;
        this.apps = apps;
        this.includeFolders = includeFolders;
        this.ids = ids;
        this.states = states;
    }
    public ExplorerReindexResourcesRequest(final Set<String> ids) {
        this(null, null, null, false, ids, null);
    }
    public ExplorerReindexResourcesRequest() {
        this(null, null, null, false, null, null);
    }

    public Date getFrom() {
        return from;
    }

    public Date getTo() {
        return to;
    }

    public Set<String> getApps() {
        return apps;
    }

    public boolean isIncludeFolders() {
        return includeFolders;
    }

    public Set<String> getIds() {
        return ids;
    }

    public Set<String> getStates() {
        return states;
    }

    @Override
    public String toString() {
        return "ExplorerReindexResourcesRequest{" +
            "from=" + from +
            ", to=" + to +
            ", apps=" + apps +
            ", includeFolders=" + includeFolders +
            ", ids=" + ids +
            ", states=" + states +
            '}';
    }
}
