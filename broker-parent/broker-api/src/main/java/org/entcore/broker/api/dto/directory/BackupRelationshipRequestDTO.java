package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BackupRelationshipRequestDTO {
  private final String userId;
  private final boolean backupAdmlGroups;

  @JsonCreator
  public BackupRelationshipRequestDTO(@JsonProperty("userId") final String userId,
                                      @JsonProperty("backupAdmlGroups") final boolean backupAdmlGroups) {
    this.userId = userId;
    this.backupAdmlGroups = backupAdmlGroups;
  }

  public String getUserId() {
    return userId;
  }

  public boolean isBackupAdmlGroups() {
    return backupAdmlGroups;
  }
}
