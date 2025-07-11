package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.entcore.broker.api.dto.BaseResponseDTO;

public class BackupRelationhipResponseDTO extends BaseResponseDTO {

  @JsonCreator
  public BackupRelationhipResponseDTO(@JsonProperty("success") boolean success, @JsonProperty("message") String message) {
    super(success, message);
  }
}
