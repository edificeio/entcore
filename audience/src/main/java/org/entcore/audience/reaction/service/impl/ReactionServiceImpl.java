package org.entcore.audience.reaction.service.impl;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.audience.reaction.dao.ReactionDao;
import org.entcore.audience.reaction.model.ReactionDetailsResponse;
import org.entcore.audience.reaction.model.ReactionType;
import org.entcore.audience.reaction.model.ReactionsSummary;
import org.entcore.audience.reaction.service.ReactionService;
import org.entcore.common.user.UserInfos;

import java.util.Set;

public class ReactionServiceImpl implements ReactionService {

    private final ReactionDao reactionDao;

    public ReactionServiceImpl(ReactionDao reactionDao) {
        this.reactionDao = reactionDao;
    }

    @Override
    public Future<ReactionsSummary> getReactionsSummary(String module, String resourceType, Set<String> resourceIds, UserInfos user) {
        return reactionDao.getReactionsSummary(module, resourceType, resourceIds, user);
    }

    @Override
    public Future<ReactionDetailsResponse> getReactionDetails(String resourceId, HttpServerRequest httpRequest, UserInfos user) {
        return null;
    }

    @Override
    public Future<Void> upsertReaction(String module, String resourceType, String resourceId, UserInfos userInfos, ReactionType reactionType) {
        return reactionDao.upsertReaction(module, resourceType, resourceId, userInfos, reactionType);
    }

    @Override
    public Future<Void> deleteReaction(String resourceId, HttpServerRequest httpRequest, UserInfos user) {
        return null;
    }
}
