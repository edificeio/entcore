package org.entcore.broker.listener;

import io.vertx.core.Future;

public interface BrokerListener<K, V> {
  Future<V> onMessage(K message, String subject);
  Class<K> getRequestType();
  Class<V> getResponseType();
}
