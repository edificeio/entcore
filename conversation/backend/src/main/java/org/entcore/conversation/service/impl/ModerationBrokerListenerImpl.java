package org.entcore.conversation.service.impl;

import io.edifice.moderation.events.ModerationEventBrokerListener;
import io.edifice.moderation.v1.*;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.utils.Config;
import org.entcore.conversation.util.QuotaUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Answers the moderation application: can a user see a conversation resource, what version is it, delete it.
 * Only messages are supported, identified by the path {@code message/<messageId>}.
 * A user can see a message as long as it is in one of their folders, trash included.
 */
public class ModerationBrokerListenerImpl extends ModerationEventBrokerListener {

    private static final Logger log = LoggerFactory.getLogger(ModerationBrokerListenerImpl.class);
    private static final String MESSAGE_PATH_PREFIX = "/conversation/message/";
    private static final String HAS_RIGHT_QUERY =
            "SELECT EXISTS(SELECT 1 FROM conversation.usermessages_v2 um " +
            "WHERE um.user_id = ?::UUID AND um.message_id = ?::UUID) AS \"hasRight\"";
    /** messages.id is a VARCHAR primary key: compared as text so that the index is used. */
    private static final String VERSIONS_QUERY =
            "SELECT m.id, encode(sha256(convert_to(coalesce(m.body, ''), 'UTF8')), 'hex') AS hash " +
            "FROM conversation.messages m WHERE m.id IN ";

    /** Removes the message from every box, and returns per user the storage to give back. */
    private static final String DELETE_MESSAGE_QUERY =
            "WITH deleted AS (DELETE FROM conversation.usermessages_v2 um WHERE um.message_id = ?::UUID " +
            "RETURNING um.user_id, um.total_quota) " +
            "SELECT user_id::text AS user_id, coalesce(sum(total_quota), 0)::bigint AS total_quota FROM deleted GROUP BY user_id";

    private final Sql sql;
    private final Vertx vertx;

    public ModerationBrokerListenerImpl(Sql sql, Vertx vertx, Parameters parameters) {
        super(vertx, parameters);
        this.sql = sql;
        this.vertx = vertx;
    }

    /**
     * Removes the message from the box of every user and gives them back its storage.
     * Fails on a malformed request or an unsupported path; answers {@code ERROR} when the deletion itself fails.
     */
    @Override
    protected Future<DeleteResourceResponse> onDeleteResource(DeleteResourceRequest request) {
        if (request == null || request.getResource() == null || request.getResource().getPath() == null) {
            return Future.failedFuture("moderation.invalid.request");
        }
        final String path = request.getResource().getPath();
        if (!path.startsWith(MESSAGE_PATH_PREFIX)) {
            return Future.failedFuture("moderation.unsupported.resource");
        }
        final String messageId = path.substring(MESSAGE_PATH_PREFIX.length());
        final Promise<DeleteResourceResponse> promise = Promise.promise();
        sql.prepared(DELETE_MESSAGE_QUERY, new JsonArray().add(messageId), SqlResult.validResultHandler(result -> {
            if (result.isLeft()) {
                log.error(String.format("Moderation delete failed on message %s : %s", messageId, result.left().getValue()));
                promise.complete(new DeleteResourceResponse(ModerationResponseStatus.ERROR));
                return;
            }
            final Map<String, Long> sizes = new HashMap<>();
            for (Object o : result.right().getValue()) {
                final JsonObject row = (JsonObject) o;
                if (row.getLong("total_quota") > 0) {
                    sizes.put(row.getString("user_id"), -row.getLong("total_quota"));
                }
            }
            final int threshold = Config.getConf().getInteger("alertStorage", QuotaUtil.DEFAULT_STORAGE_ALERT_THRESHOLD);
            // The rows are gone : the response does not depend on the storage update, logged by QuotaUtil on failure.
            QuotaUtil.updateUsersQuota(vertx.eventBus(), sizes, threshold)
                    .onComplete(v -> promise.complete(new DeleteResourceResponse(ModerationResponseStatus.OK)));
        }));
        return promise.future();
    }

    /**
     * Fails on a malformed request or an unsupported path; answers {@code false} when the check itself fails.
     */
    @Override
    protected Future<RightsOnResourceResponse> onHasRight(RightsOnResourceRequest request) {
        if (request == null || request.getUserId() == null || request.getResource() == null || request.getResource().getPath() == null ) {
            return Future.failedFuture("moderation.invalid.request");
        }
        final String path = request.getResource().getPath();
        if (!path.startsWith(MESSAGE_PATH_PREFIX)) {
            return Future.failedFuture("moderation.unsupported.resource");
        }
        final String userId = request.getUserId();
        final String messageId = path.substring(MESSAGE_PATH_PREFIX.length());
        log.debug(String.format("Checking moderation right on message %s for user %s", messageId, userId));

        final Promise<RightsOnResourceResponse> promise = Promise.promise();
        sql.prepared(HAS_RIGHT_QUERY, new JsonArray().add(userId).add(messageId), SqlResult.validUniqueResultHandler(result -> {
            if (result.isLeft()) {
                log.error(String.format("Moderation right check failed on message %s for user %s: %s", messageId, userId, result.left().getValue()));
                promise.complete(new RightsOnResourceResponse(false));
            } else {
                promise.complete(new RightsOnResourceResponse(result.right().getValue().getBoolean("hasRight", false)));
            }
        }));
        return promise.future();
    }

    /**
     * Version of each message = SHA-256 of its body. Unknown messages are left out of the response.
     * Fails on a malformed request or when any path is not a message path.
     */
    @Override
    protected Future<ResourcesVersionsResponse> onResourcesVersions(ResourcesVersionsRequest request) {
        if (request == null || request.getResources() == null || request.getResources().isEmpty()) {
            return Future.failedFuture("moderation.invalid.request");
        }
        final JsonArray messageIds = new JsonArray();
        for (Resource resource : request.getResources()) {
            if (resource == null || resource.getPath() == null || !resource.getPath().startsWith(MESSAGE_PATH_PREFIX)) {
                return Future.failedFuture("moderation.unsupported.resource");
            }
            messageIds.add(resource.getPath().substring(MESSAGE_PATH_PREFIX.length()));
        }

        final String query = VERSIONS_QUERY + Sql.listPrepared(messageIds.getList());
        final Promise<ResourcesVersionsResponse> promise = Promise.promise();
        sql.prepared(query, messageIds, SqlResult.validResultHandler(result -> {
            if (result.isLeft()) {
                log.error("Moderation versions lookup failed: " + result.left().getValue());
                promise.fail("moderation.versions.error");
            } else {
                promise.complete(new ResourcesVersionsResponse(result.right().getValue().stream()
                        .map(JsonObject.class::cast)
                        .map(row -> new Resource(MESSAGE_PATH_PREFIX + row.getString("id"), row.getString("hash")))
                        .collect(Collectors.toList())));
            }
        }));
        return promise.future();
    }
}
