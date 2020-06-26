package org.entcore.auth.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.opensaml.saml2.core.Assertion;

public class SSOMatrix extends AbstractSSOProvider {
    @Override
    public void generate(EventBus eb, String userId, String host, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User {id:{userId}}) RETURN u.id as id, u.login as login, u.displayName as displayName";
        Neo4j.getInstance().execute(query, new JsonObject().put("userId", userId), Neo4jResult.validUniqueResultHandler(evt -> {
            if (evt.isLeft()) {
                handler.handle(new Either.Left(evt.left().getValue()));
                return;
            }

            JsonArray result = new JsonArray();
            JsonObject user = evt.right().getValue();
            result.add(new JsonObject().put("uid", user.getString("id", "id")));
            result.add(new JsonObject().put("mxid", mxid(user.getString("id", ""), user.getString("login", ""))));
            result.add(new JsonObject().put("displayName", user.getString("displayName", "")));
            handler.handle(new Either.Right<>(result));
        }));
    }

    private String mxid(String id, String login) {
        return String.format("%s-%s", login, id.substring(0, 8));
    }

    @Override
    public void execute(Assertion assertion, Handler<Either<String, Object>> handler) {
        handler.handle(new Either.Left<String, Object>("execute function ot available on SSO Matrix Implementation"));
    }
}
