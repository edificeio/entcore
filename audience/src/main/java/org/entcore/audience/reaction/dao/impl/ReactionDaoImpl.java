package org.entcore.audience.reaction.dao.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import org.entcore.audience.reaction.dao.ReactionDao;
import org.entcore.audience.reaction.model.ReactionType;
import org.entcore.audience.reaction.model.ReactionsSummary;
import org.entcore.common.sql.ISql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.time.LocalDateTime;
import java.util.Set;

public class ReactionDaoImpl implements ReactionDao {

    private final ISql sql;

    public ReactionDaoImpl(final ISql sql) {
        this.sql = sql;
    }

    @Override
    public Future<ReactionsSummary> getReactionsSummary(String module, String resourceType, Set<String> resourceIds, UserInfos userInfos) {
        Promise<ReactionsSummary> promise = Promise.promise();

        JsonArray params = new JsonArray();
        params.add(module);
        params.add(resourceType);

        StringBuilder resourceIdsPlaceholder = new StringBuilder();
        for (String resourceId : resourceIds) {
            resourceIdsPlaceholder.append("?,");
            params.add(resourceId);
        }
        resourceIdsPlaceholder.deleteCharAt(resourceIdsPlaceholder.length()-1);

        String query = "" +
                "select resource_id, reaction_type, count(*) as reaction_counter " +
                "from resource_reaction " +
                "where module = ? " +
                "and resource_type = ? " +
                "and resource_id in (" + resourceIdsPlaceholder + ") " +
                "group by resource_id, reaction_type;";

        sql.prepared(query, params, results -> {
            Either<String, JsonArray> validatedResult = SqlResult.validResults(results);
            if (validatedResult.isRight()) {
                JsonArray queryResults = validatedResult.right().getValue();
                ReactionsSummary reactionsSummary = new ReactionsSummary();
                promise.complete(reactionsSummary);
            } else {
                promise.fail(validatedResult.left().getValue());
            }
        });
        return promise.future();
    }

    @Override
    public Future<Void> upsertReaction(String module, String resourceType, String resourceId, UserInfos userInfos, ReactionType reactionType) {
        Promise<Void> promise = Promise.promise();

        JsonArray params = new JsonArray();
        params.add(module);
        params.add(resourceType);
        params.add(resourceId);
        params.add(userInfos.getType());
        params.add(userInfos.getUserId());
        params.add(LocalDateTime.now());
        params.add(reactionType);

        String query = "" +
                "insert into audience.reactions(module, resource_type, resource_id, profile, user_id, reaction_date, reaction_type) " +
                "values (?, ?, ?, ?, ?, ?, ?) " +
                "on conflict on constraint reactions_unique_constraint do update " +
                "set reaction_date = excluded.reaction_date, " +
                "reaction_type = excluded.reaction_type;";

        sql.prepared(query, params, results -> {
            Either<String, JsonArray> validatedResult = SqlResult.validResults(results);
            if (validatedResult.isRight()) {
                promise.complete();
            } else {
                promise.fail(validatedResult.left().getValue());
            }
        });
        return promise.future();
    }

}
