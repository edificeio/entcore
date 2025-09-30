package org.entcore.broker.client;

import io.vertx.core.Vertx;

/**
 * Factory class for creating BrokerClient instances based on configuration.
 * Returns the appropriate implementation depending on the "broker-type" config value.
 */
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class BrokerClientFactory {

  private static final Logger log = LoggerFactory.getLogger(BrokerClientFactory.class);

  /**
   * Returns a BrokerClient instance according to the broker-type configuration.
   * Supported types:
   *   - "nats": returns NATSBrokerClient
   *   - "none": returns NoOpBrokerClient (does nothing)
   *   - any other value: returns RESTBrokerClient
   *
   * @param vertx Vert.x instance
   * @return BrokerClient implementation
   */
  public static BrokerClient getClient(final Vertx vertx) {
    final String brokerType = vertx.getOrCreateContext().config().getString("broker-type", "rest");
    log.debug("Broker type selected: {}", brokerType);
    if ("nats".equalsIgnoreCase(brokerType)) {
      return new NATSBrokerClient(vertx);
    } else if ("none".equalsIgnoreCase(brokerType)) {
      log.warn("Broker is disabled (broker-type=none). No broker operations will be performed.");
      return new NoOpBrokerClient(vertx);
    } else {
      return new RESTBrokerClient(vertx);
    }
  }
}
