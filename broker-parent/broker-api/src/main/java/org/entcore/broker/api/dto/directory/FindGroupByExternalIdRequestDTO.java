package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import java.beans.Transient;

/**
 * This class represents a request to find a group by its external ID in the directory.
 * It contains the external ID of the group to be found.
 * The external ID is typically used when integrating with external systems.
 */
public class FindGroupByExternalIdRequestDTO {
  /**
   * The external ID of the group to be found.
   * This is a unique identifier for the group in an external system.
   */
  private final String externalId;

  /**
   * Creates a new instance of FindGroupByExternalIdRequestDTO.
   *
   * @param externalId The external ID of the group to be found.
   */
  @JsonCreator
  public FindGroupByExternalIdRequestDTO(@JsonProperty("externalId") String externalId) {
    this.externalId = externalId;
  }

  /**
   * Gets the external ID of the group to be found.
   * @return The external ID of the group.
   */
  public String getExternalId() {
    return externalId;
  }

  /**
   * Validates the request to ensure that the externalId is not blank.
   * @return true if the request is valid, false otherwise.
   */
  @Transient()
  public boolean isValid() {
    return !StringUtils.isBlank(externalId);
  }

  @Override
  public String toString() {
    return "FindGroupByExternalIdRequestDTO{" +
            "externalId='" + externalId + '\'' +
            '}';
  }
}