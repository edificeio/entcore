package org.entcore.auth.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import org.entcore.common.events.EventHelper;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.opensaml.saml2.core.Assertion;




public class SSOMatrix extends AbstractSSOProvider {

    private final EventStore eventStore = EventStoreFactory.getFactory().getEventStore(this.getClass().getSimpleName());
    @Override
    public void generate(EventBus eb, String userId, String host, String serviceProviderEntityId, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User {id:{userId}})" +
                //Check if the user can access to the App
                "-[:IN]->(:Group)-[:AUTHORIZED]->(:Role)-[:AUTHORIZE]->(:Action)<-[:PROVIDE]-(a:Application) " +
                "WHERE a.address CONTAINS({serviceProviderEntityId}) " +
                "RETURN DISTINCT u.id as id, u.login as login, u.displayName as displayName, u.groups as groups, u.classes as classes";
        //groups and classes are used for stats do not add them to the result
        Neo4j.getInstance().execute(query, new JsonObject().put("userId", userId).put("serviceProviderEntityId",serviceProviderEntityId + "/"),
                Neo4jResult.validUniqueResultHandler(evt -> {
            if (evt.isLeft()) {
                handler.handle(new Either.Left(evt.left().getValue()));
                return;
            }

            JsonArray result = new JsonArray();
            JsonObject user = evt.right().getValue();
            result.add(new JsonObject().put("uid", user.getString("id", "id")));
            result.add(new JsonObject().put("mxid", mxid(user.getString("id", ""), user.getString("login", ""))));
            result.add(new JsonObject().put("displayName", user.getString("displayName", "")));
            UserUtils.getUserInfos(eb, userId, userInfos -> {
                if (userInfos != null) {
                    createStatsEvent(userInfos, host);
                }
            });
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

    private void createStatsEvent(UserInfos user, String host) {
        JsonObject event = new JsonObject().put("service", host).put("connector-type", "saml").put("saml-type", this.getClass().getSimpleName());
        eventStore.createAndStoreEvent(EventHelper.ACCESS_EVENT, user, event);
    }
}
