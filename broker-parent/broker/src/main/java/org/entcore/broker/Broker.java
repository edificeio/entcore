package org.entcore.broker;

import io.vertx.core.Promise;
import org.entcore.broker.client.BrokerClient;
import org.entcore.broker.client.BrokerClientFactory;
import org.entcore.broker.controllers.BrokerController;
import org.entcore.common.http.BaseServer;

public class Broker extends BaseServer {

  private BrokerClient brokerClient;

  @Override
  public void start(final Promise<Void> startPromise) throws Exception {
    super.start(startPromise);
    addController(new BrokerController());
    brokerClient = BrokerClientFactory.getClient(vertx);
    brokerClient.start().onComplete(ar -> {
      if (ar.succeeded()) {
        log.info("Broker client started successfully.");
        startPromise.tryComplete();
      } else {
        log.error("Failed to start broker client.", ar.cause());
        startPromise.tryFail(ar.cause());
      }
    });
  }

  @Override
  public void stop(Promise<Void> stopPromise) throws Exception {
    log.info("Stopping Broker...");
    super.stop(stopPromise);
    brokerClient.close().onSuccess(e -> {
      log.info("Broker client stopped successfully.");
      stopPromise.tryComplete();
    }).onFailure(err -> {
      log.error("Failed to stop broker client.", err);
      stopPromise.tryFail(err);
    });
  }
}
