package org.entcore.broker.api.dto.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO containing information about multiple resources.
 * This is returned by the resource broker when retrieving resource details.
 */
public class GetResourcesResponseDTO {
  /**
   * A list of resources with their detailed information.
   * Each resource is represented by a ResourceInfoDTO object.
   */
  private final List<ResourceInfoDTO> resources;
  
  /**
   * Creates a new GetResourcesResponseDTO with the given resources.
   * 
   * @param resources The list of resources with their detailed information.
   */
  @JsonCreator
  public GetResourcesResponseDTO(@JsonProperty("resources") List<ResourceInfoDTO> resources) {
    this.resources = resources;
  }
  
  /**
   * Gets the list of resources with their detailed information.
   * 
   * @return A list of ResourceInfoDTO objects representing the resources.
   */
  public List<ResourceInfoDTO> getResources() {
    return resources;
  }
  
  @Override
  public String toString() {
    return "GetResourcesResponseDTO{" +
            "resources=" + (resources != null ? resources.size() : 0) + " items" +
            '}';
  }
}