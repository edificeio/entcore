package org.entcore.common.messaging;

import io.vertx.core.Future;

/**
 * Service that creates a messaging client.
 */
public interface IMessagingClientFactory {
    /**
     * @return A messaging client
     */
    Future<IMessagingClient> create();
}
