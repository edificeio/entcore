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

import java.util.stream.Collectors;

/**
 * Answers the moderation application: can a user see a conversation resource?
 * Only messages are supported, identified by the path {@code message/<messageId>}.
 * A user can see a message as long as it is in one of their folders, trash included.
 */
public class ModerationBrokerListenerImpl extends ModerationEventBrokerListener {

    private static final Logger log = LoggerFactory.getLogger(ModerationBrokerListenerImpl.class);
    private static final String MESSAGE_PATH_PREFIX = "message/";
    private static final String HAS_RIGHT_QUERY =
            "SELECT EXISTS(SELECT 1 FROM conversation.usermessages_v2 um " +
            "WHERE um.user_id = ?::UUID AND um.message_id = ?::UUID) AS \"hasRight\"";
    /** messages.id is a VARCHAR primary key: compared as text so that the index is used. */
    private static final String VERSIONS_QUERY =
            "SELECT m.id, encode(sha256(convert_to(coalesce(m.body, ''), 'UTF8')), 'hex') AS hash " +
            "FROM conversation.messages m WHERE m.id IN ";

    private final Sql sql;

    public ModerationBrokerListenerImpl(Sql sql, Vertx vertx, Parameters parameters) {
        super(vertx, parameters);
        this.sql = sql;
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
