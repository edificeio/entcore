package org.entcore.broker.api.dto.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonArray;
import java.util.Collections;
import java.util.List;

/**
 * DTO representing a notification about resources that had their shares changed
 */
public class ResourceSharesChangedDTO {
    /**
     * List of resource IDs with updated sharing settings
     */
    private final List<String> resourceIds;
    
    /**
     * Type of the resources with updated sharing settings
     */
    private final String resourceType;
    
    /**
     * JSON array containing the new share settings for the resources
     */
    private final JsonArray shares;

    /**
     * Creates a new ResourceSharesChangedDTO
     * 
     * @param resourceIds List of IDs of resources with updated shares
     * @param resourceType Type of the resources with updated sharing settings
     * @param shares JSON array with the sharing information
     */
    @JsonCreator
    public ResourceSharesChangedDTO(
            @JsonProperty("resourceIds") List<String> resourceIds,
            @JsonProperty("resourceType") String resourceType,
            @JsonProperty("shares") JsonArray shares) {
        this.resourceIds = resourceIds;
        this.resourceType = resourceType;
        this.shares = shares;
    }

    /**
     * Creates a new ResourceSharesChangedDTO for a single resource
     * 
     * @param resourceId ID of the resource with updated shares
     * @param resourceType Type of the resource with updated sharing settings
     * @param shares JSON array with the sharing information
     * @return A new ResourceSharesChangedDTO with a single resource ID
     */
    public static ResourceSharesChangedDTO forSingleResource(String resourceId, String resourceType, JsonArray shares) {
        return new ResourceSharesChangedDTO(Collections.singletonList(resourceId), resourceType, shares);
    }

    /**
     * @return List of resource IDs with updated sharing settings
     */
    public List<String> getResourceIds() {
        return resourceIds;
    }

    /**
     * @return Type of the resources with updated sharing settings
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * @return JSON array containing the new share settings
     */
    public JsonArray getShares() {
        return shares;
    }
    
    @Override
    public String toString() {
        return "ResourceSharesChangedDTO{" +
                "resourceIds=" + resourceIds +
                ", resourceType='" + resourceType + '\'' +
                ", shares=" + shares +
                '}';
    }
}