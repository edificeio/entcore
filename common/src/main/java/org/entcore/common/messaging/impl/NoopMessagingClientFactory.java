package org.entcore.common.messaging.impl;

import io.vertx.core.Future;
import org.entcore.common.messaging.IMessagingClient;
import org.entcore.common.messaging.IMessagingClientFactory;

import static io.vertx.core.Future.succeededFuture;

public class NoopMessagingClientFactory implements IMessagingClientFactory {
    public static final NoopMessagingClientFactory instance = new NoopMessagingClientFactory();
    @Override
    public Future<IMessagingClient> create() {
        return succeededFuture(IMessagingClient.noop);
    }
    private NoopMessagingClientFactory() {}
}
