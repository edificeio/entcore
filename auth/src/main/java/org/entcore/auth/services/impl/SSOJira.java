package org.entcore.auth.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.user.UserUtils;
import org.opensaml.saml2.core.Assertion;

public class SSOJira extends AbstractSSOProvider {
    @Override
    public void generate(EventBus eb, String userId, String host, String serviceProviderEntityId, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User {id:{userId}})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
                "RETURN u.login as login, u.displayName as displayName, COALESCE(u.emailInternal, u.email) as email, " +
                "collect(DISTINCT s.UAI + ' - ' + s.name) AS structures, collect(DISTINCT 'Académie de ' + s.academy) AS academies";
        Neo4j.getInstance().execute(query, new JsonObject().put("userId", userId), Neo4jResult.validUniqueResultHandler(evt -> {
            if (evt.isLeft()) {
                handler.handle(new Either.Left<>(evt.left().getValue()));
                return;
            }

            JsonArray result = new JsonArray();
            JsonObject user = evt.right().getValue();
            result.add(new JsonObject().put("login", user.getString("login", "")));
            result.add(new JsonObject().put("displayName", user.getString("displayName", "")));
            result.add(new JsonObject().put("email", user.getString("email", "")));

            //Add groups for Jira
            result.add(new JsonObject().put("group", "jira-software-users-ent"));
            addingGroups(user, "structures", result);
            addingGroups(user, "academies", result);

            UserUtils.getUserInfos(eb, userId, userInfo -> {
                String userType = userInfo.getType();
                if (userType != null && (userInfo.isADML())) {
                    result.add(new JsonObject().put("group", "jira-administrateur-ent"));
                } else if (userType != null && (userType.equals("Teacher") || userType.equals("Personnel"))) {
                    result.add(new JsonObject().put("group", "jira-personnel-ent"));
                }
                handler.handle(new Either.Right<>(result));
            });
        }));
    }

    private static void addingGroups(JsonObject user, String key, JsonArray result) {
        user.getJsonArray(key, new JsonArray()).stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .forEach(value -> result.add(new JsonObject().put("group", value)));
    }

    @Override
    public void execute(Assertion assertion, Handler<Either<String, Object>> handler) {
        handler.handle(new Either.Left<>("execute function ot available on SSO Jira Implementation"));
    }
}
