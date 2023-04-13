package org.entcore.common.messaging.impl;

import org.entcore.common.messaging.IMessagingClient;
import org.entcore.common.messaging.IMessagingClientFactory;

public class NoopMessagingClientFactory implements IMessagingClientFactory {
    public static final NoopMessagingClientFactory instance = new NoopMessagingClientFactory();
    @Override
    public IMessagingClient create() {
        return IMessagingClient.noop;
    }
    private NoopMessagingClientFactory() {}
}
