package org.entcore.broker.api.dto.audience;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.beans.Transient;
import java.util.Set;

/**
 * This class represents a request to check if a user has access to specific resources.
 * It contains information about the user, module, resource type, and resource IDs.
 */
public class CheckResourceAccessRequestDTO {
    /**
     * The module name of the resources to check.
     */
    private final String module;
    
    /**
     * The resource type to check.
     */
    private final String resourceType;
    
    /**
     * The user ID requesting access.
     */
    private final String userId;
    
    /**
     * The group IDs that the user belongs to.
     */
    private final Set<String> groupIds;
    
    /**
     * The resource IDs to check access for.
     */
    private final Set<String> resourceIds;

    /**
     * Creates a new instance of CheckResourceAccessRequestDTO.
     *
     * @param module The module name of the resources.
     * @param resourceType The type of resources.
     * @param userId The user ID requesting access.
     * @param groupIds The group IDs that the user belongs to.
     * @param resourceIds The resource IDs to check access for.
     */
    @JsonCreator
    public CheckResourceAccessRequestDTO(
            @JsonProperty("module") String module,
            @JsonProperty("resourceType") String resourceType,
            @JsonProperty("userId") String userId,
            @JsonProperty("groupIds") Set<String> groupIds,
            @JsonProperty("resourceIds") Set<String> resourceIds) {
        this.module = module;
        this.resourceType = resourceType;
        this.userId = userId;
        this.groupIds = groupIds;
        this.resourceIds = resourceIds;
    }

    public String getModule() {
        return module;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getUserId() {
        return userId;
    }

    public Set<String> getGroupIds() {
        return groupIds;
    }

    public Set<String> getResourceIds() {
        return resourceIds;
    }

    /**
     * Validates the request to ensure all required fields are present.
     *
     * @return true if the request is valid, false otherwise.
     */
    @Transient
    public boolean isValid() {
        return module != null && !module.isEmpty()
                && resourceType != null && !resourceType.isEmpty()
                && userId != null && !userId.isEmpty()
                && groupIds != null
                && resourceIds != null;
    }

    @Override
    public String toString() {
        return "CheckResourceAccessRequestDTO{" +
                "module='" + module + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", userId='" + userId + '\'' +
                ", groupIds=" + (groupIds != null ? groupIds.size() : 0) +
                ", resourceIds=" + (resourceIds != null ? resourceIds.size() : 0) +
                '}';
    }
}