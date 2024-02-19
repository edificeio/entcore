package org.entcore.audience.reaction.dao;

import io.vertx.core.Future;
import org.entcore.audience.reaction.model.ReactionType;
import org.entcore.audience.reaction.model.ReactionsSummary;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;

import java.util.Set;

public interface ReactionDao {

    Future<ReactionsSummary> getReactionsSummary(String module, String resourceType, Set<String> resourceIds, UserInfos userInfos);

    Future<Void> upsertReaction(String module, String resourceType, String resourceId, UserInfos userInfos, ReactionType reactionType);


}
