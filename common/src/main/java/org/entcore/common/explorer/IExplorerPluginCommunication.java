package org.entcore.common.explorer;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.function.Function;

public interface IExplorerPluginCommunication {

    static final TypeReference<List<IngestJobStateUpdateMessage>> trUpdateMessages = new TypeReference<List<IngestJobStateUpdateMessage>>() {};

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

    default Function<Void, Void> listenForAcks(final String id, final Handler<List<IngestJobStateUpdateMessage>> onMessage) {
        final MessageConsumer<String> consumer = vertx().eventBus().consumer(id, message -> {
            final String rawBody = message.body();
            final List<IngestJobStateUpdateMessage> messages = Json.decodeValue(message.body(), trUpdateMessages);
            onMessage.handle(messages);
        });
        return e->{
            consumer.unregister();
            return null;
        };
    }
}
