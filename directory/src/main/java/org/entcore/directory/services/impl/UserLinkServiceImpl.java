package org.entcore.directory.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.directory.dto.LinkDTO;
import org.entcore.directory.services.UserLinkService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserLinkServiceImpl implements UserLinkService {

    /**
     * Nombre maximal de liens par utilisateur, contraint par l'affichage du widget.
     * Doit rester synchronise avec le CHECK ("position" BETWEEN 0 AND 9) de 001-directory-schema.sql.
     */
    private static final int MAX_LINKS_PER_USER = 10;

    private Sql sql = Sql.getInstance();

    @Override
    public Future<LinkOperationError> createLink(LinkDTO link, String userId) {
        Promise<LinkOperationError> promise = Promise.promise();
        JsonArray params = new JsonArray();
        params.add(UUID.randomUUID().toString())
              .add(userId)
              .add(link.getName())
              .add(link.getUrl())
              .add(userId);
        sql.prepared(" INSERT INTO directory.user_link(id, user_id, name, url, \"position\") " +
                        "      SELECT ?::UUID, ?, ?, ?, free.pos " +
                        "        FROM (SELECT s AS pos " +
                        "                FROM generate_series(0, " + (MAX_LINKS_PER_USER - 1) + ") AS s " +
                        "               WHERE NOT EXISTS (SELECT 1 FROM directory.user_link ul " +
                        "                                  WHERE ul.user_id = ? AND ul.\"position\" = s) " +
                        "               ORDER BY s LIMIT 1) AS free " +
                        "      ON CONFLICT (user_id, \"position\") DO NOTHING ",
                     params, message -> {
            Either<String, JsonObject> validatedResult = SqlResult.validRowsResult(message);
            if (validatedResult.isLeft()) {
                promise.fail(validatedResult.left().getValue());
            } else if (validatedResult.right().getValue().getLong("rows", 0L) == 0L) {
                // Aucune ligne insérée : plus aucun emplacement libre (limite atteinte), ou
                // emplacement pris entre-temps par une requête concurrente (ON CONFLICT DO NOTHING).
                promise.complete(new LinkOperationError(409, "directory.user.link.limit.reached"));
            } else {
                promise.complete(new LinkOperationError(200, null));
            }
        });
        return promise.future();
    }

    @Override
    public Future<List<LinkDTO>> getLinks(String userId) {
        Promise<List<LinkDTO>> promise = Promise.promise();
        sql.prepared("SELECT id, user_id, name, url FROM directory.user_link WHERE user_id = ? ORDER BY lower(name) ASC NULLS LAST, id",
                new JsonArray().add(userId),
                message -> {
                Either<String, JsonArray> validatedResult = SqlResult.validResult(message);
                if (validatedResult.isLeft()) {
                    promise.fail(validatedResult.left().getValue());
                } else {
                    List<LinkDTO> result = validatedResult.right().getValue()
                            .stream()
                            .map(JsonObject.class::cast)
                            .map( o -> o.mapTo(LinkDTO.class))
                            .collect(Collectors.toList());
                    promise.complete(result);
                }
        });
        return promise.future();
    }

    @Override
    public Future<LinkOperationError> deleteLink(LinkDTO link, String userId) {
        Promise<LinkOperationError> promise = Promise.promise();
        sql.prepared("DELETE FROM directory.user_link WHERE user_id = ? AND id = ?::UUID ",
                new JsonArray().add(userId).add(link.getId().toString()),
                message -> {
                    Either<String, JsonObject> validatedResult = SqlResult.validRowsResult(message);
                    if (validatedResult.isLeft()) {
                        promise.fail(validatedResult.left().getValue());
                    }  else if (validatedResult.right().getValue().getLong("rows", 0L) == 0L) {
                        promise.complete(new LinkOperationError(400, "Bad request"));
                    }  else {
                        promise.complete(new LinkOperationError(200, null));
                    }
                });
        return promise.future();
    }

    @Override
    public Future<Void> deleteUserLinks(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Future.succeededFuture();
        }
        Promise<Void> promise = Promise.promise();
        sql.prepared("DELETE FROM directory.user_link WHERE user_id IN " + Sql.listPrepared(userIds),
                new JsonArray(userIds),
                message -> {
                    Either<String, JsonObject> validatedResult = SqlResult.validRowsResult(message);
                    if (validatedResult.isLeft()) {
                        promise.fail(validatedResult.left().getValue());
                    } else {
                        promise.complete();
                    }
                });
        return promise.future();
    }
}
