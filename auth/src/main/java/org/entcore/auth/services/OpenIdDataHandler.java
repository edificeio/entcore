package org.entcore.auth.services;


import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import jp.eisbahn.oauth2.server.async.Handler;

public interface OpenIdDataHandler {

       void deleteTokensByAuthId(String authId);

       void getLogoutToken(String userId, String clientId, Handler<String> handler);

       void deleteAuthorization(JsonObject auth, Handler<Message<JsonObject>> callback);

}
