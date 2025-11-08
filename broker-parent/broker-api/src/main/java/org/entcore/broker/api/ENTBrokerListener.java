package org.entcore.broker.api;

import io.vertx.core.Future;

/**
 * Interface of all ent listeners.
 */
public interface ENTBrokerListener {
    /**
     * Start listening.
     * @return Future that completes when the listener is listening on NATS and ready to proceed messages.
     */
    Future<Void> start();

    /**
     * Stop listening and releases resources.
     * @return Future that completes when the listener stopped listening on NATS and released all allocated resources.
     */
    Future<Void> stop();
}
