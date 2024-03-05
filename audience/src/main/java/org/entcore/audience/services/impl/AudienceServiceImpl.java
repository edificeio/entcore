package org.entcore.audience.services.impl;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.entcore.audience.reaction.service.ReactionService;
import org.entcore.audience.services.AudienceService;
import org.entcore.audience.view.service.ViewService;

import java.util.Collections;
import java.util.Set;

public class AudienceServiceImpl implements AudienceService {

    private final ReactionService reactionService;

    private final ViewService viewService;

    public AudienceServiceImpl(ReactionService reactionService, ViewService viewService) {
        this.reactionService = reactionService;
        this.viewService = viewService;
    }

    @Override
    public Future<Void> deleteUsers(Set<String> userIds) {
        return reactionService.deleteAllReactionsOfUsers(userIds);
    }

    @Override
    public Future<Void> mergeUsers(String keptUserId, String deletedUserId) {
        Promise<Void> promise = Promise.promise();
        CompositeFuture.all(
                reactionService.deleteAllReactionsOfUsers(Collections.singleton(deletedUserId)),
                viewService.mergeUserViews(keptUserId, deletedUserId))
                .onSuccess(result -> promise.complete())
                .onFailure(th -> promise.fail(th.getCause()));
        return promise.future();
    }

    @Override
    public Future<Void> purgeDeletedResources(String module, String resourceType, Set<String> resourceIds) {
        Promise<Void> promise = Promise.promise();
        CompositeFuture.all(
                reactionService.deleteAllReactionsOfResources(resourceIds),
                viewService.deleteAllViewsOfResources(resourceIds))
                .onSuccess(result -> promise.complete())
                .onFailure(th -> promise.fail(th.getCause()));
        return promise.future();
    }
}
