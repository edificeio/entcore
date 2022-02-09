package org.entcore.common.explorer;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.function.Function;

public interface IExplorerPluginCommunication {

    Vertx vertx();

    Future<Void> waitPending();

    Future<Void> pushMessage(final ExplorerMessage message);

    Future<Void> pushMessage(final List<ExplorerMessage> messages);

    default Function<Void, Void> listen(final String id, final Handler<Message<JsonObject>> onMessage) {
        final MessageConsumer<JsonObject> consumer = vertx().eventBus().consumer(id, onMessage);
        return e->{
            consumer.unregister();
            return null;
        };
    }
}
