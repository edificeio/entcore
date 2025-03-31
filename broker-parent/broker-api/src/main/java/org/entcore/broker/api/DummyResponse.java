package org.entcore.broker.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DummyResponse {
  private final String userId;
  private final String jobId;
  private final boolean success;

  @JsonCreator
  public DummyResponse(@JsonProperty("userId") final String userId,
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
    return "DummyResponse{" +
      "userId='" + userId + '\'' +
      ", jobId='" + jobId + '\'' +
      ", success=" + success +
      '}';
  }
}
