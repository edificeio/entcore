package org.entcore.audience.view.model;

public class ViewsCounterPerProfile {
  private final String profile;
  private final int counter;

  public ViewsCounterPerProfile(String profile, int counter) {
    this.profile = profile;
    this.counter = counter;
  }

  public String getProfile() {
    return profile;
  }

  public int getCounter() {
    return counter;
  }
}
