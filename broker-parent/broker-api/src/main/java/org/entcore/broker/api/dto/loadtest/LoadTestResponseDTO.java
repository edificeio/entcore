package org.entcore.broker.api.dto.loadtest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LoadTestResponseDTO {
  private final String payload;
  private final long startWaitingAt;
  private final long startProcessingAt;
  private final long elapsedTime;

  @JsonCreator
  public LoadTestResponseDTO(
    @JsonProperty("payload") String payload,
    @JsonProperty("startWaitingAt") long startWaitingAt,
    @JsonProperty("startProcessingAt") long startProcessingAt,
    @JsonProperty("elapsedTime") long elapsedTime) {
    this.payload = payload;
    this.startWaitingAt = startWaitingAt;
    this.startProcessingAt = startProcessingAt;
    this.elapsedTime = elapsedTime;
  }

  public String getPayload() {
    return payload;
  }

  public long getElapsedTime() {
    return elapsedTime;
  }

  public long getStartWaitingAt() {
    return startWaitingAt;
  }

  public long getStartProcessingAt() {
    return startProcessingAt;
  }
}
