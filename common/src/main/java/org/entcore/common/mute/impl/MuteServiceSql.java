package org.entcore.common.mute.impl;

import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Utils;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.sqlclient.Tuple;
import org.entcore.common.explorer.IdAndVersion;
import org.entcore.common.explorer.to.MuteRequest;
import org.entcore.common.mute.MuteService;
import org.entcore.common.postgres.IPostgresClient;
import org.entcore.common.postgres.PostgresClient;
import org.entcore.common.user.UserInfos;

import java.util.Set;

/**
 * Store mute status in the same collection as the targeted resource in a set field
 * called {@link MuteServiceSql#muteFieldName} which stores the mute status (boolean)
 * for users.
 *
 * <u>Example:</u>
 * <ul>
 * <li>if only A and B have muted the resource the value of {@link MuteServiceSql#muteFieldName} will be
 * {@code {"UserIdA": true, "userIdB": true}}</li>
 * <li>if B then mutes the resource the field will be {@code {"UserIdA": true, "userIdB": false}}</li>
 */
public class MuteServiceSql implements MuteService {
    private static Logger log = LoggerFactory.getLogger(MuteServiceSql.class);
    private final String query;
    private final IPostgresClient postgresClient;
    private static final String muteFieldName = "mute";

    /**
     * @param tableName Name of the table in which the resource to mute is stored
     * @param postgresClient Client to interact with the resource SQL storage
     */
    public MuteServiceSql(final String tableName,
                          final IPostgresClient postgresClient) {
        this.postgresClient = postgresClient;
        this.query =
                "UPDATE " + tableName +
                " SET " + muteFieldName + " = jsonb_set( " + muteFieldName + ", {$1}, $2, true)" +
                " WHERE ent_id IN $3";
    }

    @Override
    public Future<Void> setMuteStatus(final MuteRequest muteRequest, final UserInfos user) {
        final Promise<Void> futureResult = Promise.promise();
        final String userId = user.getUserId();
        final Set<IdAndVersion> resourceIds = muteRequest.getResourceIds();
        Tuple tuple = Tuple.tuple()
                .addValue(userId)
                .addValue(muteRequest.isMute());
        tuple = PostgresClient.inTuple(tuple, resourceIds);
        postgresClient.queryStream(query, tuple, 1).onComplete(e -> {
            if(e.succeeded()) {
                log.debug("Successfully updated mute status of " + resourceIds);
                futureResult.complete();
            } else {
                log.error("An error occurred while updating the mute status of " + resourceIds, e.cause());
                futureResult.fail(e.cause());
            }
        });
        return futureResult.future();
    }
}
