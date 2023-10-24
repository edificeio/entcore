/*
 * Copyright © "Open Digital Education", 2018
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.
 */

package org.entcore.common.notification.ws;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.http.oauth.OAuth2Client;
import fr.wseduc.webutils.security.JWT;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.micrometer.backends.BackendRegistries;

import static fr.wseduc.webutils.Utils.isEmpty;
import static fr.wseduc.webutils.Utils.isNotEmpty;

import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;

import org.entcore.common.notification.NotificationUtils;

public class OssFcm {

    private OAuth2Client client;
    private String accessToken;
    private long tokenExpiresDate;
    private String url;
    private Logger log = LoggerFactory.getLogger(OssFcm.class);
    private JsonObject payload = new JsonObject();
    private PrivateKey key;
    private final boolean logPushNotifs;
    private final boolean removeTokenIf404;
    private MongoDb mongoDb;
    private Counter sendMessageCounter;
    private Counter sendMessageOkCounter;
    private Counter sendMessageKoCounter;
    private Counter accessTokenCounter;
    private Counter accessTokenOkCounter;
    private Counter accessTokenKoCounter;
    private Counter removeInvalidTokenCounter;
    private Counter removeInvalidTokenOkCounter;
    private Counter removeInvalidTokenKoCounter;

    public OssFcm(OAuth2Client client, String iss, String scope, String aud, String url, String key) throws Exception{
        this(client, iss, scope, aud, url, key, false, false);
    }

    public OssFcm(OAuth2Client client, String iss, String scope, String aud, String url, String key,
            boolean logPushNotifs, boolean removeTokenIf404) throws Exception{
        this.client = client;
        this.url = url;
        payload.put("iss", iss)
                .put("scope", scope)
                .put("aud", aud);

        this.key = JWT.stringToPrivateKey(key);
        this.logPushNotifs = logPushNotifs;
        if (this.logPushNotifs) {
            mongoDb = MongoDb.getInstance();
        }
        this.removeTokenIf404 = removeTokenIf404;
        final MeterRegistry registry = BackendRegistries.getDefaultNow();
        if (registry != null) {
            sendMessageCounter = Counter.builder("ossfcm.send.message")
                .description("number of message sent")
                .register(registry);
            sendMessageOkCounter = Counter.builder("ossfcm.send.message.ok")
                .description("number of message sent successfully")
                .register(registry);
            sendMessageKoCounter = Counter.builder("ossfcm.send.message.ko")
                .description("number of message sent failed")
                .register(registry);
            accessTokenCounter = Counter.builder("ossfcm.access.token")
                .description("number of access token requested")
                .register(registry);
            accessTokenOkCounter = Counter.builder("ossfcm.access.token.ok")
                .description("number of access token requested")
                .register(registry);
            accessTokenKoCounter = Counter.builder("ossfcm.access.token.ko")
                .description("number of access token requested")
                .register(registry);
            removeInvalidTokenCounter = Counter.builder("ossfcm.remove.invalid.token")
                .description("number of access token requested")
                .register(registry);
            removeInvalidTokenOkCounter = Counter.builder("ossfcm.remove.invalid.token.ok")
                .description("number of access token requested")
                .register(registry);
            removeInvalidTokenKoCounter = Counter.builder("ossfcm.remove.invalid.token.ko")
                .description("number of access token requested")
                .register(registry);
        }
    }

    public void sendNotifications(final JsonObject message) throws Exception{
        sendNotifications(null, message);
    }

    public void sendNotifications(final String userId, final JsonObject message) throws Exception{
        getAccessToken(new Handler<String>() {
            @Override
            public void handle(String token) {
                if(token == null){
                    log.error("[OssFcm] Error get token");
                    return;
                }
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-type", "application/json");
                headers.put("Accept-Language", message.getString("language", "fr"));
                sendMessageCounter.increment();
                if (isNotEmpty(userId)) {
                    message.getJsonObject("message").getJsonObject("data").put("receiver", userId);
                }
                client.postProtectedResource(url, token, headers, message.encode(),
                        new Handler<HttpClientResponse>() {
                            @Override
                            public void handle(HttpClientResponse response) {
                                if(response.statusCode() != 200) {
                                    sendMessageKoCounter.increment();
                                    log.error("[OssFcm.sendNotifications] request failed : status=" + response.statusCode()+ "/ message="+response.statusMessage()+"/ url="+response.request().absoluteURI()+"/ token="+token);
                                    if (removeTokenIf404 && (response.statusCode() == 404 || response.statusCode() == 403)) {
                                        removeInvalidToken(userId, message);
                                    }
                                } else {
                                    sendMessageOkCounter.increment();
                                }
                                if (logPushNotifs) {
                                    final JsonObject resp = new JsonObject().put("status", response.statusCode());
                                    mongoDb.insert("logpushnotifs", message.put("logcreated", MongoDb.now()).put("resp", resp));
                                }
                            }
                        });

            }
        });
    }

    private void getAccessToken(final Handler<String> handler) throws Exception{
        if(accessToken != null && tokenExpiresDate > (System.currentTimeMillis() + 1000)/1000){
            handler.handle(accessToken);
        }else{
            accessTokenCounter.increment();
            try {
                final Long date = System.currentTimeMillis()/1000;
                payload.put("iat", Long.toString(date));
                payload.put("exp", Long.toString(date + 3600));
                client.client2LO(payload, this.key, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject json) {

                        JsonObject token = json.getJsonObject("token");
                        if ("ok".equals(json.getString("status")) && token != null) {
                            accessTokenOkCounter.increment();
                            accessToken = token.getString("access_token");
                            tokenExpiresDate = date + token.getInteger("expires_in");
                            handler.handle(accessToken);
                        } else {
                            accessTokenKoCounter.increment();
                            handler.handle(null);
                        }
                    }
                });
            } catch (UnsupportedEncodingException e) {
                log.error(e.getMessage(), e);
                handler.handle(null);
            }
        }
    }

    private void removeInvalidToken(String userId, JsonObject message) {
        removeInvalidTokenCounter.increment();
        final JsonObject mes = message.getJsonObject("message");
        if (isEmpty(userId)) {
            if (isNotEmpty(mes.getString("topic"))) {
                log.error("[OssFcm.removeInvalidToken] userId= null, topic= " + mes.getString("topic"));
            }
            if (isNotEmpty(mes.getString("condition"))) {
                log.error("[OssFcm.removeInvalidToken] userId= null, condition= " + mes.getString("condition"));
            }
            if (isNotEmpty(mes.getString("token"))) {
                log.error("[OssFcm.removeInvalidToken] userId= null, token= " + mes.getString("token"));
            }
        } else if (isNotEmpty(mes.getString("token"))) {
            log.info("[OssFcm.removeInvalidToken] try remove token= " + mes.getString("token") + ", user= " + userId);
            NotificationUtils.deleteFcmToken(userId, mes.getString("token"), e -> {
                if (e.isRight()) {
                    removeInvalidTokenOkCounter.increment();
                    log.info("[OssFcm.removeInvalidToken] Removed token= " + mes.getString("token") + ", user= " + userId);
                } else {
                    removeInvalidTokenKoCounter.increment();
                    log.error("[OssFcm.removeInvalidToken] Error when remove token " +
                            mes.getString("token") + ", user= " + userId + " : " + e.left().getValue());
                }
            });
        } else {
            log.error("[OssFcm.removeInvalidToken] Missing token in message for user= " + userId);
        }
    }

}
