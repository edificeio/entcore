package org.entcore.audience.view.model;

public class ResourceViewCounter {
  private final String resourceId;
  private final int viewCounter;

  public ResourceViewCounter(String resourceId, int viewCounter) {
    this.resourceId = resourceId;
    this.viewCounter = viewCounter;
  }

  public String getResourceId() {
    return resourceId;
  }

  public int getViewCounter() {
    return viewCounter;
  }
}
