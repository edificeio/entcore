package org.entcore.broker.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DummyResponseDTO {
  private final String userId;
  private final String jobId;
  private final boolean success;

  @JsonCreator
  public DummyResponseDTO(@JsonProperty("userId") final String userId,
                          @JsonProperty("jobId") final String jobId,
                          @JsonProperty("success") final boolean success) {
    this.userId = userId;
    this.jobId = jobId;
    this.success = success;
  }

  public String getUserId() {
    return userId;
  }

  public String getJobId() {
    return jobId;
  }

  public boolean isSuccess() {
    return success;
  }

  @Override
  public String toString() {
    return "DummyResponseDTO{" +
      "userId='" + userId + '\'' +
      ", jobId='" + jobId + '\'' +
      ", success=" + success +
      '}';
  }
}
