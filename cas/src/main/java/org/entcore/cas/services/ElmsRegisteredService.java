package org.entcore.cas.services;

import fr.wseduc.cas.async.Handler;
import fr.wseduc.cas.entities.AuthCas;
import fr.wseduc.cas.entities.User;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class ElmsRegisteredService extends AbstractCas20ExtensionRegisteredService {

    private static final Logger log = LoggerFactory.getLogger(ElmsRegisteredService.class);

    private static final String ID = "id";
    private static final String FIRSTNAME = "firstName";
    private static final String LASTNAME = "lastName";
    private static final String STRUCTURE = "structure";
    private static final String STRUCTURES = "structures";
    private static final String TYPE = "type";
    private static final String PROFIL = "profil";
    private static final String UAI = "UAI";
    private static final String IN_GAR_GROUP = "inGarGroup";
    private static final String IN_GAR_GROUPS = "inGarGroups";
    private static final String IN_VALIDATEUR_GROUPS = "inValidateurGroups";
    private static final String GROUPS = "groups";
    private static final String VALIDEUR_GROUP = "Valideur de commande";
    private static final String GROUPDISPLAYNAME = "groupDisplayName";
    private static final String ACTION = "action";
    private static final String MESSAGE = "message";
    private static final String OK = "ok";
    private static final String RESULT = "result";
    private static final String STATUS = "status";
    private static final String STRUCTUREIDS = "structureIds";
    private static final String USERID = "userId";

    private Future<JsonObject> getUserInfos(String userId) {
        JsonObject jo = new JsonObject();
        jo.put(ACTION, "getUserInfos").put(USERID, userId);
        Promise<JsonObject> promise = Promise.promise();
        eb.request("directory", jo, handlerToAsyncHandler(event -> {
            JsonObject res = event.body().getJsonObject(RESULT);
            if (OK.equals(event.body().getString(STATUS)) && res != null) {
                promise.complete(res);
            } else {
                promise.fail("user.not.found");
            }
        }));
        return promise.future();
    }
    @Override
    public void getUser(final AuthCas authCas, final String service, final Handler<User> userHandler) {
        final String userId = authCas.getUser();

        Future<JsonObject> getUserFuture = getUserInfos(userId);
        Future<JsonObject> isInGroupFuture = isInGroup(userId, new ArrayList<>(authCas.getStructureIds()));

        CompositeFuture.all(getUserFuture, isInGroupFuture)
                .onSuccess((res) -> {
                    JsonObject data = getUserFuture.result();
                    JsonObject inGroups = isInGroupFuture.result();

                    User user = new User();
                    JsonObject userData = new JsonObject()
                            .put(ID, data.getString(ID))
                            .put(FIRSTNAME, data.getString(FIRSTNAME))
                            .put(LASTNAME, data.getString(LASTNAME))
                            .put(TYPE, data.getString(TYPE))
                            .put(STRUCTURES, data.getJsonArray(STRUCTURES))
                            .put(IN_GAR_GROUPS, inGroups)
                            .put(GROUPS, data.getJsonArray(GROUPS));
                    prepareUser(user, userId, service, userData);
                    userHandler.handle(user);
                }).onFailure((err) -> {
                    log.error(String.format("[entcoreCAS@%s::getUser] " +
                            "Failed to get User for eLMS. %s", this.getClass().getName(), err.getMessage()));
                    userHandler.handle(null);
                });
    }

    @Override
    protected void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionalAttributes) {
        user.setUser(data.getString(principalAttributeName));

        additionalAttributes.add(createTextElement(ID, data.getString(ID), doc));
        additionalAttributes.add(createTextElement(FIRSTNAME, data.getString(FIRSTNAME), doc));
        additionalAttributes.add(createTextElement(LASTNAME, data.getString(LASTNAME), doc));

        switch(data.getString(TYPE)) {
            case "Student" :
                additionalAttributes.add(createTextElement(PROFIL, "National_1", doc));
                break;
            case "Relative" :
                additionalAttributes.add(createTextElement(PROFIL, "National_2", doc));
                break;
            case "Teacher" :
                additionalAttributes.add(createTextElement(PROFIL, "National_3", doc));
                break;
            case "Personnel" :
                additionalAttributes.add(createTextElement(PROFIL, "National_4", doc));
                break;
        }

        final JsonObject inGroups = data.getJsonObject(IN_GAR_GROUPS);

        if (data.containsKey(STRUCTURES)) {
            for (Object o : data.getJsonArray(STRUCTURES)) {
                JsonObject structNode = (JsonObject) o;
                if (structNode != null) {
                    Element rootElement = createElement(STRUCTURE, doc);
                    rootElement.appendChild(createTextElement(UAI, structNode.getString(UAI), doc));
                    boolean inGroup = inGroups.getBoolean(structNode.getString(ID))
                            != null && inGroups.getBoolean(structNode.getString(ID));
                    rootElement.appendChild(createTextElement(IN_GAR_GROUP, String.valueOf(inGroup), doc));

                    if (data.getJsonArray(GROUPS) != null) {
                        boolean isInValidateurGroup = data.getJsonArray(GROUPS).stream()
                                .filter(JsonObject.class::isInstance)
                                .map(JsonObject.class::cast)
                                .anyMatch(group -> VALIDEUR_GROUP.equals(group.getString(GROUPDISPLAYNAME)));
                        rootElement.appendChild(createTextElement(IN_VALIDATEUR_GROUPS, String.valueOf(isInValidateurGroup), doc));
                    }

                    additionalAttributes.add(rootElement);
                }
            }
        }

    }

    private Future<JsonObject> isInGroup(String userId, ArrayList<String> structureIds) {
        Promise<JsonObject> promise = Promise.promise();

        final String GAR_ADDRESS = "openent.mediacentre";
        final JsonObject action = new JsonObject()
                .put(ACTION, "isInGarGroup")
                .put(STRUCTUREIDS, new JsonArray(structureIds))
                .put(USERID, userId);

        eb.send(GAR_ADDRESS, action, handlerToAsyncHandler(event -> {

            if (OK.equals(event.body().getString(STATUS))) {
                promise.complete(event.body().getJsonObject(MESSAGE));
                return;
            }

            log.error(String.format("[entcoreCAS@%s::isInGroup] " +
                    "Failed to retrieve gar resources. %s", this.getClass().getName(), event.body().getString(MESSAGE, "")));;
            promise.complete(new JsonObject());
        }));

        return promise.future();
    }
}
