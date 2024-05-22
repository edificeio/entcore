package org.entcore.auth.services.impl;

import fr.wseduc.webutils.Either;

import org.entcore.auth.services.OpenIdConnectServiceProvider;
import org.entcore.common.neo4j.Neo4j;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.eventbus.EventBus;

import java.security.NoSuchAlgorithmException;

import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;

public class WordlineServiceProvider implements OpenIdConnectServiceProvider {
    protected static final Logger log = LoggerFactory.getLogger(WordlineServiceProvider.class);

    private final String iss;
    private final Neo4j neo4j = Neo4j.getInstance();
    private final EventBus eb;

    public static final String SCOPE_OPENID = "openid";

    private static final String SESSION_ADDRESS = "wse.session";

    private static final String QUERY_SUB_CC = "MATCH (u:User {subCC : {sub}}) " + AbstractSSOProvider.RETURN_QUERY;
    private static final String QUERY_PIVOT_CC = "MATCH (u:User) WHERE u.id = {userId} AND NOT(HAS(u.subCC)) "
            +
            "SET u.subCC = {sub}, u.federated = {setFederated} " +
            "WITH u " + AbstractSSOProvider.RETURN_QUERY;
    private static final String QUERY_MAPPING_CC = "MATCH (n:User {login:{login}}) " +
            "WHERE NOT(HAS(n.subCC)) " +
            "RETURN n.password as password, n.activationCode as activationCode ";
    private static final String QUERY_SET_MAPPING_CC = "MATCH (u:User {login:{login}}) " +
            "WHERE NOT(HAS(u.subCC)) " +
            "SET u.subCC = {sub}, u.federated = {setFederated} " +
            "WITH u " + AbstractSSOProvider.RETURN_QUERY;
    private boolean setFederated = true;

    public WordlineServiceProvider(String iss, String clientId, String secret, EventBus eb) {
        this.iss = iss;
        this.eb = eb;
    }

    @Override
    public void executeFederate(final JsonObject payload, final Handler<Either<String, Object>> handler) {
        if (this.isPayloadValid(payload, this.iss) == true) {
            neo4j.execute(QUERY_SUB_CC, payload, validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
                @Override
                public void handle(final Either<String, JsonObject> event) {
                    if (event.isRight() && event.right().getValue().getBoolean("blockedProfile", false)) {
                        handler.handle(new Either.Left<String, Object>("blocked.profile"));
                    } else if (event.isRight() && event.right().getValue().size() > 0) {
                        handler.handle(new Either.Right<String, Object>(event.right().getValue()));
                    } else {
                        federateWithPivot(payload, handler);
                    }
                }
            }));
        } else {
            handler.handle(new Either.Left<String, Object>("invalid.openid.payload"));
        }
    }

    private void federateWithPivot(JsonObject payload, final Handler<Either<String, Object>> handler) {
        payload.put("setFederated", setFederated);
        neo4j.execute(QUERY_PIVOT_CC, payload, validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(final Either<String, JsonObject> event) {
                if (event.isRight() && event.right().getValue().getBoolean("blockedProfile", false)) {
                    handler.handle(new Either.Left<String, Object>("blocked.profile"));
                } else if (event.isRight() && event.right().getValue().size() > 0) {
                    handler.handle(new Either.Right<String, Object>(event.right().getValue()));
                } else {
                    handler.handle(new Either.Left<String, Object>(UNRECOGNIZED_USER_IDENTITY));
                }
            }
        }));
    }

    @Override
    public void mappingUser(String login, final String password, final JsonObject payload,
            final Handler<Either<String, Object>> handler) {
        final JsonObject params = new JsonObject().put("login", login).put("password", password);
        neo4j.execute(QUERY_MAPPING_CC, params, validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    JsonObject res = event.right().getValue();
                    try {
                        if (checkPassword(password, res.getString("password"),
                                res.getString("activationCode")) == true) {
                            params.put("setFederated", setFederated);
                            neo4j.execute(QUERY_SET_MAPPING_CC, params.put("sub", payload.getString("sub")),
                                    validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
                                        @Override
                                        public void handle(final Either<String, JsonObject> event) {
                                            if (event.isRight()
                                                    && event.right().getValue().getBoolean("blockedProfile", false)) {
                                                handler.handle(new Either.Left<String, Object>("blocked.profile"));
                                            } else if (event.isRight()) {
                                                handler.handle(
                                                        new Either.Right<String, Object>(event.right().getValue()));
                                            } else {
                                                handler.handle(
                                                        new Either.Left<String, Object>("invalid.openid.payload"));
                                            }
                                        }
                                    }));
                        } else
                            handler.handle(new Either.Left<String, Object>("auth.error.authenticationFailed"));
                    } catch (NoSuchAlgorithmException e) {
                        handler.handle(new Either.Left<String, Object>(e.getMessage()));
                    }
                } else {
                    handler.handle(new Either.Left<String, Object>(event.left().getValue()));
                }
            }
        }));
    }

    @Override
    public String getScope() {
        return SCOPE_OPENID;
    }

    public void setSetFederated(boolean setFederated) {
        this.setFederated = setFederated;
    }
}
