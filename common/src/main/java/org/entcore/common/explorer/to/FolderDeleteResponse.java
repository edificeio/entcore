package org.entcore.common.explorer.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FolderDeleteResponse {
    /**
     * Id to delete in explorer
     */
    private final Set<Long> deleted;

    @JsonCreator
    public FolderDeleteResponse(@JsonProperty("deleted") final Collection<Long> deleted) {
        this.deleted = new HashSet<>(deleted);
    }

    public Set<Long> getDeleted() {
        return deleted;
    }
}
