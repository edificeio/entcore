package org.entcore.conversation.service.impl;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;

import java.util.UUID;

import fr.wseduc.webutils.request.CookieHelper;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class MailToExercizer {
    private static final Logger log = LoggerFactory.getLogger(MailToExercizer.class);
    private final HttpClient client;
    private final JsonObject configuration;

    public MailToExercizer(Vertx vertx, JsonObject config) {
        HttpClientOptions options = new HttpClientOptions();
        this.client = vertx.createHttpClient(options);
        configuration = config.getJsonObject("mail-to-exercizer");
    }

    private JsonObject buildIngestionPayload(String traceId, String userId, String session,
            String ua, String referer, JsonObject body) {
        final JsonObject config = configuration;
        return new JsonObject()
                .put("batch", new JsonArray()
                        .add(new JsonObject()
                                .put("type", "trace-create")
                                .put("body", new JsonObject()
                                        .put("id", traceId)
                                        .put("timestamp", java.time.Instant.now().toString())
                                        .put("name", "Stimulation - Mail2Exo")
                                        .put("userId", userId)
                                        .put("input", new JsonObject()
                                                .put("object", body.getString("object", ""))
                                                .put("message", body.getString("message", "")))
                                        .put("output", new JsonObject()
                                                .put("clique", body.getString("buttonResponse")))
                                        .put("sessionId", session)
                                        .put("release", "v1.0.0")
                                        .put("version", config.getString("version", "0.1.0"))
                                        .put("metadata", new JsonObject()
                                                .put("navigateur", ua)
                                                .put("referer", referer))
                                        .put("tags", new JsonArray()
                                                .add(config.getString("environment", "dev")))
                                        .put("public", false))
                                .put("id", "event_12345")
                                .put("timestamp", java.time.Instant.now().toString())
                                .put("metadata", new JsonObject()
                                        .put("source", "API"))))
                .put("metadata", new JsonObject()
                        .put("projet", "Mail-To-Exo")
                        .put("environnement", config.getString("environment", "production")));
    }

    private void sendToIngestionAPI(JsonObject payload, Handler<JsonObject> handler) {
        final JsonObject config = configuration;
        RequestOptions optionsReq = new RequestOptions()
                .setAbsoluteURI(String.format("%s/api/public/ingestion", config.getString("langfuse_host", "")))
                .setMethod(HttpMethod.POST);

        client.request(optionsReq, ar -> {
            if (ar.succeeded()) {
                String authCredentials = config.getString("langfuse_public_key") + ":"
                        + config.getString("langfuse_secret_key");
                String base64Auth = java.util.Base64.getEncoder().encodeToString(authCredentials.getBytes());
                ar.result()
                        .putHeader("Content-Type", "application/json")
                        .putHeader("Authorization", "Basic " + base64Auth)
                        .send(Buffer.buffer(payload.encode()), sendAr -> {
                            if (sendAr.succeeded()) {
                                handler.handle(new JsonObject());
                                log.info("Ingestion API call successful, status: " + sendAr.result().statusCode());
                            } else {
                                handler.handle(new JsonObject());
                                log.error("Ingestion API call failed", sendAr.cause());
                            }
                        });
            } else {
                handler.handle(new JsonObject());
                log.error("Failed to create HTTP request", ar.cause());
            }
        });
    }

    public void storeEvent(HttpServerRequest request, String userId, Handler<JsonObject> handler) {
        String traceId = UUID.randomUUID().toString();
        String session = CookieHelper.getInstance().getSigned("oneSessionId", request);
        String ua = request.headers().get("User-Agent");
        String referer = request.headers().get("Referer");

        bodyToJson(request, body -> {
            JsonObject payload = buildIngestionPayload(traceId, userId, session, ua, referer, body);
            sendToIngestionAPI(payload, handler);
        });
    }
}
