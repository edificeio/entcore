package org.entcore.infra.services;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonArray;

public class PartialResults {
  private final boolean partial;
  private final JsonArray results;
  @JsonCreator
  public PartialResults(@JsonProperty("partial") boolean partial,
                        @JsonProperty("results") JsonArray results) {
    this.partial = partial;
    this.results = results;
  }

  public boolean isPartial() {
    return partial;
  }

  public JsonArray getResults() {
    return results;
  }
}
