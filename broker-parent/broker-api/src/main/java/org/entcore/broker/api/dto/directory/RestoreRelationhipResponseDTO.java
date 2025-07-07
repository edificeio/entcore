package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.entcore.broker.api.dto.BaseResponseDTO;

public class RestoreRelationhipResponseDTO extends BaseResponseDTO {
  @JsonCreator
  public RestoreRelationhipResponseDTO(@JsonProperty("success") final boolean success,
                                       @JsonProperty("message") final String message) {
    super(success, message);
  }
}
