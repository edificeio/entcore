package org.entcore.registry.services.impl;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
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
    public CompletableFuture<JsonObject> publish(MultiMap form, Buffer cover) {
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

        String boundary = "myBoundary";
        Buffer multipartBody = generateMultipartBody(boundary, form, cover);
        HttpClientRequest request = http.postAbs(libraryApiUrl.concat(RESSOURCE_ENDPOINT));
        request.handler(response -> {
            if (response.statusCode() == 401) {
                future.complete(generateJsonResponse(false, REASON.LIBRARY, MESSAGE.WRONG_TOKEN));
            } else if (response.statusCode() == 200) {
                future.complete(generateJsonResponse(true, REASON.OK, MESSAGE.OK));
            } else {
                future.complete(generateJsonResponse(false, REASON.LIBRARY, MESSAGE.LIBRARY_KO));
            }
        });
        request.putHeader("Authorization", "Bearer ".concat(libraryToken));
        request.putHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
        request.putHeader("Content-Length", String.valueOf(multipartBody.length()));
        request.write(multipartBody);
        request.end();
        return future;
    }

    private JsonObject generateJsonResponse(boolean success, REASON reason, MESSAGE message) {
        return new JsonObject()
                .put("success", success)
                .put("reason", reason.name())
                .put("message", message.name());
    }

    private Buffer generateMultipartBody(String boundary, MultiMap form, Buffer cover) {
        Buffer buffer = Buffer.buffer();
        buffer.appendBuffer(generateMultipartBodyMultiMap(boundary, form));
        buffer.appendBuffer(generateMultipartBodyCover(boundary, cover));
        return buffer;
    }

    private Buffer generateMultipartBodyCover(String boundary, Buffer cover) {
        Buffer buffer = Buffer.buffer();
        String body = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"cover\"; filename=\"cover.png\"\r\n" +
                "Content-Type: image/png\r\n" +
                "\r\n" +
                cover.toString() + "\r\n";

        buffer.appendString(body);
        return buffer;
    }
    private Buffer generateMultipartBodyMultiMap(String boundary, MultiMap form) {
        Buffer buffer = Buffer.buffer();
        form.forEach(entry -> buffer.appendBuffer(generateMultipartBodyAttribute(boundary, entry.getKey(), entry.getValue())));
        return buffer;
    }
    private Buffer generateMultipartBodyAttribute(String boundary, String attribute, String value) {
        Buffer buffer = Buffer.buffer();
        String body = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\""+attribute+"\"\r\n" +
                "\r\n" +
                value + "\r\n";

        buffer.appendString(body);
        return buffer;
    }
}
