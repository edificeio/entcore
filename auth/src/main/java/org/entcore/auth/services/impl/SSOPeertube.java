package org.entcore.auth.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.user.UserUtils;
import org.opensaml.saml2.core.Assertion;

import static org.entcore.common.aggregation.MongoConstants.TRACE_TYPE_CONNECTOR;

public class SSOPeertube extends AbstractSSOProvider {


    private final EventStore eventStore = EventStoreFactory.getFactory().getEventStore(this.getClass().getSimpleName());
    @Override
    public void generate(EventBus eb, String userId, String host, String serviceProviderEntityId, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User {id:{userId}})\n" +
                "OPTIONAL MATCH (u)-[:IN]->(:Group)-[:AUTHORIZED]->(:Role)-[:AUTHORIZE]->(:Action)<-[:PROVIDE]-(a:Application)\n" +
                "WITH u, COLLECT(a) AS applications\n" +
                "RETURN u.id AS id,\n" +
                "u.login AS login,\n" +
                "u.displayName AS displayName,\n" +
                "u.email AS email,\n" +
                "u.externalId AS externalId,\n" +
                "REDUCE(s = false, i IN applications | s OR i.name CONTAINS({serviceName})) AS hasMatchingApplication";

        Neo4j.getInstance().execute(query, new JsonObject().put("userId", userId).put("serviceName","PeertubeSAMLId"), Neo4jResult.validUniqueResultHandler(evt -> {
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
            result.add(new JsonObject().put("hasConnectorAccess", user.getBoolean("hasMatchingApplication").toString()));
            UserUtils.getUserInfos(eb, userId, userInfos -> {
                if(userInfos != null) {
                    JsonObject customAttributes = new JsonObject().put("service", host).put("connector-type", "saml")
                                    .put("saml-type", this.getClass().getSimpleName());
                    eventStore.createAndStoreEvent(TRACE_TYPE_CONNECTOR, userInfos, customAttributes);
                }

            });

            handler.handle(new Either.Right<>(result));
        }));
    }

    @Override
    public void execute(Assertion assertion, Handler<Either<String, Object>> handler) {
        handler.handle(new Either.Left<String, Object>("execute function ot available on SSO Peertube Implementation"));
    }
}
