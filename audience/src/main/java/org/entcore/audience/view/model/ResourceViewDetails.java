package org.entcore.audience.view.model;

import java.util.List;

public class ResourceViewDetails {
  private final int viewsCounter;
  private final int uniqueViewsCounter;
  private final List<ViewsCounterPerProfile> uniqueViewsPerProfile;

  public ResourceViewDetails(int viewsCounter, int uniqueViewsCounter, List<ViewsCounterPerProfile> uniqueViewsPerProfile) {
    this.viewsCounter = viewsCounter;
    this.uniqueViewsCounter = uniqueViewsCounter;
    this.uniqueViewsPerProfile = uniqueViewsPerProfile;
  }

  public int getViewsCounter() {
    return viewsCounter;
  }

  public int getUniqueViewsCounter() {
    return uniqueViewsCounter;
  }

  public List<ViewsCounterPerProfile> getUniqueViewsPerProfile() {
    return uniqueViewsPerProfile;
  }
}
