package org.entcore.common.audience.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

@JsonIgnoreProperties
public class AudienceCheckRightRequestMessage {
  private final String module;
  private final String resourceType;
  private final String userId;
  private final Set<String> userGroups;
  private final Set<String> resourceIds;

  @JsonCreator
  public AudienceCheckRightRequestMessage(@JsonProperty("module") final String module,
                                          @JsonProperty("resourceType") final String resourceType,
                                          @JsonProperty("userId") final String userId,
                                          @JsonProperty("userGroups") final Set<String> userGroups,
                                          @JsonProperty("resourceIds") final Set<String> resourceIds) {
    this.module = module;
    this.resourceType = resourceType;
    this.userId = userId;
    this.userGroups = userGroups;
    this.resourceIds = resourceIds;
  }

  public String getModule() {
    return module;
  }

  public String getResourceType() {
    return resourceType;
  }

  public String getUserId() {
    return userId;
  }

  public Set<String> getUserGroups() {
    return userGroups;
  }

  public Set<String> getResourceIds() {
    return resourceIds;
  }
}
