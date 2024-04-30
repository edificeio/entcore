package org.entcore.communication.filters;

import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;

public class CommunicationDiscoverVisibleFilter implements ResourcesProvider {

    private final Neo4j neo4j = Neo4j.getInstance();


    @Override
    public void authorize(HttpServerRequest resourceRequest, Binding binding,
                          UserInfos user, Handler<Boolean> handler) {

        if(user == null) {
            handler.handle(false);
            return;
        }

        String groupId = resourceRequest.params().get("groupId");
        if(groupId != null && !groupId.trim().isEmpty()) {
            String query = "MATCH (g:CommunityGroup:Group:Visible {id: {groupId}})<-[:IN|COMMUNIQUE]-(u:User {id: {userId}}) " +
                            "RETURN COUNT(u) > 0 as exists";

            JsonObject params = new JsonObject().put("userId", user.getUserId()).put("groupId", groupId);

            check(query, params, handler);
        }

        handler.handle(true);
    }

    private void check(String query, JsonObject params, Handler<Boolean> handler) {
        neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonArray r = event.body().getJsonArray("result");
                handler.handle(
                        "ok".equals(event.body().getString("status")) &&
                                r != null && r.size() == 1 &&
                                (r.getJsonObject(0)).getBoolean("exists", false)
                );
            }
        });
    }
}
