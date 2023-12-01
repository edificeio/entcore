package org.entcore.auth.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.opensaml.saml2.core.Assertion;

import java.util.ArrayList;
import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class SSOGLPI extends AbstractSSOProvider {
    private static final Logger log = LoggerFactory.getLogger(SSOGLPI.class);
    private static final String ACTION = "action";
    private static final String USERID = "userId";
    private static final String DIRECTORY = "directory";
    private static final String RESULT = "result";
    private static final String STATUS = "status";
    private static final String MESSAGE = "message";
    private static final String OK = "ok";
    protected static final String EXTERNAL_ID = "externalId";
    private static final String FIRSTNAME = "firstName";
    private static final String LASTNAME = "lastName";
    private static final String ACADEMIC_MAIL = "emailAcademy";
    private static final String UAIS = "uais";
    protected static final String STRUCTURES = "structures";
    protected static final String UAI = "UAI";
    protected static final String STRUCTURE_NODES = "structureNodes";

    @Override
    public void generate(EventBus eb, String userId, String host, String serviceProviderEntityId, Handler<Either<String, JsonArray>> handler) {
        getUser(eb, userId)
            .compose(this::fillResult)
            .onSuccess(result -> handler.handle(new Either.Right<>(result)))
            .onFailure(err -> {
                log.error("[Auth@SSOGLPI::generate] Failed to generate response for GLPI : " + err.getMessage());
                handler.handle(new Either.Left(err.getMessage()));
            });
    }

    public Future<JsonObject> getUser(EventBus eb, String userId) {
        Promise<JsonObject> promise = Promise.promise();

        JsonObject jo = new JsonObject();
        jo.put(ACTION, "getUser").put(USERID, userId);
        eb.request(DIRECTORY, jo, handlerToAsyncHandler(message -> {
            JsonObject messageResult = message.body().getJsonObject(RESULT, null);
            if (OK.equals(message.body().getString(STATUS)) && messageResult != null) {
                promise.complete(messageResult);
            }
            else {
                String messageError = message.body().getString(MESSAGE, null);
                log.error("[Auth@SSOGLPI::getUser] Failed to get user infos for user with id " + userId + " : " + messageError);
                promise.fail(messageError);
            }
        }));

        return promise.future();
    }

    private Future<JsonArray> fillResult(JsonObject userInfos) {
        Promise<JsonArray> promise = Promise.promise();

        try {
            JsonArray result = new JsonArray();

            // ExternalId
            if (userInfos.containsKey(EXTERNAL_ID)) {
                result.add(new JsonObject().put(EXTERNAL_ID, userInfos.getString(EXTERNAL_ID, EXTERNAL_ID)));
            }

            // Lastname
            if (userInfos.containsKey(LASTNAME)) {
                result.add(new JsonObject().put(LASTNAME, userInfos.getString(LASTNAME, LASTNAME)));
            }

            // Firstname
            if (userInfos.containsKey(FIRSTNAME)) {
                result.add(new JsonObject().put(FIRSTNAME, userInfos.getString(FIRSTNAME, FIRSTNAME)));
            }

            // AcademicMail
            if (userInfos.containsKey(ACADEMIC_MAIL)) {
                result.add(new JsonObject().put(ACADEMIC_MAIL, userInfos.getString(ACADEMIC_MAIL, ACADEMIC_MAIL)));
            }

            // UAIs
            if (userInfos.containsKey(STRUCTURES)) {
                List<String> structures = new ArrayList<>();
                for (Object o : userInfos.getJsonArray(STRUCTURES, new JsonArray())) {
                    if (o instanceof JsonObject) {
                        JsonObject structure = (JsonObject) o;
                        if (structure.containsKey(UAI)) {
                            structures.add(structure.getString(UAI));
                        }
                    }
                    else if (o instanceof String) {
                        String uai = userInfos.getJsonArray(STRUCTURE_NODES).stream()
                                .map(JsonObject.class::cast)
                                .filter(structNode -> structNode.getString(EXTERNAL_ID).equals(o))
                                .findFirst()
                                .orElse(null)
                                .getString(UAI, null);
                        structures.add(uai);
                    }
                }
                result.add(new JsonObject().put(UAIS, structures.size() == 1 ? structures.get(0) : structures.toString()));
            }

            promise.complete(result);
        }
        catch (Exception e) {
            log.error("[Auth@SSOGLPI::fillResult] Failed to fill result with user infos  : " + e.getMessage());
            promise.fail(e.getMessage());
        }

        return promise.future();
    }

    @Override
    public void execute(Assertion assertion, Handler<Either<String, Object>> handler) {
        String errMessage = "Execute function not available on SSO GLPI Implementation";
        log.error("[Auth@SSOGLPI::execute] " + errMessage);
        handler.handle(new Either.Left<>(errMessage));
    }
}
