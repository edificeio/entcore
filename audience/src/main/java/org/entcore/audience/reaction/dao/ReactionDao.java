package org.entcore.audience.reaction.dao;

import io.vertx.core.Future;
import org.entcore.audience.reaction.model.ReactionCounters;
import org.entcore.audience.reaction.model.UserReaction;
import org.entcore.common.user.UserInfos;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ReactionDao {

    Future<Map<String, ReactionCounters>> getReactionsCountersByResource(String module, String resourceType, Set<String> resourceIds);

    Future<Map<String, String>> getUserReactionByResource(String module, String resourceType, Set<String> resourceIds, String userId);

    Future<List<UserReaction>> getUsersReactions(String module, String resourceType, String resourceId, int page, int size);

    Future<Void> upsertReaction(String module, String resourceType, String resourceId, String userId, String userProfile, String reactionType);

    Future<Void> deleteReaction(String module, String resourceType, String resourceId, String userId);

    Future<Void> deleteAllReactionsOfUsers(Set<String> userIds);

    Future<Void> deleteAllReactionsOfResources(Set<String> resourceIds);
}
