package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import java.beans.Transient;

/**
 * This class represents a request to create a new group in the directory.
 * It contains the external ID, name, class ID, structure ID and optional label of the group to be created.
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
  
  /**
   * The ID of the class to which this group belongs.
   * It is a unique identifier for the class in the directory. It can be null if the group is not associated with a specific class.
   */
  private final String classId;
  
  /**
   * The ID of the structure (school or organization) to which this group belongs.
   * It is a unique identifier for the structure in the directory. It can be null if the group is not associated with a specific structure.
   */
  private final String structureId;

  /**
   * The label to add to the group in the Neo4j database.
   * This optional field can be used to categorize groups with a specific Neo4j label.
   * It can be null if no additional label is needed.
   */
  private final String label;

  /**
   * Creates a new instance of CreateGroupRequestDTO.
   *
   * @param externalId The external ID of the group to be created.
   * @param name The name of the group to be created.
   * @param classId The ID of the class to which this group belongs.
   * @param structureId The ID of the structure to which this group belongs.
   * @param label The optional Neo4j label to add to the group.
   */
  @JsonCreator
  public CreateGroupRequestDTO(
          @JsonProperty("externalId") String externalId, 
          @JsonProperty("name") String name,
          @JsonProperty("classId") String classId,
          @JsonProperty("structureId") String structureId,
          @JsonProperty("label") String label) {
    this.externalId = externalId;
    this.name = name;
    this.classId = classId;
    this.structureId = structureId;
    this.label = label;
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
  
  /**
   * Gets the ID of the class to which this group belongs.
   * @return The class ID. It can be null if the group is not associated with a specific class.
   */
  public String getClassId() {
    return classId;
  }
  
  /**
   * Gets the ID of the structure to which this group belongs.
   * @return The structure ID. It can be null, but it is recommended to provide this value.
   */
  public String getStructureId() {
    return structureId;
  }

  /**
   * Gets the Neo4j label to add to the group.
   * @return The label to add. It can be null if no additional label is needed.
   */
  public String getLabel() {
    return label;
  }

  /**
   * Validates the request to ensure that the name is not blank.
   * @return true if the request is valid, false otherwise.
   */
  @Transient()
  public boolean isValid() {
    if(StringUtils.isBlank(name)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "CreateGroupRequestDTO{" +
            "externalId='" + externalId + '\'' +
            ", name='" + name + '\'' +
            ", classId='" + classId + '\'' +
            ", structureId='" + structureId + '\'' +
            ", label='" + label + '\'' +
            '}';
  }
}