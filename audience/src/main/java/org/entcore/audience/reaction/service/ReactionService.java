package org.entcore.audience.reaction.service;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.audience.reaction.model.ReactionDetailsResponse;
import org.entcore.audience.reaction.model.ReactionType;
import org.entcore.audience.reaction.model.ReactionsSummary;
import org.entcore.common.user.UserInfos;

import java.util.Set;

public interface ReactionService {
    Future<ReactionsSummary> getReactionsSummary(String module, String resourceType, Set<String> resourceIds, UserInfos user);

    Future<ReactionDetailsResponse> getReactionDetails(String resourceId, HttpServerRequest httpRequest, UserInfos user);

    Future<Void> upsertReaction(String module, String resourceType, String resourceId, UserInfos userInfos, ReactionType reactionType);
    Future<Void> deleteReaction(String resourceId, HttpServerRequest httpRequest, UserInfos user);

}
