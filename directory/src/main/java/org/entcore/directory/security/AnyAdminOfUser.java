package org.entcore.directory.security;

import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Set;

import static org.entcore.common.user.DefaultFunctions.SUPER_ADMIN;

public class AnyAdminOfUser implements ResourcesProvider {

    @Override
    public void authorize(final HttpServerRequest request, Binding binding, final UserInfos user, final Handler<Boolean> handler) {

        //Super-admin "hack"
        if(user.getFunctions().containsKey(SUPER_ADMIN)) {
            handler.handle(true);
            return;
        }
        String userId = request.params().get("userId");
        if (userId == null || userId.trim().isEmpty()) {
            handler.handle(false);
            return;
        }

        Set<String> ids = DirectoryResourcesProvider.getIds(user);
        String query =
                "MATCH (u:User {id : {userId}})-[:IN]->()-[:DEPENDS]->()-[:BELONGS*0..1]->s2 " +
                        "WHERE s2.id IN {ids} RETURN count(*) > 0 as exists UNION " +
                        "MATCH (u: User {id : {userId}})-[:HAS_RELATIONSHIPS]->(b: Backup) " +
                        "WHERE ANY(structId IN b.structureIds WHERE structId IN {ids}) " +
                        "RETURN count(*) > 0 as exists";
        JsonObject params = new JsonObject()
                .putString("id", request.params().get("groupId"))
                .putString("userId", userId)
                .putArray("ids", new JsonArray(ids.toArray()));
        request.pause();
        Neo4j.getInstance().execute(query, params, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> r) {
                request.resume();
                JsonArray res = r.body().getArray("result");
                if ("ok".equals(r.body().getString("status")) &&
                        res.size() == 2 && (((JsonObject) res.get(0)).getBoolean("exists", false)
                        || ((JsonObject) res.get(1)).getBoolean("exists", false))) {
                    handler.handle(true);
                } else {
                    handler.handle(false);
                }
            }
        });
    }
}
