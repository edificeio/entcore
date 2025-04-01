package org.entcore.broker.client;

import io.vertx.core.Vertx;

public class BrokerClientFactory {
  public static BrokerClient getClient(final Vertx vertx) {
    final String brokerType = vertx.getOrCreateContext().config().getString("broker-type", "rest");
    if ("nats".equalsIgnoreCase(brokerType)) {
      return new NATSBrokerClient(vertx);
    } else {
      return new RESTBrokerClient(vertx);
    }
  }
}
