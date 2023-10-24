package org.entcore.auth.services.impl;

import fr.wseduc.webutils.http.oauth.OpenIdConnectClient;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import jp.eisbahn.oauth2.server.data.DataHandler;
import jp.eisbahn.oauth2.server.data.DataHandlerFactory;
import jp.eisbahn.oauth2.server.models.AuthInfo;
import org.entcore.auth.oauth.JsonRequestAdapter;
import org.entcore.auth.oauth.OAuthDataHandler;
import org.entcore.auth.services.OpenIdServiceProviderFactory;
import org.entcore.common.neo4j.Neo4j;

import java.util.LinkedHashSet;
import java.util.Set;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;

public class OpenIdSloServiceImpl {
    private final HttpClient httpClient;
    private final DataHandlerFactory oauthDataFactory;
    private static final String CLIENT_ID = "clientId";
    private static final String USER_ID = "userId";
    private static final String SESSION_ID = "sessionId";
    private static final String LOGOUT_URL = "logoutUrl";
    private static final String SESSION_ADDRESS = "wse.session";
    protected static final Logger log = LoggerFactory.getLogger(OpenIdSloServiceImpl.class);
    private final OpenIdServiceProviderFactory openIdConnectServiceProviderFactory = null;
    private final EventBus eb;
    private final Neo4j neo4j = Neo4j.getInstance();

    public OpenIdSloServiceImpl(Vertx vertx, DataHandlerFactory oauthDataFactory) {
        this.httpClient = vertx.createHttpClient();
        this.oauthDataFactory = oauthDataFactory;
        this.eb = vertx.eventBus();
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
            data.getAuthorizationsBySessionId(obj.getString(SESSION_ID), authorizations -> {
                if (authorizations == null)
                    return;
                for (AuthInfo authorization : authorizations) {
                    data.deleteTokensByAuthId(authorization.getId());
                    JsonObject client = new JsonObject()
                            .put(SESSION_ID, obj.getString(SESSION_ID))
                            .put(USER_ID, authorization.getUserId())
                            .put(CLIENT_ID, authorization.getClientId())
                            .put(LOGOUT_URL, authorization.getLogoutUrl());
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
        final String payload = new JsonObject()
          .put("logout_token", data.getString("logout_token"))
          .encode();
        final String logoutUrl = data.getString(LOGOUT_URL);
        httpClient.request(new RequestOptions()
          .setAbsoluteURI(logoutUrl)
          .setMethod(HttpMethod.POST)
          .addHeader("content-type", "application/json")
          .addHeader("content-length", String.valueOf(payload.length())))
          .flatMap(request -> request.send(payload))
          .onSuccess(response -> {
              log.debug("Response received with status code " + response.statusCode());
              response.bodyHandler(body -> log.debug("Body: " + body.toString()));
          })
          .onFailure(th -> log.error("An error occurred while calling logout url " + logoutUrl, th));
    }

    @SuppressWarnings("deprecation")
    public void logoutWithSlo(String token, OpenIdConnectClient oic, HttpServerRequest request) {
        oic.getJwtInstance().verifyAndGet(token, payload -> {
            if (payload != null) {
                final String QUERY_SUB_CC = "MATCH (u:User {subCC : {sub}}) " + AbstractSSOProvider.RETURN_QUERY;
                final String subject = payload.getString("sub");
                neo4j.execute(QUERY_SUB_CC, new JsonObject().put("sub", subject),
                        validUniqueResultHandler(event -> {
                            if (event.isRight() && event.right().getValue().size() > 0) {
                                String userId = event.right().getValue().getString("id");

                                JsonObject sessionMessage = new JsonObject().put("action", "dropAllByUserId")
                                        .put("userId", userId);
                                eb.request(SESSION_ADDRESS, sessionMessage,
                                  (Handler<AsyncResult<Message<JsonObject>>>) message -> {
                                      if (!message.succeeded())
                                          log.error("Unable to remove session for CC user " + userId, message.cause());
                                  });
                            } else
                                log.error("Unable to find CC user (subject " + subject + ")");
                            request.response().setStatusCode(200).end();
                        }));
            } else {
                request.response().setStatusCode(401).end();

            }
        });
    }

}