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

import fr.wseduc.webutils.http.oauth.OAuth2Client;
import fr.wseduc.webutils.security.JWT;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;

public class OssFcm {

    private OAuth2Client client;
    private String accessToken;
    private long tokenExpiresDate;
    private static String url;
    private Logger log = LoggerFactory.getLogger(OssFcm.class);
    private JsonObject payload = new JsonObject();
    private PrivateKey key;


    public OssFcm(OAuth2Client client, String iss, String scope, String aud, String url, String key) throws Exception{
        this.client = client;
        this.url = url;
        payload.put("iss", iss)
                .put("scope", scope)
                .put("aud", aud);

        this.key = JWT.stringToPrivateKey(key);

    }

    public void sendNotifications(final JsonObject message) throws Exception{
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
                client.postProtectedResource(url, token, headers, message.encode(),
                        new Handler<HttpClientResponse>() {
                            @Override
                            public void handle(HttpClientResponse response) {
                                if(response.statusCode() != 200){
                                    log.error("[OssFcm.sendNotifications] request failed : " + response.statusMessage());
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
            try {
                final Long date = System.currentTimeMillis()/1000;
                payload.put("iat", Long.toString(date));
                payload.put("exp", Long.toString(date + 3600));
                client.client2LO(payload, this.key, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject json) {

                        JsonObject token = json.getJsonObject("token");
                        if ("ok".equals(json.getString("status")) && token != null) {
                            accessToken = token.getString("access_token");
                            tokenExpiresDate = date + token.getInteger("expires_in");
                            handler.handle(accessToken);
                        } else {
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
}
