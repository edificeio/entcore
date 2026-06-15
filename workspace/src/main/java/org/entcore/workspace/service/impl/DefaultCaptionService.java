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

    private static final String NATS_SUBJECT = "ia.caption.process";
    private static final String BROKER_ADDRESS = "broker.request";
    private static final long REQUEST_TIMEOUT_MS = 30000;
    private static final String DEFAULT_SIZE = "medium";
    private static final String UNKNOWN_BROWSER = "Unknown";

    private final Vertx vertx;

    public DefaultCaptionService(MongoDb mongo, Vertx vertx) {
        this.documentDao = new DocumentDao(mongo);
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
        final String taskField = taskType.value();

        documentDao.findById(documentId).onFailure(err -> {
            logger.error("Mongo error while fetching document ID {}: {}", documentId, err.getMessage());
            promise.fail("Mongo error for document ID: " + documentId);
        }).onSuccess(document -> {
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
                promise.complete(fresh);
//                final JsonObject update = new JsonObject().put("$set", new JsonObject().put(taskField, fresh));
//
//                documentDao.update(documentId, update, result -> {
//                    if ("ok".equals(result.getString("status"))) {
//                        promise.complete(fresh);
//                    } else {
//                        logger.error("Error while creating the {} for ID: {}", taskField, documentId);
//                        promise.fail("Failed to store " + taskField + " for document ID: " + documentId);
//                    }
//                });
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
        logger.info("Raw response for {}: {}", taskType, body);
        return Future.succeededFuture(String.valueOf(body));
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
