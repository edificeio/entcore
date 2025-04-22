package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents a group in the directory.
 * It contains the basic information about a group such as its ID, external ID, name, and display name.
 */
public class GroupDTO {
  /**
   * The ID of the group.
   * It is a unique identifier for the group in the directory.
   */
  private final String id;

  /**
   * The name of the group.
   * This is the technical name of the group.
   */
  private final String name;

  /**
   * Creates a new instance of GroupDTO.
   *
   * @param id The ID of the group.
   * @param name The name of the group.
   */
  @JsonCreator
  public GroupDTO(
          @JsonProperty("id") String id,
          @JsonProperty("name") String name) {
    this.id = id;
    this.name = name;
  }

  /**
   * Gets the ID of the group.
   * @return The ID of the group.
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the name of the group.
   * @return The name of the group.
   */
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return "GroupDTO{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            '}';
  }
}