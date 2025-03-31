package org.entcore.broker.client;

import io.vertx.core.Future;
import org.entcore.broker.listener.BrokerListener;

/**
 * Interface for a client to interact with the broker.
 */
public interface BrokerClient {

    /**
     * Starts the broker client.
     * @return a Future that will be completed when the client is started
     */
    Future<Void> start();

    /**
     *
     * @param subject Subject to which the message will be sent
     * @param message Message to be sent
     * @return a Future that will be completed when the message is sent
     * @param <K> Type of the message to send
     */
    <K> Future<Void> sendMessage(String subject, K message);

    /**
     * Sends a message to the broker and waits for a reply.
     * @param subject Subject to which the message will be sent
     * @param message Message to be sent
     * @return a Future that will be completed when the message is sent and will return the payload received
     * @param <K> Type of the message to send
     * @param <V> Type of the response
     */
    <K, V> Future<V> request(String subject, K message);

    <K, V> Future<V> request(String subject, K message, long timeout);

    /**
     * Subscribes to a subject and registers a listener to handle incoming messages.<br />
     * NB : Depending on the implementation, you might not be able to register multiple listeners to the same subject
     * @param subject Subject to subscribe to
     * @return a Future that will complete when the unsubscription is complete
     */
    Future<Void> unsubscribe(String subject);

    /**
     * Registers a listener to handle incoming messages form a subject.
     * @param subject Subject to subscribe to
     * @param listener Listener to handle incoming messages
     * @return a Future that will complete when the subscription is complete
     * @param <K> Type of the message the incoming message
     * @param <V> Type of the response
     */
    <K,V> Future<Void> subscribe(String subject, BrokerListener<K, V> listener);

    /**
     * Closes the broker client and free associated resources (like listeners, files, etc.).
     * @return a Future that will be completed when the client is closed
     */
    Future<Void> close();
}
