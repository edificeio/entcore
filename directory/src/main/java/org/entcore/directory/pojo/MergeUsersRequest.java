package org.entcore.directory.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MergeUsersRequest {
    private final String target;
    private final String source;
    private final boolean keepRelations;

    @JsonCreator
    public MergeUsersRequest(@JsonProperty("target") final String target,
                             @JsonProperty("source") final String source,
                             @JsonProperty("keepRelations") final boolean keepRelations) {
        this.target = target;
        this.source = source;
        this.keepRelations = keepRelations;
    }

    public String getTarget() {
        return target;
    }

    public String getSource() {
        return source;
    }

    public boolean isKeepRelations() {
        return keepRelations;
    }
}
