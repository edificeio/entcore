package org.entcore.conversation.service.impl;

import io.edifice.moderation.events.ModerationEventBrokerListener;
import io.edifice.moderation.v1.RightsOnResourceRequest;
import io.edifice.moderation.v1.RightsOnResourceResponse;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        log.debug("Checking moderation right on message {} for user {}", messageId, userId);

        final Promise<RightsOnResourceResponse> promise = Promise.promise();
        sql.prepared(HAS_RIGHT_QUERY, new JsonArray().add(userId).add(messageId), SqlResult.validUniqueResultHandler(result -> {
            if (result.isLeft()) {
                log.error("Moderation right check failed on message {} for user {}: {}", messageId, userId, result.left().getValue());
                promise.complete(new RightsOnResourceResponse(false));
            } else {
                promise.complete(new RightsOnResourceResponse(result.right().getValue().getBoolean("hasRight", false)));
            }
        }));
        return promise.future();
    }
}
