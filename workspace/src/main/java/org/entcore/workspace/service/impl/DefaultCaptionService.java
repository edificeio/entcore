package org.entcore.workspace.service.impl;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import org.entcore.common.s3.S3Client;
import org.entcore.common.service.impl.MongoDbCrudService;
import org.entcore.common.user.UserInfos;
import org.entcore.workspace.service.CaptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.function.Function;

enum TaskType {
    CAPTION("caption"),
    OCR("ocr");

    private final String value;

    TaskType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}

public class DefaultCaptionService extends MongoDbCrudService implements CaptionService {
    private static final Logger logger = LoggerFactory.getLogger(DefaultCaptionService.class);

    private static final String NATS_SUBJECT = "ia.caption.process";
    private static final String BROKER_ADDRESS = "broker.request";
    private static final long REQUEST_TIMEOUT_MS = 30000;
    private static final String DEFAULT_SIZE = "medium";
    private static final String UNKNOWN_BROWSER = "Unknown";

    private final Vertx vertx;
    private final MongoDb mongo;

    public DefaultCaptionService(MongoDb mongo, final String MONGO_DOCUMENTS_COLLECTION, Vertx vertx) {
        super(MONGO_DOCUMENTS_COLLECTION);
        this.mongo = mongo;
        this.vertx = vertx;
    }

    @Override
    public Future<String> getCaption(UserInfos user, String documentId, String sessionId, String userAgent, String language) {
        return getOrGenerate(documentId, TaskType.CAPTION, doc ->
                generate(doc, TaskType.CAPTION, user, sessionId, userAgent, language)
        );
    }

    @Override
    public Future<String> getOcr(UserInfos user, String documentId, String sessionId, String userAgent, String language) {
        return getOrGenerate(documentId, TaskType.OCR,
                             doc -> generate(doc, TaskType.OCR, user, sessionId, userAgent, language)
        );
    }

    private Future<String> getOrGenerate(String documentId, TaskType taskType, Function<JsonObject, Future<String>> generator) {
        final Promise<String> promise = Promise.promise();
        final JsonObject matcher = new JsonObject().put("_id", documentId);
        final String taskField = taskType.value();

        mongo.findOne(collection, matcher, message -> {
            final JsonObject body = message.body();

            if (body == null || !"ok".equals(body.getString("status"))) {
                logger.error("Mongo error while fetching document ID {}: {}", documentId, body);
                promise.fail("Mongo error for document ID: " + documentId);
                return;
            }

            final JsonObject document = body.getJsonObject("result");
            if (document == null || document.isEmpty()) {
                logger.error("No document found for ID: {}", documentId);
                promise.fail(new FileNotFoundException("No document found for ID: " + documentId));
                return;
            }

            if (document.containsKey(taskField)) {
                promise.complete(document.getString(taskField));
                return;
            }

            generator.apply(document).onSuccess(fresh -> {
                final JsonObject update = new JsonObject().put("$set", new JsonObject().put(taskField, fresh));

                mongo.update(collection, matcher, update, updateResult -> {
                    JsonObject result = updateResult.body();
                    if (result != null && "ok".equals(result.getString("status"))) {
                        promise.complete(fresh);
                    } else {
                        logger.error("Error while creating the {} for ID: {}", taskField, documentId);
                        promise.fail("Failed to store " + taskField + " for document ID: " + documentId);
                    }
                });
            }).onFailure(err -> {
                logger.error("Error while generating the {} for ID: {}", taskField, documentId, err);
                promise.fail(err);
            });
        });

        return promise.future();
    }

    /**
     * Calls the caption/OCR model over NATS (subject {@code ia.caption.process}) and returns the generated text.
     */
    private Future<String> generate(JsonObject document, TaskType taskType, UserInfos user, String sessionId, String userAgent, String language) {
        final String fileId = document.getString("file");
        if (fileId == null || fileId.isEmpty()) {
            return Future.failedFuture("Document has no associated file for " + taskType);
        }

        final JsonObject payload = createPayload(taskType.value(), user, sessionId, fileId, userAgent, language);
        JsonObject requestBody = new JsonObject().put("subject", NATS_SUBJECT).put("message", payload);
        DeliveryOptions deliveryOptions = new DeliveryOptions().setSendTimeout(REQUEST_TIMEOUT_MS);

        return this.vertx
                .eventBus()
                .<String>request(BROKER_ADDRESS, requestBody, deliveryOptions)
                .compose(reply -> parseWorkerResponse(reply.body(), taskType))
                .onFailure(err -> logger.error("Error while calling NATS for {}: {}", taskType, err.getMessage()));
    }

    private Future<String> parseWorkerResponse(String body, TaskType taskType) {
        if (body == null || body.isEmpty()) {
            return Future.failedFuture("Empty response from IA worker for " + taskType);
        }

        final JsonObject json;
        try {
            json = new JsonObject(body);
        } catch (DecodeException e) {
            return Future.failedFuture("Invalid IA worker response for " + taskType + ": " + body);
        }

        if (Boolean.FALSE.equals(json.getBoolean("success")) || json.containsKey("error")) {
            return Future.failedFuture(json.getString("error", "IA worker error for " + taskType));
        }

        final String status = json.getString("status");
        if (status != null && !"success".equals(status)) {
            return Future.failedFuture(
                    json.getString("message", "IA worker returned status '" + status + "' for " + taskType));
        }

        final String result = json.getString("result");
        if (result == null || result.isEmpty()) {
            return Future.failedFuture("IA worker returned an empty result for " + taskType);
        }

        return Future.succeededFuture(result);
    }

    /**
     * Builds the NATS request payload for the caption/OCR model.
     *
     * @param taskType  "caption" or "ocr"
     * @param user      the requesting user
     * @param sessionId the user's session id
     * @param fileId    the storage file id (document's "file" field), NOT the workspace document id
     * @param userAgent the request User-Agent header
     * @param language  the request language code (primary Accept-Language subtag)
     */
    private JsonObject createPayload(String taskType, UserInfos user, String sessionId, String fileId, String userAgent, String language) {
        final String s3Path = S3Client.getPath(fileId);

        return new JsonObject().put("userId", user.getUserId())
                               .put("session", sessionId)
                               .put("browser", UNKNOWN_BROWSER)
                               .put("taskType", taskType)
                               .put("language", language)
                               .put("s3Path", s3Path)
                               .put("pfId", "plateform-id")     // TODO real value (platform id)
                               .put("size", DEFAULT_SIZE);
    }
}
