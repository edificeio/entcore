package org.entcore.broker.api.dto.loadtest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LoadTestRequestDTO {
  /** Data that comes from the client, used to simulate a varying payload size. */
  private final String payload;
  /** Delay in milliseconds before the response is sent, simulating network latency. <br/>If delay < 0, the service will never reply.*/
  private final long delay;
  /** Size of the response in bytes, used to simulate varying response sizes.*/
  private final long responseSize;

  @JsonCreator
  public LoadTestRequestDTO(@JsonProperty("payload") String payload,
                            @JsonProperty("delay") long delay,
                            @JsonProperty("responseSize") long responseSize) {
    this.payload = payload;
    this.delay = delay;
    this.responseSize = responseSize;
  }

  public String getPayload() {
    return payload;
  }

  public long getDelay() {
    return delay;
  }

  public long getResponseSize() {
    return responseSize;
  }
}
