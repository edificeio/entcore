package org.entcore.auth.services.impl;

import java.util.LinkedHashSet;
import java.util.Set;

import org.entcore.auth.oauth.JsonRequestAdapter;
import org.entcore.auth.oauth.OAuthDataHandler;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import jp.eisbahn.oauth2.server.data.DataHandler;
import jp.eisbahn.oauth2.server.data.DataHandlerFactory;

import static fr.wseduc.webutils.Utils.*;

public class OpenIdSloServiceImpl {
    private final HttpClient httpClient;
    private final DataHandlerFactory oauthDataFactory;
    private static final String CLIENT_ID = "clientId";
    private static final String USER_ID = "userId";
    private static final String SESSION_ID = "sessionId";
    private static final String LOGOUT_URL = "logoutUrl";
    protected static final Logger log = LoggerFactory.getLogger(OpenIdSloServiceImpl.class);

    public OpenIdSloServiceImpl(Vertx vertx, DataHandlerFactory oauthDataFactory) {
        this.httpClient = vertx.createHttpClient();
        this.oauthDataFactory = oauthDataFactory;
    }

    public void sloOpenId(final Message<JsonObject> message) {
        JsonObject logoutToken = new JsonObject();
        final Set<JsonObject> clients = new LinkedHashSet<>();
        if (message.body() != null && isNotEmpty(message.body().getString(SESSION_ID))
                && isNotEmpty(message.body().getString(USER_ID))) {
            JsonObject obj = new JsonObject()
                    .put(USER_ID, message.body().getString(USER_ID))
                    .put(SESSION_ID, message.body().getString(SESSION_ID));
            final DataHandler data = oauthDataFactory.create(new JsonRequestAdapter(obj));
            ((OAuthDataHandler) data).getAuthorizationsBySessionId(obj.getString(SESSION_ID), authorizations -> {
                if (authorizations == null)
                    return;
                for (JsonObject authorization : authorizations) {
                    ((OAuthDataHandler) data).deleteTokensByAuthId(authorization.getString("id"));
                    JsonObject client = new JsonObject()
                            .put(SESSION_ID, authorization.getString(SESSION_ID))
                            .put(USER_ID, authorization.getString(USER_ID))
                            .put(CLIENT_ID, authorization.getString(CLIENT_ID))
                            .put(LOGOUT_URL, authorization.getString(LOGOUT_URL));
                    clients.add(client);
                    ((OAuthDataHandler) data).deleteAuthorization(client, res -> {
                        if (res.body() != null)
                            log.debug("Authorization deleted");
                    });
                }
                for (JsonObject auth : clients) {
                    if (auth.getString(LOGOUT_URL) != null) {
                        ((OAuthDataHandler) data).getLogoutToken(auth.getString(
                                USER_ID),
                                auth.getString(CLIENT_ID),
                                response -> {
                                    logoutToken.put("logout_token", response).put(LOGOUT_URL,
                                            auth.getString(LOGOUT_URL));
                                    if (response != null) {
                                        sendRequest(logoutToken);
                                    }
                                });
                    } else {
                        log.error("logout url is is empty");
                    }
                }

            });
        }
    }

    @SuppressWarnings("deprecation")
    private void sendRequest(JsonObject data) {
        HttpClientRequest request = this.httpClient
                .postAbs(data.getString(LOGOUT_URL), response -> {
                    log.debug("Response received with status code " + response.statusCode());
                    response.bodyHandler(body -> log.debug("Body: " + body.toString()));
                })
                .putHeader("content-type", "application/json")
                .putHeader("content-length",
                        String.valueOf(
                                new JsonObject().put("logout_token", data.getString("logout_token")).toString()
                                        .length()))
                .write(new JsonObject().put("logout_token", data.getString("logout_token")).toString());
        request.end();
    }
}
