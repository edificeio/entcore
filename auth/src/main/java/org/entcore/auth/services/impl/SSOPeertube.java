package org.entcore.auth.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.opensaml.saml2.core.Assertion;

public class SSOPeertube extends AbstractSSOProvider {
    @Override
    public void generate(EventBus eb, String userId, String host, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User {id:{userId}}) RETURN u.id as id, u.login as login, u.displayName as displayName, u.email as email, u.externalId as externalId";
        Neo4j.getInstance().execute(query, new JsonObject().put("userId", userId), Neo4jResult.validUniqueResultHandler(evt -> {
            if (evt.isLeft()) {
                handler.handle(new Either.Left(evt.left().getValue()));
                return;
            }

            JsonArray result = new JsonArray();
            JsonObject user = evt.right().getValue();
            result.add(new JsonObject().put("id", user.getString("id", "id")));
            result.add(new JsonObject().put("username", user.getString("login", "")));
            result.add(new JsonObject().put("displayName", user.getString("displayName", "")));
            result.add(new JsonObject().put("email", user.getString("email", "")));
            result.add(new JsonObject().put("externalId", user.getString("externalId", "")));
            handler.handle(new Either.Right<>(result));
        }));
    }

    @Override
    public void execute(Assertion assertion, Handler<Either<String, Object>> handler) {
        handler.handle(new Either.Left<String, Object>("execute function ot available on SSO Peertube Implementation"));
    }
}
