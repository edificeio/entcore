package org.entcore.auth.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.opensaml.saml2.core.Assertion;

public class SSOJira extends AbstractSSOProvider {
    @Override
    public void generate(EventBus eb, String userId, String host, String serviceProviderEntityId, Handler<Either<String, JsonArray>> handler) {
        JsonObject request = new JsonObject()
                .put("userId", userId)
                .put("host", host)
                .put("serviceProviderEntityId", serviceProviderEntityId);
        eb.request("fr.openent.ssojira", request, reply -> {
            if (reply.succeeded()) {
                JsonArray response = (JsonArray) reply.result().body();
                handler.handle(new Either.Right<String, JsonArray>(response));
            } else {
                handler.handle(new Either.Left<String, JsonArray>(reply.cause().getMessage()));
            }
        });
    }
    @Override
    public void execute(Assertion assertion, Handler<Either<String, Object>> handler) {
        handler.handle(new Either.Left<>("execute function ot available on SSO Jira Implementation"));
    }
}
