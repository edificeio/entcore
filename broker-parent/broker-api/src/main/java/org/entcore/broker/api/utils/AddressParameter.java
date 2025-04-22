package org.entcore.broker.api.utils;

/**
 * Association between a parameter name and its value to be used while declaring a proxyfier broker listener.
 */
public class AddressParameter {
  private final String name;
  private final String value;

  public AddressParameter(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }
}
