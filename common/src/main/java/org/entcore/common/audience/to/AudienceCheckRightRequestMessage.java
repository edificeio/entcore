package org.entcore.common.audience.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

@JsonIgnoreProperties
public class AudienceCheckRightRequestMessage {
  private final String appName;
  private final String resourceType;
  private final String userId;
  private final Set<String> resourceIds;

  @JsonCreator
  public AudienceCheckRightRequestMessage(@JsonProperty("appName") final String appName,
                                          @JsonProperty("resourceType") final String resourceType,
                                          @JsonProperty("userId") final String userId,
                                          @JsonProperty("resourceIds") final Set<String> resourceIds) {
    this.appName = appName;
    this.resourceType = resourceType;
    this.userId = userId;
    this.resourceIds = resourceIds;
  }

  public String getAppName() {
    return appName;
  }

  public String getResourceType() {
    return resourceType;
  }

  public String getUserId() {
    return userId;
  }

  public Set<String> getResourceIds() {
    return resourceIds;
  }
}
