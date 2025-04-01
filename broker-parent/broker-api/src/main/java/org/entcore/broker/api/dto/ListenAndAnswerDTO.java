package org.entcore.broker.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ListenAndAnswerDTO {
  private final String userId;
  private final String jobId;

  @JsonCreator
  public ListenAndAnswerDTO(@JsonProperty("userId") final String userId,
                            @JsonProperty("jobId") final String jobId) {
    this.userId = userId;
    this.jobId = jobId;
  }

  public String getUserId() {
    return userId;
  }

  public String getJobId() {
    return jobId;
  }

  @Override
  public String toString() {
    return "ListenAndAnswerDTO{" +
      "userId='" + userId + '\'' +
      ", jobId='" + jobId + '\'' +
      '}';
  }
}
