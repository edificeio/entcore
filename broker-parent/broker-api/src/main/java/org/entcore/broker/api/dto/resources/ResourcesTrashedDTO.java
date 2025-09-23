package org.entcore.broker.api.dto.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;

/**
 * DTO representing a notification about trashed resources.
 */
public class ResourcesTrashedDTO {
    /**
     * List of resource IDs that have been trashed
     */
    private final List<String> resourceIds;
    
    /**
     * Application name that owns the resources
     */
    private final String application;
    
    /**
     * Type of the resources that were trashed (e.g., "blog", "exercise", "news", etc.)
     */
    private final String resourceType;

    /**
     * Creates a new ResourcesTrashedDTO with the specified resources
     * 
     * @param resourceIds List of IDs of trashed resources
     * @param application Application name that owns the resources
     * @param resourceType Type of the resources that were trashed
     */
    @JsonCreator
    public ResourcesTrashedDTO(
            @JsonProperty("resourceIds") List<String> resourceIds,
            @JsonProperty("application") String application,
            @JsonProperty("resourceType") String resourceType) {
        this.resourceIds = resourceIds;
        this.application = application;
        this.resourceType = resourceType;
    }

    /**
     * Creates a new ResourcesTrashedDTO for a single resource
     * 
     * @param resourceId ID of the trashed resource
     * @param application Application name that owns the resource
     * @param resourceType Type of the resource that was trashed
     * @return A new ResourcesTrashedDTO with a single resource ID
     */
    public static ResourcesTrashedDTO forSingleResource(String resourceId, String application, String resourceType) {
        return new ResourcesTrashedDTO(Collections.singletonList(resourceId), application, resourceType);
    }

    /**
     * @return List of resource IDs that have been trashed
     */
    public List<String> getResourceIds() {
        return resourceIds;
    }

    /**
     * @return Application name that owns the resources
     */
    public String getApplication() {
        return application;
    }

    /**
     * @return Type of the resources that were trashed
     */
    public String getResourceType() {
        return resourceType;
    }
    
    @Override
    public String toString() {
        return "ResourcesTrashedDTO{" +
                "resourceIds=" + resourceIds +
                ", application='" + application + '\'' +
                ", resourceType='" + resourceType + '\'' +
                '}';
    }
}