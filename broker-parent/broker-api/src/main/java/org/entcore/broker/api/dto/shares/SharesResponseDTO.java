

package org.entcore.broker.api.dto.shares;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * This class represents a response to a request for shares.
 * It contains the ID, kind, and permissions associated to a user or a group.
 */
public class SharesResponseDTO {
  public enum Kind {
    Group, User
  }
  /**
   * The ID of the user or group associated with the shares.
   * It is a unique identifier for the user or group in the directory.
   */
  private final String id;
  /**
   * The kind of the entity associated with the shares.
   * It can be either a group or a user.
   */
  private final Kind kind;
  /**
   * A list of permissions associated with the shares.
   * Each permission is represented by a string.
   */
  private final List<String> permissions;

  @JsonCreator
  public SharesResponseDTO(@JsonProperty("id") String id, @JsonProperty("kind") Kind kind, @JsonProperty("permissions") List<String> permissions) {
    this.id = id;
    this.kind = kind;
    this.permissions = permissions;
  }

  /**
   * Gets the ID of the user or group associated with the shares.
   * @return The ID of the user or group. It is a unique identifier for the user or group in the directory.
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the kind of the entity associated with the shares.
   * @return The kind of the entity. It can be either a group or a user.
   */
  public Kind getKind() {
    return kind;
  }

  /**
   * Gets the list of permissions associated with the shares.
   * @return A list of permissions represented by strings.
   */
  public List<String> getPermissions() {
    return permissions;
  }
}
