package org.entcore.common.explorer.impl;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.entcore.common.explorer.IExplorerPlugin;

import java.time.Duration;

public class ExplorerPluginClientDefault extends ExplorerPluginClient {
    private final Vertx vertx;
    private final String application;
    private final String resourceType;

    public ExplorerPluginClientDefault(final Vertx vertx, final String application, final String resourceType) {
        this.vertx = vertx;
        this.application = application;
        this.resourceType = resourceType;
    }

    @Override
    protected <T> Future<T> send(MultiMap headers, JsonObject payload, final Duration timeout) {
        final String address = IExplorerPlugin.addressFor(application, resourceType);
        final DeliveryOptions options = new DeliveryOptions().setHeaders(headers).setSendTimeout(timeout.toMillis());
        final Promise<Message<T>> promise = Promise.promise();
        vertx.eventBus().request(address, payload, options, promise);
        return promise.future().map(e->{
            return e.body();
        });
    }
}
