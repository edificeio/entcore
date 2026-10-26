package org.entcore.auth.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.user.UserUtils;
import org.opensaml.saml2.core.Assertion;

public class SSOGLPI extends AbstractSSOProvider {
    private final EventStore eventStore = EventStoreFactory.getFactory().getEventStore(this.getClass().getSimpleName());
    private static final Logger log = LoggerFactory.getLogger(SSOGLPI.class);
    protected static final String EXTERNAL_ID = "externalId";
    private static final String FIRSTNAME = "firstName";
    private static final String LASTNAME = "lastName";
    private static final String MAIL = "mail";
    private static final String UAIS = "uais";
    private static final String NAME_ID = "NameId";

    @Override
    public void generate(EventBus eb, String userId, String host, String serviceProviderEntityId, JsonObject eventAttributes,
                         Handler<Either<String, JsonArray>> handler) {
        final String query = "MATCH (s:Structure)-[:DEPENDS]-(:ProfileGroup)-[:IN]-(u:User {id:{userId}}) " +
                "WITH u, collect(s.UAI) AS uais MATCH u-[:IN]->(:Group)-[:AUTHORIZED]->(:Role)-[:AUTHORIZE]->(:Action)<-[:PROVIDE]-(e:External) WHERE e.address CONTAINS({serviceProviderEntityId}) " +
                "RETURN DISTINCT u.externalId AS externalId, u.firstName AS firstName, u.lastName AS lastName, coalesce(u.emailAcademy, u.email) AS mail, uais";

        Neo4j.getInstance().execute(query, new JsonObject().put("userId", userId).put("serviceProviderEntityId", serviceProviderEntityId),
                Neo4jResult.validUniqueResultHandler(evt -> {
                    if (evt.isLeft()) {
                        handler.handle(new Either.Left(evt.left().getValue()));
                        return;
                    }

                    final JsonArray result = new JsonArray();
                    JsonObject res = evt.right().getValue();

                    result.add(new JsonObject().put(EXTERNAL_ID, res.getString(EXTERNAL_ID)));
                    // Glpi use in the first time the NameId saml claim otherwise the NameId in the subject SAML assertion
                    result.add(new JsonObject().put(NAME_ID, res.getString(EXTERNAL_ID)));
                    result.add(new JsonObject().put(LASTNAME, res.getString(LASTNAME)));
                    result.add(new JsonObject().put(FIRSTNAME, res.getString(FIRSTNAME)));
                    result.add(new JsonObject().put(MAIL, res.getString(MAIL)));
                    result.add(new JsonObject().put(UAIS, res.getJsonArray("uais", new JsonArray()).toString()));

                    UserUtils.getUserInfos(eb, userId, userInfos -> {
                        if(userInfos != null) {
                            JsonObject customAttributes = new JsonObject().put("service", host).put("connector-type", "saml")
                                    .put("saml-type", this.getClass().getSimpleName());
                            eventStore.createConnectorEvent(userInfos, customAttributes, eventAttributes);
                        }
                    });

                    handler.handle(new Either.Right<String, JsonArray>(result));
                }));
    }

    @Override
    public void execute(Assertion assertion, Handler<Either<String, Object>> handler) {
        String errMessage = "Execute function not available on SSO GLPI Implementation";
        log.error("[Auth@SSOGLPI::execute] " + errMessage);
        handler.handle(new Either.Left<>(errMessage));
    }
}
