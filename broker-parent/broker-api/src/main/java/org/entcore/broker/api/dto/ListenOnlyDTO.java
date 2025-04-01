package org.entcore.broker.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ListenOnlyDTO {
  private final String userId;
  private final long timestamp;

  @JsonCreator
  public ListenOnlyDTO(@JsonProperty("userId") final String userId,
                       @JsonProperty("timestamp") final long timestamp) {
    this.userId = userId;
    this.timestamp = timestamp;
  }

  public String getUserId() {
    return userId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return "ListenOnlyDTO{" +
      "userId='" + userId + '\'' +
      ", timestamp=" + timestamp +
      '}';
  }
}