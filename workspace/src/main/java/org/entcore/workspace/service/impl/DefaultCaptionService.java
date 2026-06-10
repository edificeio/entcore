package org.entcore.workspace.service.impl;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.entcore.common.s3.S3Client;
import org.entcore.common.service.impl.MongoDbCrudService;
import org.entcore.workspace.service.CaptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.function.Function;

public class DefaultCaptionService extends MongoDbCrudService implements CaptionService {
    private static final Logger logger = LoggerFactory.getLogger(DefaultCaptionService.class);

    MongoDb mongo;

    public DefaultCaptionService(MongoDb mongo, final String MONGO_DOCUMENTS_COLLECTION) {
        super(MONGO_DOCUMENTS_COLLECTION);
        this.mongo = mongo;
    }

    @Override
    public Future<String> getCaption(String documentId, String sessionId, String userAgent) {
        return getOrGenerate(documentId, "caption", doc -> generate(doc, "caption"));
    }

    @Override
    public Future<String> getOcr(String documentId, String sessionId, String userAgent) {
        return getOrGenerate(documentId, "ocr", doc -> generate(doc, "ocr"));
    }

    private Future<String> getOrGenerate(String documentId, String field, Function<JsonObject, Future<String>> generator) {
        final Promise<String> promise = Promise.promise();
        final JsonObject matcher = new JsonObject().put("_id", documentId);

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

            if (document.containsKey(field)) {
                promise.complete(document.getString(field));
                return;
            }

            generator.apply(document).onSuccess(fresh -> {
                final JsonObject update = new JsonObject().put("$set", new JsonObject().put(field, fresh));

                mongo.update(collection, matcher, update, updateResult -> {
                    JsonObject result = updateResult.body();
                    if (result != null && "ok".equals(result.getString("status"))) {
                        promise.complete(fresh);
                    } else {
                        logger.error("Error while creating the {} for ID: {}", field, documentId);
                        promise.fail("Failed to store " + field + " for document ID: " + documentId);
                    }
                });
            }).onFailure(err -> {
                logger.error("Error while generating the {} for ID: {}", field, documentId, err);
                promise.fail(err);
            });
        });

        return promise.future();
    }

    /**
     * Builds the NATS request payload for the caption/OCR model.
     *
     * @param fileId   the storage file id (document's "file" field), NOT the workspace document id
     * @param taskType "caption" or "ocr"
     */
    private JsonObject createPayload(String fileId, String taskType) {
        final String s3Path = S3Client.getPath(fileId);

        return new JsonObject()
                .put("userId", "123")            // TODO real value (requesting user)
                .put("session", "sess-abc")      // TODO real value
                .put("browser", "Chrome")        // TODO real value
                .put("taskType", taskType)
                .put("language", "fr")           // TODO real value (document/user locale)
                .put("s3Path", s3Path)
                .put("pfId", "plateform-id")     // TODO real value (platform id)
                .put("size", "medium");
    }

    /**
     * Calls the caption/OCR model over NATS (subject {@code ia.caption.process}) and returns the generated text.
     */
    private Future<String> generate(JsonObject document, String taskType) {
        final String fileId = document.getString("file");
        if (fileId == null || fileId.isEmpty()) {
            return Future.failedFuture("Document has no associated file for " + taskType);
        }

        final JsonObject payload = createPayload(fileId, taskType);

        // TODO send payload over NATS subject "ia.caption.process"
        return Future.failedFuture("NATS call to ia.caption.process not implemented yet");
    }
}
