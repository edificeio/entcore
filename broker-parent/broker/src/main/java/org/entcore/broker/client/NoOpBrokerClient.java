package org.entcore.broker.client;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.broker.listener.BrokerListener;

/**
 * No-operation implementation of BrokerClient that does nothing and returns successful Futures.
 * This implementation is used when broker-type is set to "none" and ensures no errors or timeouts
 * are generated for the caller.
 */
public class NoOpBrokerClient implements BrokerClient {

  // Logger for debug/info output
  private static final Logger log = LoggerFactory.getLogger(NoOpBrokerClient.class);

  private Vertx vertx;
  private MessageConsumer<?> publishConsumer;
  private MessageConsumer<?> requestConsumer;

  /**
   * Constructor for NoOpBrokerClient.
   */
  public NoOpBrokerClient(Vertx vertx) {
      this.vertx = vertx;
  }

  /**
   * Starts the NoOpBrokerClient. Does nothing and always succeeds.
   */
  @Override
  public Future<Void> start() {
    log.info("NoOpBrokerClient starting - registering event bus handlers");

    // Setup publish handler
    publishConsumer = vertx.eventBus().consumer("broker.publish", message -> {
        log.debug("NoOpBrokerClient.publish called - message ignored");
        message.reply(null);
    });

    // Setup request handler
    requestConsumer = vertx.eventBus().consumer("broker.request", message -> {
        log.debug("NoOpBrokerClient.request called - returning null response");
        message.reply(null);
    });

    log.info("NoOpBrokerClient started - no actual broker operations will be performed");
    return Future.succeededFuture();
  }

  /**
   * Ignores any message sent and always returns a succeeded Future.
   */
  @Override
  public <K> Future<Void> sendMessage(String subject, K message) {
    log.debug("NoOpBrokerClient.sendMessage called for subject: {} - message ignored", subject);
    return Future.succeededFuture();
  }

  /**
   * Ignores any request and always returns a succeeded Future with null response.
   */
  @Override
  public <K, V> Future<V> request(String subject, K message) {
    log.debug("NoOpBrokerClient.request called for subject: {} - returning null response", subject);
    return Future.succeededFuture(null);
  }

  /**
   * Ignores any request and always returns a succeeded Future with null response, regardless of timeout.
   */
  @Override
  public <K, V> Future<V> request(String subject, K message, long timeout) {
    log.debug("NoOpBrokerClient.request called for subject: {} with timeout: {} - returning null response", subject, timeout);
    return Future.succeededFuture(null);
  }

  /**
   * Ignores unsubscribe requests and always returns a succeeded Future.
   */
  @Override
  public Future<Void> unsubscribe(String subject) {
    log.debug("NoOpBrokerClient.unsubscribe called for subject: {} - no operation performed", subject);
    return Future.succeededFuture();
  }

  /**
   * Ignores subscription requests and always returns a succeeded Future.
   */
  @Override
  public <K, V> Future<Void> subscribe(String subject, BrokerListener<K, V> listener) {
    log.debug("NoOpBrokerClient.subscribe called for subject: {} - no actual subscription performed", subject);
    return Future.succeededFuture();
  }

  /**
   * Closes the NoOpBrokerClient. Does nothing and always succeeds.
   */
  @Override
  public Future<Void> close() {
    log.info("NoOpBrokerClient closing - unregistering event bus handlers");

    if (publishConsumer != null) {
        publishConsumer.unregister();
    }

    if (requestConsumer != null) {
        requestConsumer.unregister();
    }

    log.info("NoOpBrokerClient closed - no cleanup needed");
    return Future.succeededFuture();
  }
}