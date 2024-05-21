package org.entcore.auth.services.impl;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import jp.eisbahn.oauth2.server.exceptions.OAuthError;
import jp.eisbahn.oauth2.server.models.UserData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.entcore.common.neo4j.Neo4j;

import fr.wseduc.webutils.security.JWT;
import static fr.wseduc.webutils.Utils.isNotEmpty;

public class JwtVerifier {

    public final ConcurrentMap<String, JWT> jwtInstances = new ConcurrentHashMap<>();
    private final Neo4j neo4j = Neo4j.getInstance();
    private final Vertx vertx;
    private static final Logger log = LoggerFactory.getLogger(JwtVerifier.class);

    public JwtVerifier(Vertx vertx) {
        this.vertx = vertx;
    }

    public void verifyJWT(String clientId, String token, Handler<String> handler)
            throws OAuthError {
        try {
            if (jwtInstances.get(clientId) != null) {
                jwtInstances.get(clientId).verifyAndGet(token, payload -> {
                    if (payload != null) {
                        handler.handle(payload.getString("userId"));
                    } else {
                        handler.handle(null);
                    }
                });
            } else {
                handler.handle(null);
                throw new Exception();
            }
        } catch (Exception e) {
            handler.handle(null);
            log.error("Error signature verification failed : " + e);
        }
    }

    public void getUserByExternalId(String id, final Handler<JsonObject> handler) {
        String query = "Match (u:User {id:{id}}) OPTIONAL MATCH (p:Profile) WHERE HAS(u.profiles) AND p.name = head(u.profiles) RETURN DISTINCT u.id as id, u.activationCode as activationCode, u.login as login, u.email as email, u.mobile as mobile, u.federated, u.blocked as blockedUser, p.blocked as blockedProfile, u.source as source";
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        neo4j.execute(query, params, new io.vertx.core.Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> res) {
                JsonArray a = res.body().getJsonArray("result");
                if ("ok".equals(res.body().getString("status")) && a != null && a.size() == 1) {
                    JsonObject r = a.getJsonObject(0);
                    handler.handle(r);
                } else {
                    handler.handle(null);
                }
            }
        });
    }

    public UserData json2UserData(JsonObject j) {
        if (j == null) {
            return null;
        } else if (isNotEmpty(j.getString("activationCode"))) {
            return new UserData(j.getString("id"), j.getString("activationCode"),
                    j.getString("login"), j.getString("email"), j.getString("mobile"), j.getString("source"));
        } else {
            return new UserData(j.getString("id"));
        }
    }

}
