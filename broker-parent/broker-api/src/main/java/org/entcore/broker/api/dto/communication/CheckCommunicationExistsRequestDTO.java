package org.entcore.broker.api.dto.communication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CheckCommunicationExistsRequestDTO {
  private final String queryUserId;
  private final String userId;

  @JsonCreator
  public CheckCommunicationExistsRequestDTO(@JsonProperty("queryUserId") final String queryUserId,
                                            @JsonProperty("userId") final String userId) {
    this.queryUserId = queryUserId;
    this.userId = userId;
  }

  public String getQueryUserId() {
    return queryUserId;
  }

  public String getUserId() {
    return userId;
  }
}
