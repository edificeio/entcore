package org.entcore.common.explorer.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import static java.util.Collections.emptySet;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown=true)
public class ExplorerReindexResourcesRequest {
    private final Date from;
    private final Date to;
    private final Set<String> apps;
    private final boolean includeFolders;
    private final Set<String> ids;


    @JsonCreator
    public ExplorerReindexResourcesRequest(@JsonProperty("from") final Date from,
                                           @JsonProperty("to") final Date to,
                                           @JsonProperty("apps") final Set<String> apps,
                                           @JsonProperty("includeFolders") final boolean includeFolders,
                                           @JsonProperty("ids") final Set<String> ids) {
        this.from = from;
        this.to = to;
        this.apps = apps;
        this.includeFolders = includeFolders;
        this.ids = ids;
    }
    public ExplorerReindexResourcesRequest(final Set<String> ids) {
        this(null, null, null, false, ids);
    }
    public ExplorerReindexResourcesRequest() {
        this(null, null, null, false, null);
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

    @Override
    public String toString() {
        return "ExplorerReindexRequest{" +
                "from=" + from +
                ", to=" + to +
                ", apps=" + apps +
                ", includeFolders=" + includeFolders +
                ", ids=" + ids +
                '}';
    }
}
