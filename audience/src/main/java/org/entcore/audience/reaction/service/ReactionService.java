package org.entcore.audience.reaction.service;

import io.vertx.core.Future;
import org.entcore.audience.reaction.model.ReactionDetailsResponse;
import org.entcore.audience.reaction.model.ReactionsSummaryResponse;
import org.entcore.common.user.UserInfos;

import java.util.Set;

public interface ReactionService {
    Future<ReactionsSummaryResponse> getReactionsSummary(String module, String resourceType, Set<String> resourceIds, UserInfos user);

    Future<ReactionDetailsResponse> getReactionDetails(String module, String resourceType, String resourceId, int page, int size);

    Future<Void> upsertReaction(String module, String resourceType, String resourceId, UserInfos userInfos, String reactionType);

    Future<Void> deleteReaction(String module, String resourceType, String resourceId, UserInfos user);

}
