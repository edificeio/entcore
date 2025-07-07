package org.entcore.broker.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderDTO {
  private final String field;
  private final boolean ascending;

  @JsonCreator
  public OrderDTO(@JsonProperty("field") final String field,
                  @JsonProperty("ascending") final boolean ascending) {
    this.field = field;
    this.ascending = ascending;
  }

  public String getField() {
    return field;
  }

  public boolean isAscending() {
    return ascending;
  }
}
