package org.entcore.broker.api.dto.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;

/**
 * DTO representing a notification about deleted resources.
 */
public class ResourcesDeletedDTO {
    /**
     * List of resource IDs that have been deleted
     */
    private final List<String> resourceIds;
    
    /**
     * Type of the resources that were deleted (e.g., "blog", "exercise", "news", etc.)
     */
    private final String resourceType;

    /**
     * Creates a new ResourcesDeletedDTO with the specified resources
     * 
     * @param resourceIds List of IDs of deleted resources
     * @param resourceType Type of the resources that were deleted
     */
    @JsonCreator
    public ResourcesDeletedDTO(
            @JsonProperty("resourceIds") List<String> resourceIds,
            @JsonProperty("resourceType") String resourceType) {
        this.resourceIds = resourceIds;
        this.resourceType = resourceType;
    }

    /**
     * Creates a new ResourcesDeletedDTO for a single resource
     * 
     * @param resourceId ID of the deleted resource
     * @param resourceType Type of the resource that was deleted
     * @return A new ResourcesDeletedDTO with a single resource ID
     */
    public static ResourcesDeletedDTO forSingleResource(String resourceId, String resourceType) {
        return new ResourcesDeletedDTO(Collections.singletonList(resourceId), resourceType);
    }

    /**
     * @return List of resource IDs that have been deleted
     */
    public List<String> getResourceIds() {
        return resourceIds;
    }

    /**
     * @return Type of the resources that were deleted
     */
    public String getResourceType() {
        return resourceType;
    }
    
    @Override
    public String toString() {
        return "ResourcesDeletedDTO{" +
                "resourceIds=" + resourceIds +
                ", resourceType='" + resourceType + '\'' +
                '}';
    }
}