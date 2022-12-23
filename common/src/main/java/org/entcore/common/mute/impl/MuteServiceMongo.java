package org.entcore.common.mute.impl;

import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Utils;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.explorer.IdAndVersion;
import org.entcore.common.explorer.to.MuteRequest;
import org.entcore.common.mute.MuteService;
import org.entcore.common.user.UserInfos;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Store mute status in the same collection as the targeted resource in a field
 * called {@link MuteServiceMongo#muteFieldName}.
 */
public class MuteServiceMongo implements MuteService {
    private static Logger log = LoggerFactory.getLogger(MuteServiceMongo.class);
    private final String collectionName;
    private final MongoDb mongo;
    private static final String muteFieldName = "mute";

    public MuteServiceMongo(String collectionName, MongoDb mongo) {
        this.collectionName = collectionName;
        this.mongo = mongo;
    }

    @Override
    public Future<Void> setMuteStatus(final MuteRequest muteRequest, final UserInfos user) {
        final Promise<Void> futureResult = Promise.promise();
        final String userId = user.getUserId();
        final Set<String> resourceIds = muteRequest.getResourceIds().stream().map(id -> id.getId()).collect(Collectors.toSet());
        final QueryBuilder query = QueryBuilder.start("_id").in(resourceIds);
        final JsonObject q = MongoQueryBuilder.build(query);
        final MongoUpdateBuilder updateQuery = new MongoUpdateBuilder();
        if(muteRequest.isMute()) {
            updateQuery.addToSet(muteFieldName, userId);
        } else {
            updateQuery.pull(muteFieldName, userId);
        }

        mongo.update(collectionName, q, updateQuery.build(),res -> {
            final Either<String, JsonObject> result = Utils.validResult(res);
            if(result.isLeft()) {
                futureResult.fail(result.left().getValue());
            } else {
                futureResult.complete();
            }
        });
        return futureResult.future();
    }
}
