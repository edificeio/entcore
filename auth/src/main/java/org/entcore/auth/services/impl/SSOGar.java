package org.entcore.auth.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import org.opensaml.saml2.core.Assertion;

import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;


public class SSOGar extends AbstractSSOProvider {

    @Override
    public void generate(EventBus eb, String userId, Handler<Either<String, JsonArray>> handler) {

        JsonObject sendTOMediacentre = new JsonObject().put("action", "getConfig");


        eb.send("openent.mediacentre", sendTOMediacentre, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                if (("error").equals(message.body().getString("status")) ) {
                    handler.handle(new Either.Left<>(message.body().toString()));
                } else {
                    JsonObject configMediacentre = message.body().getJsonObject("message");
                    JsonArray jsonArrayResult = new JsonArray();
                    jsonArrayResult
                            .add(new JsonObject().put("idEnt", configMediacentre.getString("id-ent")))
                            .add(new JsonObject().put("GARPersonIdentifiant", userId));
                    handler.handle(new Either.Right<String, JsonArray>(jsonArrayResult));
                }
            }
        }));
    }

    @Override
    public void execute(Assertion assertion, Handler<Either<String, Object>> handler) {
        handler.handle(new Either.Left<String, Object>("execute function ot avalable on SSO Gar Implementation"));
    }


}
