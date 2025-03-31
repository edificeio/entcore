package org.entcore.broker.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NATSResponseDTO {
  private final Object response;
  private final String err;
  private final boolean disposed;
  private final String id;

  @JsonCreator
  public NATSResponseDTO(@JsonProperty("response") Object response,
                         @JsonProperty("err") String err,
                         @JsonProperty("disposed") boolean disposed,
                         @JsonProperty("response") String id) {
    this.response = response;
    this.err = err;
    this.disposed = disposed;
    this.id = id;
  }

  public Object getResponse() {
    return response;
  }

  public String getErr() {
    return err;
  }

  public boolean isDisposed() {
    return disposed;
  }

  public String getId() {
    return id;
  }
}