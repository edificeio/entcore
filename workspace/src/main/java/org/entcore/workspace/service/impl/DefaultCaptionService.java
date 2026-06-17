package org.entcore.workspace.service.impl;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import org.entcore.common.s3.S3Client;
import org.entcore.common.user.UserInfos;
import org.entcore.workspace.dao.DocumentDao;
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

public class DefaultCaptionService implements CaptionService {
    private static final Logger logger = LoggerFactory.getLogger(DefaultCaptionService.class);
    private final DocumentDao documentDao;
    private final String platformId;

    private static final String NATS_SUBJECT = "ia.caption.process";
    private static final String BROKER_ADDRESS = "broker.request";
    private static final long REQUEST_TIMEOUT_MS = 30000;
    private static final String DEFAULT_SIZE = "medium";

    private final Vertx vertx;

    public DefaultCaptionService(MongoDb mongo, String platformId, Vertx vertx) {
        this.documentDao = new DocumentDao(mongo);
        this.platformId = platformId;
        this.vertx = vertx;
    }

    @Override
    public Future<String> getCaption(UserInfos user, String documentId, String sessionId, String userAgent, String language) {
        return getOrGenerate(documentId, TaskType.CAPTION, doc ->
                generate(doc, TaskType.CAPTION, user, sessionId, userAgent, language));
    }

    @Override
    public Future<String> getOcr(UserInfos user, String documentId, String sessionId, String userAgent, String language) {
        return getOrGenerate(documentId, TaskType.OCR, doc ->
                generate(doc, TaskType.OCR, user, sessionId, userAgent, language));
    }

    private Future<String> getOrGenerate(String documentId, TaskType taskType, Function<JsonObject, Future<String>> generator) {
        final String taskField = taskType.value();

        return documentDao.findById(documentId).compose(document -> {
            if (document == null || document.isEmpty()) {
                return Future.failedFuture(new FileNotFoundException("No document found for ID: " + documentId));
            }
            if (document.containsKey(taskField)) {
                return Future.succeededFuture(document.getString(taskField));
            }
            return generator.apply(document).compose(fresh -> storeResult(documentId, taskField, fresh));
        }).onFailure(err -> logger.error("Failed to get or generate {} for document ID {}: {}", taskField, documentId,
                                         err.getMessage()));
    }

    private Future<String> storeResult(String documentId, String taskField, String value) {
        final JsonObject update = new JsonObject().put("$set", new JsonObject().put(taskField, value));
        final Promise<String> promise = Promise.promise();

        documentDao.update(documentId, update, result -> {
            if (!"ok".equals(result.getString("status"))) {
                logger.warn("Failed to cache {} for document ID {}", taskField, documentId);
            }
            promise.complete(value);
        });

        return promise.future();
    }

    private Future<String> generate(JsonObject document, TaskType taskType, UserInfos user, String sessionId, String userAgent, String language) {
        final String fileId = document.getString("file");
        if (fileId == null || fileId.isEmpty()) {
            return Future.failedFuture("Document has no associated file for " + taskType);
        }

        final JsonObject payload = createPayload(taskType.value(), user, sessionId, fileId, userAgent, language);
        final JsonObject requestBody = new JsonObject().put("subject", NATS_SUBJECT).put("message", payload);
        final DeliveryOptions deliveryOptions = new DeliveryOptions().setSendTimeout(REQUEST_TIMEOUT_MS);

        return vertx.eventBus()
                .<String>request(BROKER_ADDRESS, requestBody, deliveryOptions)
                .compose(message -> {
                    if (message.body() == null || message.body().isEmpty()) {
                        return Future.failedFuture("Received empty response from caption service");
                    }

                    JsonObject response = new JsonObject(message.body());

                    final Boolean successState = response.getBoolean("success", false);
                    if (successState.equals(Boolean.FALSE)) {
                        return Future.failedFuture(
                                response.getString("error", "Unknown error from caption service")
                        );
                    }

                    return Future.succeededFuture(message.body());
                });
    }

    private JsonObject createPayload(String taskType, UserInfos user, String sessionId, String fileId, String userAgent, String language) {
        final String s3Path = S3Client.getPath(fileId);

        return new JsonObject()
                .put("userId", user.getUserId())
                .put("session", sessionId)
                .put("browser", userAgent)
                .put("taskType", taskType)
                .put("language", language)
                .put("s3Path", s3Path)
                .put("pfId", platformId)
                .put("size", DEFAULT_SIZE);
    }
}
