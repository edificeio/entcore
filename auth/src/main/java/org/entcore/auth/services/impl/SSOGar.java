package org.entcore.auth.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.opensaml.saml2.core.Assertion;


public class SSOGar extends AbstractSSOProvider {
    private static final Logger log = LoggerFactory.getLogger(SSOGar.class);

    @Override
    public void generate(EventBus eb, String userId, String host, String serviceProviderEntityId, Handler<Either<String, JsonArray>> handler) {
        String query = "match (s:Structure)-[:DEPENDS]-(:ProfileGroup)-[:IN]-(u:User {id:{userId}}) " +
                "where has(s.exports) AND s.exports <> [] unwind s.exports as exp with exp where exp starts with 'GAR-' " +
                "return distinct replace(exp, 'GAR-', '') as entId limit 1 ";

        Neo4j.getInstance().execute(query, new JsonObject().put("userId", userId),
                Neo4jResult.validUniqueResultHandler(evt -> {
                    if (evt.isLeft()) {
                        handler.handle(new Either.Left(evt.left().getValue()));
                        return;
                    }

                    JsonArray jsonArrayResult = new JsonArray();
                    JsonObject entIdJO = evt.right().getValue();
                    //empty result
                    if(!entIdJO.containsKey("entId")) {
                        log.error("[SSOGar@generate] Empty GAR project Id for user : " + userId);
                    }

                    jsonArrayResult
                            .add(new JsonObject().put("idEnt", entIdJO.getString("entId")));
                    jsonArrayResult
                            .add(new JsonObject().put("GARPersonIdentifiant", userId));
                    handler.handle(new Either.Right<String, JsonArray>(jsonArrayResult));
                }));
    }

    @Override
    public void execute(Assertion assertion, Handler<Either<String, Object>> handler) {
        handler.handle(new Either.Left<String, Object>("execute function ot avalable on SSO Gar Implementation"));
    }
}
