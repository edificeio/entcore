package org.entcore.audience.reaction.service.impl;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.audience.reaction.dao.ReactionDao;
import org.entcore.audience.reaction.model.*;
import org.entcore.audience.reaction.service.ReactionService;
import org.entcore.common.user.UserInfos;

import java.util.*;
import java.util.stream.Collectors;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class ReactionServiceImpl implements ReactionService {
    public static final String DIRECTORY_ADDRESS = "entcore.directory";
    private final EventBus eventBus;
    private final ReactionDao reactionDao;

    public ReactionServiceImpl(EventBus eventBus, ReactionDao reactionDao) {
        this.eventBus =eventBus;
        this.reactionDao = reactionDao;
    }

    @Override
    public Future<ReactionsSummaryResponse> getReactionsSummary(String module, String resourceType, Set<String> resourceIds, UserInfos userInfos) {
        Promise<ReactionsSummaryResponse> reactionsSummaryPromise = Promise.promise();
        Future<Map<String, ReactionCounters>> reactionsCountersByResourceFuture = reactionDao.getReactionsCountersByResource(module, resourceType, resourceIds);
        Future<Map<String, String>> userReactionByResourceFuture = reactionDao.getUserReactionByResource(module, resourceType, resourceIds, userInfos.getUserId());

        CompositeFuture.all(reactionsCountersByResourceFuture, userReactionByResourceFuture).compose(result -> {
            final Map<String, ReactionsSummaryForResource> reactionsSummary = new HashMap<>();
            reactionsCountersByResourceFuture.result().forEach((resourceId, reactionsCounters) -> reactionsSummary.put(resourceId, new ReactionsSummaryForResource(
                    reactionsCounters.getCountByType().keySet(),
                    userReactionByResourceFuture.result().get(resourceId),
                    reactionsCounters.getAllReactionsCounter()
            )));
            reactionsSummaryPromise.complete(new ReactionsSummaryResponse(reactionsSummary));
            return Future.succeededFuture();
        }).onFailure(reactionsSummaryPromise::fail);
        return reactionsSummaryPromise.future();
    }

    @Override
    public Future<ReactionDetailsResponse> getReactionDetails(String module, String resourceType, String resourceId, int page, int size) {
        Future<Map<String, ReactionCounters>> reactionCountersByResource = reactionDao.getReactionsCountersByResource(module, resourceType, Collections.singleton(resourceId));
        Future<List<UserReaction>> userReactionsFuture = reactionDao.getUsersReactions(module, resourceType, resourceId, page, size);

        return CompositeFuture.all(reactionCountersByResource, userReactionsFuture)
                .compose(result -> enrichUserReactionsWithDisplayName(userReactionsFuture.result()))
                .map(enrichedUserReaction -> new ReactionDetailsResponse(reactionCountersByResource.result().get(resourceId), enrichedUserReaction));
    }

    private Future<List<UserReaction>> enrichUserReactionsWithDisplayName(List<UserReaction> userReactions) {
        return getUserDisplayNames(userReactions.stream().map(UserReaction::getUserId).collect(Collectors.toSet()))
                .map(displayNamesByUser -> {
                    List<UserReaction> userReactionsWithName = new ArrayList<>();
                    userReactions.forEach(userReaction -> userReactionsWithName.add(new UserReaction(
                            userReaction.getUserId(),
                            userReaction.getProfile(),
                            userReaction.getReactionType(),
                            displayNamesByUser.get(userReaction.getUserId()))));
                    return userReactionsWithName;
                });
    }

    private Future<Map<String, String>> getUserDisplayNames(Set<String> userIds) {
        Promise<Map<String, String>> promise = Promise.promise();
        JsonObject messageBody = new JsonObject()
                .put("action", "get-users-displayNames")
                .put("userIds", new JsonArray(new ArrayList<>(userIds)));
        eventBus.request(DIRECTORY_ADDRESS, messageBody, response -> {
            if (response.succeeded()) {
                Map<String, String> displayNameByUserId = new HashMap<>();
                JsonObject responseBody = (JsonObject) response.result().body();
                responseBody.forEach(entry -> displayNameByUserId.put(entry.getKey(), entry.getValue().toString()));
                promise.complete(displayNameByUserId);
            } else {
                promise.fail(response.cause());
            }
        });
        return promise.future();
    }

    @Override
    public Future<Void> upsertReaction(String module, String resourceType, String resourceId, UserInfos userInfos, String reactionType) {
        return reactionDao.upsertReaction(module, resourceType, resourceId, userInfos.getUserId(), userInfos.getType(), reactionType);
    }

    @Override
    public Future<Void> deleteReaction(String module, String resourceType, String resourceId, UserInfos userInfos) {
        return reactionDao.deleteReaction(module, resourceType, resourceId, userInfos.getUserId());
    }
}
