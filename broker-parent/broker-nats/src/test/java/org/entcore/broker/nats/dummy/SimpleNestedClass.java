package org.entcore.broker.nats.dummy;

public class SimpleNestedClass {
  private final String name;
  private final Boolean myBool;

  public SimpleNestedClass(String name, Boolean myBool) {
    this.name = name;
    this.myBool = myBool;
  }

  public String getName() {
    return name;
  }

  public Boolean getMyBool() {
    return myBool;
  }
}
