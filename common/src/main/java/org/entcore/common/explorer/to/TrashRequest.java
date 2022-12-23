package org.entcore.common.explorer.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * Payload of requests to trash resources and folders of an application for a user.
 */
@JsonIgnoreProperties
public class TrashRequest {
    /** Application which handles the resources to trash. */
    private final String application;
    /** Explorer ids (i.e. numerical) of the resources to trash or untrash.*/
    private final Set<String> resourceIds;
    /** IDs of the folders to trash or untrash.*/
    private final Set<String> folderIds;

    @JsonCreator
    public TrashRequest(@JsonProperty("application") String application,
                        @JsonProperty("resourceIds") Set<String> resourceIds,
                        @JsonProperty("folderIds") Set<String> folderIds) {
        this.application = application;
        this.resourceIds = resourceIds;
        this.folderIds = folderIds;
    }

    public String getApplication() {
        return application;
    }

    public Set<String> getResourceIds() {
        return resourceIds;
    }

    public Set<String> getFolderIds() {
        return folderIds;
    }
}
