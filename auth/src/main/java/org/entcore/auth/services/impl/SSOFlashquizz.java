package org.entcore.auth.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.user.UserUtils;
import org.opensaml.saml2.core.Assertion;

public class SSOFlashquizz extends AbstractSSOProvider {
    private final EventStore eventStore = EventStoreFactory.getFactory().getEventStore(this.getClass().getSimpleName());

    @Override
    public void generate(EventBus eb, String userId, String host, String serviceProviderEntityId, JsonObject eventAttributes, Handler<Either<String, JsonArray>> handler) {
        JsonObject request = new JsonObject()
                .put("userId", userId)
                .put("host", host)
                .put("serviceProviderEntityId", serviceProviderEntityId);
        eb.request("fr.openent.ssoflashquizz", request, reply -> {
            if (reply.succeeded()) {
                JsonArray response = (JsonArray) reply.result().body();
                UserUtils.getUserInfos(eb, userId, userInfos -> {
                    if(userInfos != null) {
                        JsonObject customAttributes = new JsonObject().put("service", host).put("connector-type", "saml")
                                .put("saml-type", this.getClass().getSimpleName());
                        eventStore.createConnectorEvent(userInfos, customAttributes, eventAttributes);
                    }
                });

                handler.handle(new Either.Right<String, JsonArray>(response));
            } else {
                handler.handle(new Either.Left<String, JsonArray>(reply.cause().getMessage()));
            }
        });
    }

    @Override
    public void execute(Assertion assertion, Handler<Either<String, Object>> handler) {
        handler.handle(new Either.Left<>("[SSOFlashquizz::execute] execute function not available on SSO Flashquizz Implementation"));
    }
}
