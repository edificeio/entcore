package org.entcore.common.messaging;

/**
 * Service that creates a messaging client.
 */
public interface IMessagingClientFactory {
    /**
     * @return A messaging client
     */
    IMessagingClient create();
}
