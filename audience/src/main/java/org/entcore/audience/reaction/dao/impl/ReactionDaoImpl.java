package org.entcore.audience.reaction.dao.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.collections4.CollectionUtils;
import org.entcore.audience.reaction.dao.ReactionDao;
import org.entcore.audience.reaction.model.ReactionCounters;
import org.entcore.audience.reaction.model.UserReaction;
import org.entcore.common.sql.ISql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.*;

public class ReactionDaoImpl implements ReactionDao {

    private final ISql sql;

    public ReactionDaoImpl(final ISql sql) {
        this.sql = sql;
    }

    @Override
    public Future<Map<String, ReactionCounters>> getReactionsCountersByResource(String module, String resourceType, Set<String> resourceIds) {
        Promise<Map<String, ReactionCounters>> promise = Promise.promise();
        if(CollectionUtils.isEmpty(resourceIds)) {
            promise.complete(new HashMap<>());
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
                    final Map<String, ReactionCounters> reactions = new HashMap<>();
                    queryResults.forEach(r -> {
                        final JsonObject result = (JsonObject) r;
                        final String resourceId = result.getString("resource_id");
                        final ReactionCounters reactionsForResource = reactions.computeIfAbsent(resourceId, (k) -> new ReactionCounters(new HashMap<>()));
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
                    promise.complete(reactions);
                } else {
                    promise.fail(validatedResult.left().getValue());
                }
            });
        }
        return promise.future();
    }

    @Override
    public Future<Map<String, String>> getUserReactionByResource(String module, String resourceType, Set<String> resourceIds, UserInfos userInfos) {
        Promise<Map<String, String>> promise = Promise.promise();

        if (CollectionUtils.isEmpty(resourceIds)) {
            promise.complete(new HashMap<>());
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
            params.add(userInfos.getUserId());

            String userReactionQuery = "select resource_id, reaction_type as user_reaction " +
                    "from audience.reactions " +
                    "where module = ? " +
                    "and resource_type = ? " +
                    "and resource_id in (" + resourceIdsPlaceholder + ") " +
                    "and user_id = ?";

            sql.prepared(userReactionQuery, params, results -> {
                Either<String, JsonArray> validatedResults = SqlResult.validGroupedResults(results);
                if (validatedResults.isRight()) {
                    final Map<String, String> userReactionByResource = new HashMap<>();
                    final JsonArray queryResults = validatedResults.right().getValue();
                    queryResults.forEach(r -> {
                        final JsonObject result = (JsonObject) r;
                        userReactionByResource.put(result.getString("resource_id"), result.getString("user_reaction"));
                    });
                    promise.complete(userReactionByResource);
                } else {
                    promise.fail(validatedResults.left().getValue());
                }
            });
        }
        return promise.future();
    }

    @Override
    public Future<List<UserReaction>> getUserReactions(String module, String resourceType, String resourceId, int page, int size) {
        Promise<List<UserReaction>> promise = Promise.promise();

        JsonArray params = new JsonArray();
        params.add(module);
        params.add(resourceType);
        params.add(resourceId);
        params.add(size);
        params.add((page-1)*size);

        String query = "" +
                "select user_id, profile, reaction_type " +
                "from audience.reactions " +
                "where module = ? " +
                "and resource_type = ? " +
                "and resource_id = ? " +
                "order by reaction_date " +
                "limit ? " +
                "offset ?";

        sql.prepared(query, params, results -> {
            Either<String, JsonArray> validatedResults = SqlResult.validResults(results);
            if (validatedResults.isRight()) {
                final List<UserReaction> userReactions = new ArrayList<>();
                final JsonArray queryResults = validatedResults.right().getValue();
                queryResults.forEach(r -> {
                    JsonObject result = (JsonObject) r;
                    userReactions.add(new UserReaction(result.getString("user_id"), result.getString("profile"), result.getString("reaction_type")));
                });
                promise.complete(userReactions);
            } else {
                promise.fail(validatedResults.left().getValue());
            }
        });

        return promise.future();
    }

    @Override
    public Future<Void> upsertReaction(String module, String resourceType, String resourceId, UserInfos userInfos, String reactionType) {
        Promise<Void> promise = Promise.promise();

        JsonArray params = new JsonArray();
        params.add(module);
        params.add(resourceType);
        params.add(resourceId);
        params.add(userInfos.getType());
        params.add(userInfos.getUserId());
        params.add(reactionType);

        String query =
                "insert into audience.reactions(module, resource_type, resource_id, profile, user_id, reaction_type) " +
                "values (?, ?, ?, ?, ?, ?) " +
                "on conflict on constraint reactions_unique_constraint do update " +
                "set reaction_date = excluded.reaction_date, " +
                "reaction_type = excluded.reaction_type";

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

    @Override
    public Future<Void> deleteReaction(String module, String resourceType, String resourceId, UserInfos userInfos) {
        Promise<Void> promise = Promise.promise();

        JsonArray params = new JsonArray();
        params.add(module);
        params.add(resourceType);
        params.add(resourceId);
        params.add(userInfos.getUserId());

        String query = "delete from audience.reactions " +
                "where module = ? " +
                "and resource_type = ? " +
                "and resource_id = ? " +
                "and user_id = ?";

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
