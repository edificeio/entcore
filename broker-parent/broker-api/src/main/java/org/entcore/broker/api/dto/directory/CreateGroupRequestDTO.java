package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
/**
 * This class represents a request to create a new group in the directory.
 * It contains the external ID and name of the group to be created.
 */
public class CreateGroupRequestDTO {
  /**
   * The external ID of the group to be created.
   * It is a unique identifier for the group in the directory. It can be null if the group does not have an external ID.
   */
  private final String externalId;
  /**
   * The name of the group to be created.
   * It is a human-readable name for the group. It cannot be null or empty.
   */
  private final String name;

  @JsonCreator
  public CreateGroupRequestDTO(@JsonProperty("externalId") String externalId, @JsonProperty("name") String name) {
    this.externalId = externalId;
    this.name = name;
  }

  /**
   * Gets the external ID of the group to be created.
   * @return The external ID of the group. It can be null if the group does not have an external ID.
   */
  public String getExternalId() {
    return externalId;
  }

  /**
   * Gets the name of the group to be created.
   * @return The name of the group. It cannot be null or empty.
   */
  public String getName() {
    return name;
  }
}
