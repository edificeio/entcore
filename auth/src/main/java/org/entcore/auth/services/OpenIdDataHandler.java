package org.entcore.auth.services;

import java.util.List;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import jp.eisbahn.oauth2.server.async.Handler;

public interface OpenIdDataHandler {
       void getAuthorizationsBySessionId(String sessionId, Handler<List<JsonObject>> handler);

       void getTokensByAuthId(String authId, Handler<List<JsonObject>> handler);

       void deleteTokensByAuthId(String authId);

       void getLogoutToken(String userId, String clientId, Handler<String> handler);

       void deleteAuthorization(JsonObject auth, Handler<Message<JsonObject>> callback);

}
