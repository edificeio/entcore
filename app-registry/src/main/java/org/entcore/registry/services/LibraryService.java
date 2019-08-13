package org.entcore.registry.services;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.CompletableFuture;

public interface LibraryService {
    CompletableFuture<JsonObject> publish(MultiMap form, Buffer cover);
}
