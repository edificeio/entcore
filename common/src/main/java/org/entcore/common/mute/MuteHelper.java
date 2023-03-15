package org.entcore.common.mute;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;

import java.util.HashSet;
import java.util.Set;

public class MuteHelper {

    /**
     * Bus address of the function that retrieves the ids of the users who
     * muted a resource.
     */
    public final static String FETCH_RESOURCE_MUTES_BY_ENTID_ADRESS = "mute.fetch.by.entid";

    public final Vertx vertx;

    public MuteHelper(final Vertx vertx) {
        this.vertx = vertx;
    }

    public Future<Set<String>> fetResourceMutesByEntId(final String resourceEntId) {
        final Promise<Set<String>> promise = Promise.promise();
        vertx.eventBus().request(FETCH_RESOURCE_MUTES_BY_ENTID_ADRESS, resourceEntId, e -> {
            if(e.succeeded()) {
                final JsonArray userIds = (JsonArray) e.result().body();
                final Set<String> userIdsSet = new HashSet<>();
                for (Object userId : userIds) {
                    userIdsSet.add((String) userId);
                }
                promise.complete(userIdsSet);
            } else {
                promise.fail(e.cause());
            }
        });
        return promise.future();
    }
}
