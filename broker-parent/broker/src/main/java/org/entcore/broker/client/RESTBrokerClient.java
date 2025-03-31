package org.entcore.broker.client;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.apache.commons.lang3.NotImplementedException;
import org.entcore.broker.listener.BrokerListener;

public class RESTBrokerClient implements BrokerClient {
  public RESTBrokerClient(final Vertx vertx) {
  }

  @Override
  public Future<Void> start() {
    throw new NotImplementedException();
  }

  @Override
  public <K> Future<Void> sendMessage(String subject, K message) {
    throw new NotImplementedException();
  }

  @Override
  public <K, V> Future<V> request(String subject, K message, String replyTo) {
    throw new NotImplementedException();
  }

  @Override
  public <K, V> Future<V> request(String subject, K message, String replyTo, long timeout) {
    throw new NotImplementedException();
  }

  @Override
  public Future<Void> unsubscribe(String subject) {
    throw new NotImplementedException();
  }

  @Override
  public <K, V> Future<Void> subscribe(String subject, BrokerListener<K, V> listener) {
    throw new NotImplementedException();
  }

  @Override
  public Future<Void> close() {
    throw new NotImplementedException();
  }
}
