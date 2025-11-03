package org.entcore.broker.client;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.broker.listener.BrokerListener;

/**
 * No-operation implementation of BrokerClient that does nothing and returns successful Futures.
 * This implementation is used when broker-type is set to "none" and ensures no errors or timeouts
 * are generated for the caller.
 */
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

public class NoOpBrokerClient implements BrokerClient {

  // Logger for debug/info output
  private static final Logger log = LoggerFactory.getLogger(NoOpBrokerClient.class);

  private final Vertx vertx;
  private MessageConsumer<JsonObject> publishConsumer;
  private MessageConsumer<JsonObject> requestConsumer;

  /**
   * Constructor for NoOpBrokerClient.
   * @param vertx Vert.x instance used for event bus consumption
   */
  public NoOpBrokerClient(Vertx vertx) {
    this.vertx = vertx;
  }

  /**
   * Starts the NoOpBrokerClient. Registers a consumer for 'broker.publish' that always replies with success.
   */
  @Override
  public Future<Void> start() {
    log.info("NoOpBrokerClient started - no actual broker operations will be performed");
    // Unregister previous consumer if any
    if (publishConsumer != null) {
      publishConsumer.unregister();
    }
    publishConsumer = vertx.eventBus().consumer("broker.publish", message -> {
      log.debug("NoOpBrokerClient received broker.publish for subject: {} (replying with dummy success)", message.body().getString("subject"));
      message.reply(null); // Always reply with success (null)
    });
      requestConsumer = vertx.eventBus().consumer("broker.request", message -> {
      log.debug("NoOpBrokerClient received broker.request for subject: {} (replying with dummy success)", message.body().getString("subject"));
      message.reply(null); // Always reply with success (null)
    });
    return Future.succeededFuture();
  }

  /**
   * Ignores any message sent and always returns a succeeded Future.
   */
  @Override
  public <K> Future<Void> sendMessage(String subject, K message) {
    log.warn("NoOpBrokerClient.sendMessage called for subject: {} - message ignored", subject);
    return Future.succeededFuture();
  }

  /**
   * Ignores any request and always returns a succeeded Future with null response.
   */
  @Override
  public <K, V> Future<V> request(String subject, K message) {
    log.warn("NoOpBrokerClient.request called for subject: {} - returning null response", subject);
    return Future.succeededFuture(null);
  }

  /**
   * Ignores any request and always returns a succeeded Future with null response, regardless of timeout.
   */
  @Override
  public <K, V> Future<V> request(String subject, K message, long timeout) {
    log.warn("NoOpBrokerClient.request called for subject: {} with timeout: {} - returning null response", subject, timeout);
    return Future.succeededFuture(null);
  }

  /**
   * Ignores unsubscribe requests and always returns a succeeded Future.
   */
  @Override
  public Future<Void> unsubscribe(String subject) {
    log.warn("NoOpBrokerClient.unsubscribe called for subject: {} - no operation performed", subject);
    return Future.succeededFuture();
  }

  /**
   * Ignores subscription requests and always returns a succeeded Future.
   */
  @Override
  public <K, V> Future<Void> subscribe(String subject, BrokerListener<K, V> listener) {
    log.warn("NoOpBrokerClient.subscribe called for subject: {} - no actual subscription performed", subject);
    return Future.succeededFuture();
  }

  /**
   * Closes the NoOpBrokerClient. Unregisters the event bus consumer and always succeeds.
   */
  @Override
  public Future<Void> close() {
    log.info("NoOpBrokerClient closed - no cleanup needed");
    if (publishConsumer != null) {
      publishConsumer.unregister();
      publishConsumer = null;
    }
    if (requestConsumer != null) {
      requestConsumer.unregister();
      requestConsumer = null;
    }
    return Future.succeededFuture();
  }
}