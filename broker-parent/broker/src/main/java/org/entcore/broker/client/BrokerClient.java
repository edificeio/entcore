package org.entcore.broker.client;

import io.vertx.core.Future;
import org.entcore.broker.listener.BrokerListener;

public interface BrokerClient {

    Future<Void> start();

    <K> Future<Void> sendMessage(String subject, K message);

    <K, V> Future<V> request(String subject, K message, String replyTo);

    <K, V> Future<V> request(String subject, K message, String replyTo, long timeout);

    Future<Void> unsubscribe(String subject);

    <K,V> Future<Void> subscribe(String subject, BrokerListener<K, V> listener);

    Future<Void> close();
}
