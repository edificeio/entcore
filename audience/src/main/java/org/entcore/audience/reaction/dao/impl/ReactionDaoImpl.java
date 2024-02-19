package org.entcore.audience.reaction.dao.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.collections4.CollectionUtils;
import org.entcore.audience.reaction.dao.ReactionDao;
import org.entcore.audience.reaction.model.ReactionType;
import org.entcore.audience.reaction.model.ReactionsSummary;
import org.entcore.common.sql.ISql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ReactionDaoImpl implements ReactionDao {

    private final ISql sql;

    public ReactionDaoImpl(final ISql sql) {
        this.sql = sql;
    }

    @Override
    public Future<ReactionsSummary> getReactionsSummary(String module, String resourceType, Set<String> resourceIds, UserInfos userInfos) {
        Promise<ReactionsSummary> promise = Promise.promise();
        if(CollectionUtils.isEmpty(resourceIds)) {
            promise.complete(new ReactionsSummary(new HashMap<>()));
        } else {
            JsonArray params = new JsonArray();
            params.add(module);
            params.add(resourceType);

            StringBuilder resourceIdsPlaceholder = new StringBuilder();
            for (String resourceId : resourceIds) {
                resourceIdsPlaceholder.append("?,");
                params.add(resourceId);
            }
            resourceIdsPlaceholder.deleteCharAt(resourceIdsPlaceholder.length() - 1);

            String query =
                "select resource_id, reaction_type, count(*) as reaction_counter " +
                    "from audience.reactions " +
                    "where module = ? " +
                    "and resource_type = ? " +
                    "and resource_id in (" + resourceIdsPlaceholder + ") " +
                    "group by resource_id, reaction_type;";

            sql.prepared(query, params, results -> {
                Either<String, JsonArray> validatedResult = SqlResult.validGroupedResults(results);
                if (validatedResult.isRight()) {
                    final JsonArray queryResults = validatedResult.right().getValue();
                    final Map<String, ReactionsSummary.ReactionsSummaryForResource> reactions = new HashMap<>();
                    queryResults.forEach(r -> {
                        final JsonObject result = (JsonObject) r;
                        final String resourceId = result.getString("resource_id");
                        final ReactionsSummary.ReactionsSummaryForResource reactionsForResource = reactions.computeIfAbsent(resourceId, (k) -> new ReactionsSummary.ReactionsSummaryForResource(new HashMap<>()));
                        final String type = result.getString("reaction_type");
                        final Integer count = result.getInteger("reaction_counter", 0);
                        reactionsForResource.getCountByType().compute(type, (k, v) -> {
                            if(v == null) {
                                return count;
                            } else {
                                return count + v;
                            }
                        });
                    });
                    promise.complete(new ReactionsSummary(reactions));
                } else {
                    promise.fail(validatedResult.left().getValue());
                }
            });
        }
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
