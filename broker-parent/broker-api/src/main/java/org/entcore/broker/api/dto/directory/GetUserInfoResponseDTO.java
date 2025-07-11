package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class GetUserInfoResponseDTO {
  private final List<GetUserInfoItemDTO> info;

  @JsonCreator
  public GetUserInfoResponseDTO(@JsonProperty("info") final List<GetUserInfoItemDTO> info) {
    this.info = info;
  }

  public List<GetUserInfoItemDTO> getInfo() {
    return info;
  }
}
