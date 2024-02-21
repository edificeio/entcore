package org.entcore.audience.reaction.service.impl;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.entcore.audience.reaction.dao.ReactionDao;
import org.entcore.audience.reaction.model.*;
import org.entcore.audience.reaction.service.ReactionService;
import org.entcore.common.user.UserInfos;

import java.util.*;

public class ReactionServiceImpl implements ReactionService {

    private final ReactionDao reactionDao;

    public ReactionServiceImpl(ReactionDao reactionDao) {
        this.reactionDao = reactionDao;
    }

    @Override
    public Future<ReactionsSummaryResponse> getReactionsSummary(String module, String resourceType, Set<String> resourceIds, UserInfos userInfos) {
        Promise<ReactionsSummaryResponse> reactionsSummaryPromise = Promise.promise();
        Future<Map<String, ReactionCounters>> reactionsCountersByResourceFuture = reactionDao.getReactionsCountersByResource(module, resourceType, resourceIds);
        Future<Map<String, String>> userReactionByResourceFuture = reactionDao.getUserReactionByResource(module, resourceType, resourceIds, userInfos);

        CompositeFuture.all(reactionsCountersByResourceFuture, userReactionByResourceFuture).compose(result -> {
            final Map<String, ReactionsSummaryForResource> reactionsSummary = new HashMap<>();
            reactionsCountersByResourceFuture.result().forEach((resourceId, reactionsCounters) -> {
                reactionsSummary.put(resourceId, new ReactionsSummaryForResource(
                        reactionsCounters.getCountByType().keySet(),
                        userReactionByResourceFuture.result().get(resourceId),
                        reactionsCounters.getAllReactionsCounter()
                ));
            });
            reactionsSummaryPromise.complete(new ReactionsSummaryResponse(reactionsSummary));
            return Future.succeededFuture();
        }).onFailure(reactionsSummaryPromise::fail);
        return reactionsSummaryPromise.future();
    }

    @Override
    public Future<ReactionDetailsResponse> getReactionDetails(String module, String resourceType, String resourceId, int page, int size) {
        Promise<ReactionDetailsResponse> promise = Promise.promise();
        Future<Map<String, ReactionCounters>> reactionCountersByResource = reactionDao.getReactionsCountersByResource(module, resourceType, Collections.singleton(resourceId));
        Future<List<UserReaction>> userReactionsFuture = reactionDao.getUserReactions(module, resourceType, resourceId, page, size);

        CompositeFuture.all(reactionCountersByResource, userReactionsFuture).compose(result -> {
            ReactionDetailsResponse reactionDetailsResponse = new ReactionDetailsResponse(
                    reactionCountersByResource.result().get(resourceId),
                    userReactionsFuture.result()
            );
            promise.complete(reactionDetailsResponse);
            return Future.succeededFuture();
        }).onFailure(promise::fail);
        return promise.future();
    }

    @Override
    public Future<Void> upsertReaction(String module, String resourceType, String resourceId, UserInfos userInfos, String reactionType) {
        return reactionDao.upsertReaction(module, resourceType, resourceId, userInfos, reactionType);
    }

    @Override
    public Future<Void> deleteReaction(String module, String resourceType, String resourceId, UserInfos userInfos) {
        return reactionDao.deleteReaction(module, resourceType, resourceId, userInfos);
    }
}
