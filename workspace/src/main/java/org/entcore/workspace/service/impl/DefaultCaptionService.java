package org.entcore.workspace.service.impl;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.entcore.common.folders.impl.DocumentHelper;
import org.entcore.common.s3.S3Client;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.impl.S3Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.workspace.dao.DocumentDao;
import org.entcore.workspace.service.CaptionService;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
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
    private final WebClient webClient;

    public DefaultCaptionService(MongoDb mongo, String platformId, Vertx vertx, Storage storage, JsonObject config) {
        this.documentDao = new DocumentDao(mongo);
        this.platformId = platformId;
        this.vertx = vertx;
        this.storage = storage;
        this.captionOcrS3Storage = initCaptionStorage(vertx, config);
        this.webClient = WebClient.create(vertx);
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

    /**
     * Download the image located at the given external URL and get its description, without persisting any
     * workspace document (result is not cached).
     *
     * @param user
     * @param imageUrl
     * @param sessionId
     * @param userAgent
     * @param language
     * @return
     */
    @Override
    public Future<String> getCaptionFromUrl(UserInfos user, String imageUrl, String sessionId, String userAgent, String language) {
        return generateFromUrl(imageUrl, TaskType.CAPTION, user, sessionId, userAgent, language);
    }

    /**
     * Download the image located at the given external URL and extract its text, without persisting any
     * workspace document (result is not cached).
     *
     * @param user
     * @param imageUrl
     * @param sessionId
     * @param userAgent
     * @param language
     * @return
     */
    @Override
    public Future<String> getOcrFromUrl(UserInfos user, String imageUrl, String sessionId, String userAgent, String language) {
        return generateFromUrl(imageUrl, TaskType.OCR, user, sessionId, userAgent, language);
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

        return readFromWorkspaceStorage(fileId)
                .compose(buffer -> writeToCaptionStorage(s3Path, buffer, contentType, filename))
                .compose(v -> {
                    final JsonObject payload = createPayload(taskType.value(), user, sessionId, s3Path, userAgent,
                                                             language);
                    return requestCaption(payload);
                });
    }

    /**
     * Download the image from the given external URL and get its caption/OCR, without persisting a workspace
     * document.
     *
     * @param imageUrl
     * @param taskType
     * @param user
     * @param sessionId
     * @param userAgent
     * @param language
     * @return
     */
    private Future<String> generateFromUrl(String imageUrl, TaskType taskType, UserInfos user, String sessionId, String userAgent, String language) {
        if (captionOcrS3Storage == null) {
            return Future.failedFuture(new IllegalStateException("Caption S3 storage not configured"));
        }

        final URI uri;
        try {
            uri = new URI(imageUrl);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new URISyntaxException(imageUrl, "Unsupported scheme");
            }
        } catch (URISyntaxException | NullPointerException e) {
            return Future.failedFuture(new IllegalArgumentException("Invalid image URL: " + imageUrl));
        }

        return downloadImage(imageUrl).compose(response -> {
            final String contentType = response.getHeader("Content-Type");
            if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                return Future.failedFuture(new IllegalArgumentException("URL does not point to an image: " + imageUrl));
            }

            final String filename = extractFilenameFromUrl(uri);
            final String s3Path = S3Client.getPath(UUID.randomUUID().toString()) + extractExtension(filename);

            return writeToCaptionStorage(s3Path, response.body(), contentType, filename)
                    .compose(v -> requestCaption(createPayload(taskType.value(), user, sessionId, s3Path, userAgent, language)));
        });
    }

    private Future<HttpResponse<Buffer>> downloadImage(String imageUrl) {
        return webClient.getAbs(imageUrl).send().compose(response -> {
            if (response.statusCode() != 200) {
                return Future.failedFuture(
                        new IllegalArgumentException("Failed to download image, status code: " + response.statusCode()));
            }
            return Future.succeededFuture(response);
        });
    }

    private static String extractFilenameFromUrl(URI uri) {
        final String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            return "image";
        }
        final String name = path.substring(path.lastIndexOf('/') + 1);
        return name.isEmpty() ? "image" : name;
    }

    /**
     * Read a file from the workspace platform storage.
     *
     * @param fileId
     * @return
     */
    private Future<Buffer> readFromWorkspaceStorage(String fileId) {
        final Promise<Buffer> promise = Promise.promise();

        storage.readFile(fileId, buffer -> {
            if (buffer == null) {
                promise.fail("Failed to read file " + fileId + " from workspace storage");
            } else {
                promise.complete(buffer);
            }
        });

        return promise.future();
    }

    /**
     * Write a file to the AI service's own storage.
     *
     * @param s3Path
     * @param buffer
     * @param contentType
     * @param filename
     * @return
     */
    private Future<Void> writeToCaptionStorage(String s3Path, Buffer buffer, String contentType, String filename) {
        final Promise<Void> promise = Promise.promise();

        captionOcrS3Storage.writeBuffer(s3Path, buffer, contentType, filename, writeResult -> {
            if ("ok".equals(writeResult.getString("status"))) {
                promise.complete();
            } else {
                promise.fail(
                        writeResult.getString("message", "Failed to write file to caption S3 storage"));
            }
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
