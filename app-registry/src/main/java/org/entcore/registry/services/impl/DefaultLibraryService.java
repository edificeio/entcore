package org.entcore.registry.services.impl;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.registry.services.LibraryService;

import java.util.concurrent.CompletableFuture;

enum REASON {BAD_CONFIGURATION, LIBRARY, OK}

enum MESSAGE {API_URL_NOT_SET, DISABLED, WRONG_TOKEN, LIBRARY_KO, OK}

public class DefaultLibraryService implements LibraryService {
    JsonObject config;
    HttpClient http;
    private static final String RESSOURCE_ENDPOINT = "api/ressources";
    private static final String CONFIG_LIBRARY_ENABLED = "library-enabled";
    private static final String CONFIG_LIBRARY_API_URL = "library-api-url";
    private static final String CONFIG_LIBRARY_TOKEN = "library-token";

    public DefaultLibraryService(HttpClient http, JsonObject config) {
        this.config = config;
        this.http = http;
    }

    @Override
    public CompletableFuture<JsonObject> add() {
        boolean isLibraryEnabled = config.getBoolean(CONFIG_LIBRARY_ENABLED, false);
        String libraryApiUrl = config.getString(CONFIG_LIBRARY_API_URL);
        String libraryToken = config.getString(CONFIG_LIBRARY_TOKEN);

        if (!isLibraryEnabled) {
            return CompletableFuture.completedFuture(generateJsonResponse(false, REASON.BAD_CONFIGURATION, MESSAGE.DISABLED));
        }
        if (!(libraryApiUrl != null && libraryApiUrl.length() > 0)) {
            return CompletableFuture.completedFuture(generateJsonResponse(false, REASON.BAD_CONFIGURATION, MESSAGE.API_URL_NOT_SET));
        }
        if (!(libraryToken != null && libraryToken.length() > 0)) {
            return CompletableFuture.completedFuture(generateJsonResponse(false, REASON.BAD_CONFIGURATION, MESSAGE.WRONG_TOKEN));
        }

        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        HttpClientRequest request = http.postAbs(libraryApiUrl.concat(RESSOURCE_ENDPOINT), response -> {
            if (response.statusCode() == 401) {
                future.complete(generateJsonResponse(false, REASON.LIBRARY, MESSAGE.WRONG_TOKEN));
            } else if (response.statusCode() == 200) {
                future.complete(generateJsonResponse(true, REASON.OK, MESSAGE.OK));
            } else {
                future.complete(generateJsonResponse(false, REASON.LIBRARY, MESSAGE.LIBRARY_KO));
            }
        });
        request.putHeader("Authorization", "Bearer ".concat(libraryToken));
        request.end();
        return future;
    }

    private JsonObject generateJsonResponse(boolean success, REASON reason, MESSAGE message) {
        return new JsonObject()
                .put("success", success)
                .put("reason", reason.name())
                .put("message", message.name());
    }
}
