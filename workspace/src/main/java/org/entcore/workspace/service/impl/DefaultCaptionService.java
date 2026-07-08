package org.entcore.workspace.service.impl;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import org.entcore.common.folders.impl.DocumentHelper;
import org.entcore.common.s3.S3Client;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.impl.S3Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.workspace.dao.DocumentDao;
import org.entcore.workspace.service.CaptionService;

import java.io.FileNotFoundException;
import java.net.URI;
import java.util.function.Function;

enum TaskType {
    CAPTION("caption", "alt"),
    OCR("ocr", "edifice-ocr");

    private final String value;
    private final String mongoField;

    TaskType(String value, String mongoField) {
        this.value = value;
        this.mongoField = mongoField;
    }

    public String value() {
        return value;
    }

    public String mongoField() {
        return mongoField;
    }
}

public class DefaultCaptionService implements CaptionService {

    private static final Logger log = LoggerFactory.getLogger(DefaultCaptionService.class);

    private static final String NATS_SUBJECT = "ia.caption.process";
    private static final String BROKER_ADDRESS = "broker.request";
    private static final long REQUEST_TIMEOUT_MS = 120000;
    private static final String DEFAULT_SIZE = "medium";

    private final DocumentDao documentDao;
    private final String platformId;
    private final Vertx vertx;
    private final Storage storage;
    private final S3Storage captionOcrS3Storage;

    public DefaultCaptionService(MongoDb mongo, String platformId, Vertx vertx, Storage storage, JsonObject config) {
        this.documentDao = new DocumentDao(mongo);
        this.platformId = platformId;
        this.vertx = vertx;
        this.storage = storage;
        this.captionOcrS3Storage = initCaptionStorage(vertx, config);
    }

    private static S3Storage initCaptionStorage(Vertx vertx, JsonObject config) {
        try {
            final JsonObject s3caption = config.getJsonObject("s3iacaption");
            if (s3caption != null) {
                return new S3Storage(
                        vertx, new URI(s3caption.getString("uri")),
                        s3caption.getString("accessKey"), s3caption.getString("secretKey"),
                        s3caption.getString("region"), s3caption.getString("bucket"),
                        s3caption.getString("ssec"),
                        s3caption.getBoolean("keepAlive", false),
                        s3caption.getInteger("timeout", 10000),
                        s3caption.getInteger("threshold", 100),
                        s3caption.getLong("openDelay", 10000L),
                        s3caption.getInteger("poolSize", 16)
                );
            }

            log.error("Missing s3iacaption config");
            return null;
        } catch (Exception e) {
            log.error("S3 caption storage initialization error", e);
            return null;
        }
    }

    /**
     * Get the description of the document (image only). If the description is already cached in the document it will be returned right away.
     * If the description isn't cached yet, AI service will be called.
     *
     * @param user
     * @param documentId
     * @param sessionId
     * @param userAgent
     * @param language
     * @return
     */
    @Override
    public Future<String> getCaption(UserInfos user, String documentId, String sessionId, String userAgent, String language) {
        return getOrGenerate(documentId, TaskType.CAPTION, doc ->
                generate(doc, TaskType.CAPTION, user, sessionId, userAgent, language));
    }

    /**
     * Extract the text of the document (image only). If the text is already cached in the document it will be returned right away.
     * If the text isn't cached yet, AI service will be called.
     *
     * @param user
     * @param documentId
     * @param sessionId
     * @param userAgent
     * @param language
     * @return
     */
    @Override
    public Future<String> getOcr(UserInfos user, String documentId, String sessionId, String userAgent, String language) {
        return getOrGenerate(documentId, TaskType.OCR, doc ->
                generate(doc, TaskType.OCR, user, sessionId, userAgent, language));
    }

    private static String extractExtension(String filename) {
        final int dotIndex = filename.lastIndexOf('.');
        return dotIndex < 0 ? "" : filename.substring(dotIndex);
    }

    /**
     *
     *
     * @param documentId
     * @param taskType
     * @param generator
     * @return
     */
    private Future<String> getOrGenerate(String documentId, TaskType taskType, Function<JsonObject, Future<String>> generator) {
        final String taskField = taskType.mongoField();

        return documentDao.findById(documentId).compose(document -> {
            if (document == null || document.isEmpty()) {
                return Future.failedFuture(new FileNotFoundException("No document found for ID: " + documentId));
            }
            if (document.containsKey(taskField)) {
                return Future.succeededFuture(document.getString(taskField));
            }

            if (!DocumentHelper.isImage(document)) {
                return Future.failedFuture(new IllegalArgumentException("Document " + documentId + " is not an image"));
            }

            return generator.apply(document).compose(fresh -> storeResult(documentId, taskField, fresh));
        }).onFailure(err -> log.error("Failed to get or generate " + taskField + " for document ID " + documentId + ": "
                                              + err.getMessage()));
    }

    /**
     * Cache the result of the AI service task in the mongo document.
     *
     * @param documentId
     * @param taskField
     * @param value
     * @return
     */
    private Future<String> storeResult(String documentId, String taskField, String value) {
        final JsonObject update = new JsonObject().put("$set", new JsonObject().put(taskField, value));
        final Promise<String> promise = Promise.promise();

        documentDao.update(documentId, update, result -> {
            if (!"ok".equals(result.getString("status"))) {
                log.warn("Failed to cache " + taskField + " for document ID " + documentId);
            }
            promise.complete(value);
        });

        return promise.future();
    }

    private Future<String> generate(JsonObject document, TaskType taskType, UserInfos user, String sessionId, String userAgent, String language) {
        final String fileId = document.getString("file");
        if (fileId == null || fileId.isEmpty()) {
            return Future.failedFuture(
                    new IllegalArgumentException("Document has no associated file for " + taskType.value()));
        }
        if (captionOcrS3Storage == null) {
            return Future.failedFuture(new IllegalStateException("Caption S3 storage not configured"));
        }

        final String contentType = document.getJsonObject("metadata", new JsonObject())
                .getString("content-type", "application/octet-stream");
        final String filename = document.getString("name", fileId);
        final String s3Path = S3Client.getPath(fileId) + extractExtension(filename);

        return copyToCaptionStorage(fileId, s3Path, contentType, filename)
                .compose(v -> {
                    final JsonObject payload = createPayload(taskType.value(), user, sessionId, s3Path, userAgent,
                                                             language);
                    return requestCaption(payload);
                });
    }

    /**
     * Copy the document from platform storage to AI service's own storage.
     *
     * @param fileId
     * @param s3Path
     * @param contentType
     * @param filename
     * @return
     */
    private Future<Void> copyToCaptionStorage(String fileId, String s3Path, String contentType, String filename) {
        final Promise<Void> promise = Promise.promise();

        storage.readFile(fileId, buffer -> {
            if (buffer == null) {
                promise.fail("Failed to read file " + fileId + " from workspace storage");
                return;
            }

            captionOcrS3Storage.writeBuffer(s3Path, buffer, contentType, filename, writeResult -> {
                if ("ok".equals(writeResult.getString("status"))) {
                    promise.complete();
                } else {
                    promise.fail(
                            writeResult.getString("message", "Failed to write file to caption S3 storage"));
                }
            });
        });

        return promise.future();
    }

    private Future<String> requestCaption(JsonObject payload) {
        final JsonObject requestBody = new JsonObject()
                .put("subject", NATS_SUBJECT)
                .put("message", payload)
                .put("timeout", REQUEST_TIMEOUT_MS);
        final DeliveryOptions deliveryOptions = new DeliveryOptions().setSendTimeout(REQUEST_TIMEOUT_MS);

        return vertx.eventBus()
                .<String>request(BROKER_ADDRESS, requestBody, deliveryOptions)
                .compose(this::handleCaptionResponse);
    }

    private Future<String> handleCaptionResponse(Message<String> message) {
        if (message.body() == null || message.body().isEmpty()) {
            return Future.failedFuture("Received empty response from caption service");
        }

        final JsonObject response = new JsonObject(message.body());
        if (!"success".equals(response.getString("status"))) {
            return Future.failedFuture(response.getString("message", "Unknown error from caption service"));
        }

        final String result = response.getString("result");
        if (result == null) {
            return Future.failedFuture("Caption service response missing result field");
        }

        return Future.succeededFuture(result);
    }

    private JsonObject createPayload(String taskType, UserInfos user, String sessionId, String s3Path, String userAgent, String language) {
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
