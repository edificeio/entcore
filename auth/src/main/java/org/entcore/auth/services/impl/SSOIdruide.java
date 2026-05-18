package org.entcore.auth.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
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

public class SSOIdruide extends AbstractSSOProvider {


    private final EventStore eventStore = EventStoreFactory.getFactory().getEventStore(this.getClass().getSimpleName());
    @Override
    public void generate(EventBus eb, String userId, String host, String serviceProviderEntityId, Handler<Either<String, JsonArray>> handler) {
        final String emailDomain = Vertx.currentContext().config().getJsonObject("idruide-email-domain-by-host", new JsonObject()).getString(host);
        String query = "MATCH (u:User {id:{userId}}) RETURN u.displayName AS displayName, u.lastName AS lastName, u.firstName AS firstName, u.emailAcademy AS mail, head(u.profiles) as profile";

        Neo4j.getInstance().execute(query, new JsonObject().put("userId", userId), Neo4jResult.validUniqueResultHandler(evt -> {
            if (evt.isLeft()) {
                handler.handle(new Either.Left(evt.left().getValue()));
                return;
            }

            JsonArray result = new JsonArray();
            JsonObject user = evt.right().getValue();
            final String profile = user.getString("profile", "");
            result.add(new JsonObject().put("id", userId));
            result.add(new JsonObject().put("preferred_username", userId));
            result.add(new JsonObject().put("displayName", user.getString("displayName", "")));
            result.add(new JsonObject().put("lastName", user.getString("lastName", "")));
            result.add(new JsonObject().put("firstName", user.getString("firstName", "")));
            result.add(new JsonObject().put("email", ("Student".equals(profile)) ? userId + "@" + emailDomain : user.getString("mail","")));
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
        handler.handle(new Either.Left<String, Object>("execute function ot available on SSO Idruide Implementation"));
    }
}
