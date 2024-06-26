package org.entcore.common.explorer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceDuplicatedMessage {
  private final String application;
  private final String originalResourceId;
  private final String duplicatedResourceId;

  @JsonCreator
  public ResourceDuplicatedMessage(@JsonProperty("application") final String application,
                                   @JsonProperty("originalResourceId") final String originalResourceId,
                                   @JsonProperty("duplicatedResourceId") final String duplicatedResourceId) {
    this.application = application;
    this.originalResourceId = originalResourceId;
    this.duplicatedResourceId = duplicatedResourceId;
  }

  public String getOriginalResourceId() {
    return originalResourceId;
  }

  public String getDuplicatedResourceId() {
    return duplicatedResourceId;
  }

  public String getApplication() {
    return application;
  }
}
