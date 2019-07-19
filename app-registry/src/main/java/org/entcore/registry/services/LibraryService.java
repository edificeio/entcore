package org.entcore.registry.services;

import io.vertx.core.json.JsonObject;

import java.util.concurrent.CompletableFuture;

public interface LibraryService {
    CompletableFuture<JsonObject> add();
}
