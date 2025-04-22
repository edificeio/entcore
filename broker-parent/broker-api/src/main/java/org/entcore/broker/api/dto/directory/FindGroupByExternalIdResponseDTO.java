package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents a response to a request to find a group by its external ID in the directory.
 * It contains the group information if found, or null if not found.
 */
public class FindGroupByExternalIdResponseDTO {

  /**
   * The group information if found, or null if not found.
   */
  private final GroupDTO group;

  /**
   * Creates a new instance of FindGroupByExternalIdResponseDTO.
   *
   * @param group The group information if found, or null if not found.
   */
  @JsonCreator
  public FindGroupByExternalIdResponseDTO(@JsonProperty("group") GroupDTO group) {
    this.group = group;
  }

  /**
   * Gets the group information if found.
   * @return The group information if found, or null if not found.
   */
  public GroupDTO getGroup() {
    return group;
  }

  @Override
  public String toString() {
    return "FindGroupByExternalIdResponseDTO{" +
            " group=" + group +
            '}';
  }
}