package org.entcore.common.explorer.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExplorerResourceDetailsQuery {
  private final String resourceId;
  private final String application;
  private final String userId;
  @JsonCreator
  public ExplorerResourceDetailsQuery(@JsonProperty("resourceId") final String resourceId,
                                      @JsonProperty("application") final String application,
                                      @JsonProperty("userId") final String userId) {
    this.resourceId = resourceId;
    this.application = application;
    this.userId = userId;
  }

  public String getResourceId() {
    return resourceId;
  }

  public String getApplication() {
    return application;
  }

  public String getUserId() {
    return userId;
  }
}
