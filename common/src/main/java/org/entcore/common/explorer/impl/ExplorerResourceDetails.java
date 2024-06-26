package org.entcore.common.explorer.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExplorerResourceDetails {
  private final long id;
  private final String resourceId;
  private final String application;
  private final String resourceType;
  private final String entityType;
  private final Long parentId;

  @JsonCreator
  public ExplorerResourceDetails(@JsonProperty("id") final long id,
                                 @JsonProperty("resourceId") final String resourceId,
                                 @JsonProperty("application") final String application,
                                 @JsonProperty("resourceType") final String resourceType,
                                 @JsonProperty("entityType") final String entityType,
                                 @JsonProperty("parentId") final Long parentId) {
    this.id = id;
    this.resourceId = resourceId;
    this.application = application;
    this.resourceType = resourceType;
    this.entityType = entityType;
    this.parentId = parentId;
  }

  public long getId() {
    return id;
  }

  public String getResourceId() {
    return resourceId;
  }

  public String getApplication() {
    return application;
  }

  public String getResourceType() {
    return resourceType;
  }

  public String getEntityType() {
    return entityType;
  }

  public Long getParentId() {
    return parentId;
  }
}
