package org.entcore.broker;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.broker.client.BrokerClient;
import org.entcore.broker.client.BrokerClientFactory;
import org.entcore.broker.controllers.BrokerController;
import org.entcore.common.http.BaseServer;

import java.util.ArrayList;
import java.util.List;

public class Broker extends BaseServer {

  private BrokerClient brokerClient;
  private MessageConsumer<JsonObject> migrationConsumer;


  @Override
  public void start(final Promise<Void> startPromise) throws Exception {
    super.start(startPromise);
    addController(new BrokerController());
    brokerClient = BrokerClientFactory.getClient(vertx);
    brokerClient.start().onSuccess(ar -> {
      log.info("Broker client started successfully.");
      this.migrationConsumer = vertx.eventBus().consumer("broker.proxy.legacy.migration", this::onMigrationMessage);
      startPromise.tryComplete();
    }).onFailure(ar -> {
      log.error("Failed to start broker client.", ar);
      startPromise.tryFail(ar);
    });
  }

  private void onMigrationMessage(final Message<JsonObject> message) {
    final JsonObject payload = message.body();
    final String subject = "broker.proxy.legacy.migration." + payload.getString("service") + "." + payload.getString("action");
    brokerClient.request(subject, payload.toString()).onSuccess(downstreamServiceResponse -> {
      message.reply(parseProxyfiedMigrationResponse(downstreamServiceResponse));
    }).onFailure(th -> {
      log.error("An error occurred while processing migration message: " + payload, th);
      message.fail(500, th.getMessage());
    });
  }

  private Object parseProxyfiedMigrationResponse(Object downstreamServiceResponse) {
    final String rawResponse;
    if (downstreamServiceResponse instanceof byte[]) {
      final byte[] responseBytes = (byte[]) downstreamServiceResponse;
      rawResponse = new String(responseBytes);
    } else if (downstreamServiceResponse instanceof String) {
      rawResponse = ((String) downstreamServiceResponse);
    } else {
      return JsonObject.mapFrom(downstreamServiceResponse);
    }
    if (rawResponse.charAt(0) == '{') {
      return new JsonObject(rawResponse);
    } else {
      return new JsonArray(rawResponse);
    }
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    log.info("Stopping Broker...");
    final List<Future<?>> futures = new ArrayList<>();
    futures.add(brokerClient.close());
    if (migrationConsumer != null) {
      futures.add(migrationConsumer.unregister());
    }
    Future.all(futures)
      .onSuccess(e -> stopPromise.tryComplete())
      .onFailure(err -> {
        log.error("Failed to unregister consumers", err);
        stopPromise.tryFail(err);
      });
  }
}
