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
        configuration = config.getJsonObject("mail-to-exercizer", new JsonObject());
    }
  
    public void getPrompt(String promptName, Handler<JsonObject> handler) {
        String baseUrl = configuration.getString("langfuse_host", "");
        String username = configuration.getString("langfuse_public_key", "");
        String password = configuration.getString("langfuse_secret_key", "");
        
        RequestOptions options = new RequestOptions()
                .setAbsoluteURI(baseUrl + "/api/public/v2/prompts/" + promptName)
                .setMethod(HttpMethod.GET);
        
        client.request(options, ar -> {
            if (ar.succeeded()) {
                String authString = username + ":" + password;
                String encodedAuth = java.util.Base64.getEncoder().encodeToString(authString.getBytes());
                
                ar.result()
                    .putHeader("Authorization", "Basic " + encodedAuth)
                    .send(sendAr -> {
                        if (sendAr.succeeded()) {
                            sendAr.result().body(body -> {
                                if (body.succeeded()) {
                                    JsonObject response = new JsonObject(body.result());
                                    handler.handle(response);
                                } else {
                                    log.error("Failed to get prompt response", body.cause());
                                    handler.handle(new JsonObject().put("error", "Failed to get prompt response"));
                                }
                            });
                        } else {
                            log.error("Prompt API call failed", sendAr.cause());
                            handler.handle(new JsonObject().put("error", "API call failed"));
                        }
                    });
            } else {
                log.error("Failed to create HTTP request", ar.cause());
                handler.handle(new JsonObject().put("error", "Failed to create HTTP request"));
            }
        });
    }

    public void callMistralAI(String message, String subject, Handler<JsonObject> handler){
        this.getPrompt("Filter", 
            prompt -> {
                if (prompt.containsKey("error")) {
                    handler.handle(new JsonObject().put("error", "Failed to get prompt"));
                    return;
                }
                String promptResponse = prompt.getString("prompt");
                log.debug("Prompt: " + prompt);
                this.callMistralAI(message, subject, promptResponse, handler);
            });
    }


    public void callMistralAI(String message, String subject, String prompt,  Handler<JsonObject> handler) {
        final JsonObject config = configuration;
        log.debug("Calling Mistral AI API" +" "+ message + " "+ subject);
        RequestOptions optionsReq = new RequestOptions()
                .setAbsoluteURI("https://api.mistral.ai/v1/chat/completions")
                .setMethod(HttpMethod.POST);

        JsonObject requestBody = new JsonObject()
            .put("model", "open-mistral-nemo")
            .put("messages", new JsonArray()
                .add(new JsonObject()
                    .put("role", "system")
                    .put("content", prompt))
                .add(new JsonObject()
                    .put("role", "user")
                    .put("content",String.format("analyze this message %s which has the object %s", message, subject))))
            .put("response_format", new JsonObject()
                .put("type", "json_schema")
                .put("json_schema", new JsonObject()
                    .put("name", "chatResponse")
                    .put("schema", new JsonObject()
                        .put("type", "object")
                        .put("properties", new JsonObject()
                            .put("distribution", new JsonObject()
                                .put("type", "string")
                                .put("enum", new JsonArray()
                                    .add("YES")
                                    .add("NO"))))
                        .put("required", new JsonArray()
                            .add("distribution")))))
            .put("temperature", 0);

        client.request(optionsReq, ar -> {
            if (ar.succeeded()) {
                String apiKey = config.getString("mistral_api_key", configuration.getString("mistral_api_key"));
                ar.result()
                        .putHeader("Content-Type", "application/json")
                        .putHeader("Accept", "application/json")
                        .putHeader("Authorization", "Bearer " + apiKey)
                        .send(Buffer.buffer(requestBody.encode()), sendAr -> {
                            if (sendAr.succeeded()) {
                                sendAr.result().body(body -> {
                                    if (body.succeeded()) {
                                        JsonObject response = new JsonObject(body.result());
                                        JsonArray choices = response.getJsonArray("choices");
                                        if (choices != null && !choices.isEmpty()) {
                                            JsonObject messageWrapper = choices.getJsonObject(0).getJsonObject("message");
                                            if (messageWrapper != null) {
                                                String contentString = messageWrapper.getString("content", "{}");
                                                try {
                                                    JsonObject contentJson = new JsonObject(contentString);
                                                    handler.handle(contentJson);
                                                } catch (Exception e) {
                                                    handler.handle(new JsonObject().put("error", "Invalid JSON in content"));
                                                }
                                            } else {
                                                handler.handle(new JsonObject().put("error", "No message found in choices"));
                                            }
                                        } else {
                                            handler.handle(new JsonObject().put("error", "No choices found"));
                                        }
                                        log.debug("Mistral AI response: " + choices.getJsonObject(0).getJsonObject("message").getString("content"));
                                    } else {
                                        handler.handle(new JsonObject().put("error", "Failed to get response body"));
                                        log.error("Failed to get Mistral AI response", body.cause());
                                    }
                                });
                            } else {
                                handler.handle(new JsonObject().put("error", "API call failed"));
                                log.error("Mistral AI API call failed", sendAr.cause());
                            }
                        });
            } else {
                handler.handle(new JsonObject().put("error", "Failed to create HTTP request"));
                log.error("Failed to create HTTP request", ar.cause());
            }
        });
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
                                                .put("referer", referer)
                                                .put("mistral", body.getString("model", "")))
                                        .put("tags", new JsonArray()
                                                .add(config.getString("environment", "production")))
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
                                log.debug("Ingestion API call successful, status: " + sendAr.result().statusCode());
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
