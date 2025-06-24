package org.entcore.broker.api.dto.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import java.beans.Transient;

import java.util.List;

/**
 * Request DTO for retrieving information about multiple resources.
 * Contains a list of resource IDs for which information is requested.
 */
public class GetResourcesRequestDTO {
  /**
   * The list of resource IDs or UUIDs to retrieve information for.
   * These are unique identifiers for the resources across applications.
   */
  private final List<String> resourceIds;
  
  /**
   * Creates a new GetResourcesRequestDTO with the given resource IDs.
   * 
   * @param resourceIds The list of resource IDs or UUIDs to retrieve information for.
   */
  @JsonCreator
  public GetResourcesRequestDTO(@JsonProperty("resourceIds") List<String> resourceIds) {
    this.resourceIds = resourceIds;
  }
  
  /**
   * Gets the list of resource IDs or UUIDs to retrieve information for.
   * 
   * @return The list of resource identifiers.
   */
  public List<String> getResourceIds() {
    return resourceIds;
  }
  
  /**
   * Validates whether this request contains valid data.
   * A valid request must have a non-null, non-empty list of resource IDs.
   * 
   * @return True if the request is valid, false otherwise.
   */
  @Transient()
  public boolean isValid() {
    if (resourceIds == null || resourceIds.isEmpty()) {
      return false;
    }
    
    // Check if any ID is null or empty
    for (String id : resourceIds) {
      if (StringUtils.isEmpty(id)) {
        return false;
      }
    }
    
    return true;
  }
  
  @Override
  public String toString() {
    return "GetResourcesRequestDTO{" +
            "resourceIds=" + resourceIds +
            '}';
  }
}