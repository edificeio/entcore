package org.entcore.broker.nats.dummy;

import java.util.Map;

public class DeeplyNestedClass {
  private final String deeplyNestedField;
  private final Map<String, String> totoro;

  public DeeplyNestedClass(String deeplyNestedField, Map<String, String> totoro) {
    this.deeplyNestedField = deeplyNestedField;
    this.totoro = totoro;
  }

  public String getDeeplyNestedField() {
    return deeplyNestedField;
  }

  public Map<String, String> getTotoro() {
    return totoro;
  }
}
