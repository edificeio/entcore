package org.entcore.common.audience.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

@JsonIgnoreProperties
public class NotifyResourceDeletionMessage {
    private final String module;

    private final String resourceType;

    private final Set<String> resourceIds;

    @JsonCreator
    public NotifyResourceDeletionMessage(@JsonProperty("module") String module,
                                         @JsonProperty("resourceType") String resourceType,
                                         @JsonProperty("resourceIds") Set<String> resourceIds) {
        this.module = module;
        this.resourceType = resourceType;
        this.resourceIds = resourceIds;
    }

    public String getModule() {
        return module;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Set<String> getResourceIds() {
        return resourceIds;
    }
}
