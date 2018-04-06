package org.entcore.common.notification;

import fr.wseduc.webutils.Either;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import org.entcore.common.neo4j.Neo4j;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;

public class NotificationUtils {
    private static final String USERBOOK_ADDRESS = "userbook.preferences";


    public static void getUsersPreferences(EventBus eb, JsonArray userIds, String fields, final Handler<JsonArray> handler){
        eb.send(USERBOOK_ADDRESS, new JsonObject()
                .put("action", "get.userlist")
                .put("application", "timeline")
                .put("additionalMatch", ", u-[:IN]->(g:Group)-[:AUTHORIZED]->(r:Role)-[:AUTHORIZE]->(act:WorkflowAction) ")
                .put("additionalWhere", "AND act.name = \"org.entcore.timeline.controllers.TimelineController|mixinConfig\" ")
                .put("additionalCollectFields", ", " + fields)
                .put("userIds", userIds), handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> event) {
                if (!"error".equals(event.body().getString("status"))) {
                    handler.handle(event.body().getJsonArray("results"));
                } else {
                    handler.handle(null);
                }
            }
        }));
    }

    public static void putFcmToken(String userId, String fcmToken, Handler<Either<String, JsonObject>> handler){
        final JsonObject params = new JsonObject().put("userId", userId).put("fcmToken", fcmToken);

        String query = "MATCH (u:User {id: {userId}}) MERGE (u)-[:PREFERS]->(uac:UserAppConf)" +
                "ON CREATE SET uac.fcmTokens = [{fcmToken}] " +
                "ON MATCH SET uac.fcmTokens = FILTER(token IN coalesce(uac.fcmTokens, []) WHERE token <> {fcmToken}) + {fcmToken}";

        Neo4j.getInstance().execute(query, params, validUniqueResultHandler(handler));

    }

    public static void getFcmTokensByUser(String userId, final Handler<Either<String, JsonArray>> handler){
        final JsonObject params = new JsonObject().put("userId", userId);

        String query = "MATCH (u:User {id:{userId}})-[:PREFERS]->(uac:UserAppConf)"
                +" RETURN uac.fcmTokens AS tokens";

        Neo4j.getInstance().execute(query, params, validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if(event.isRight()){
                        JsonArray result = event.right().getValue().getJsonArray("tokens");
                    if (result == null)
                        result = new JsonArray();
                    handler.handle(new Either.Right<String, JsonArray>(result));
                }else {
                    handler.handle(new Either.Left<String, JsonArray>(event.left().getValue()));
                }
            }
        }));
    }

    public static void deleteFcmToken(String userId, String fcmToken, Handler<Either<String, JsonObject>> handler){
        final JsonObject params = new JsonObject().put("userId", userId).put("fcmToken", fcmToken);

        String query = "MATCH (u:User {id: {userId}}) MERGE (u)-[:PREFERS]->(uac:UserAppConf)" +
                "ON CREATE SET uac.fcmTokens = [{fcmToken}] " +
                "ON MATCH SET uac.fcmTokens = FILTER(token IN coalesce(uac.fcmTokens, []) WHERE token <> {fcmToken})";

        Neo4j.getInstance().execute(query, params, validUniqueResultHandler(handler));

    }

    public static void getFcmTokensByUsers(JsonArray userIds,final Handler<Either<String, JsonArray>> handler){
        final JsonObject params = new JsonObject().put("userIds", userIds);

        String query = "MATCH (u:User)-[:PREFERS]->(uac:UserAppConf) WHERE u.id IN {userIds} " +
                " UNWIND(uac.fcmTokens) as token WITH DISTINCT token RETURN collect(token) as tokens";

        Neo4j.getInstance().execute(query, params, validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if(event.isRight()){
                    JsonArray result = event.right().getValue().getJsonArray("tokens");
                    if (result == null)
                        result = new JsonArray();
                    handler.handle(new Either.Right<String, JsonArray>(result));
                }else {
                    handler.handle(new Either.Left<String, JsonArray>(event.left().getValue()));
                }
            }
        }));
    }
}

