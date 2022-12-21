package org.entcore.cas.services;

import fr.wseduc.cas.async.Handler;
import fr.wseduc.cas.entities.AuthCas;
import fr.wseduc.cas.entities.User;
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
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class ElmsRegisteredService extends AbstractCas20ExtensionRegisteredService {

    private static final Logger log = LoggerFactory.getLogger(ElmsRegisteredService.class);

    private static final String ID = "id";
    private static final String FIRSTNAME = "firstName";
    private static final String LASTNAME = "lastName";
    private static final String STRUCTURE = "structure";
    private static final String STRUCTURES = "structures";
    private static final String UFUNCTIONS = "ufunctions";
    private static final String FUNCTION = "function";
    private static final String UAI = "UAI";
    private static final String EXTERNAL_ID = "externalId";
    private static final String IN_GAR_GROUP = "inGarGroup";
    private static final String IN_GAR_GROUPS = "inGarGroups";

    @Override
    public void getUser(final AuthCas authCas, final String service, final Handler<User> userHandler) {
        final String userId = authCas.getUser();

        JsonObject jo = new JsonObject();
        jo.put("action", "getUserInfos").put("userId", userId);
        eb.send("directory", jo, handlerToAsyncHandler(event -> {
            JsonObject res = event.body().getJsonObject("result");

            if ("ok".equals(event.body().getString("status")) && res != null) {
                ArrayList<String> structureIds = new ArrayList<>(authCas.getStructureIds());
                isInGroup(userId, structureIds)
                        .onSuccess((inGroup) -> {
                            User user = new User();
                            JsonObject data = new JsonObject()
                                    .put(ID, res.getString(ID))
                                    .put(FIRSTNAME, res.getString(FIRSTNAME))
                                    .put(LASTNAME, res.getString(LASTNAME))
                                    .put(UFUNCTIONS, res.getJsonArray(UFUNCTIONS))
                                    .put(STRUCTURES, res.getJsonArray(STRUCTURES))
                                    .put(IN_GAR_GROUPS, inGroup);
                            prepareUser(user, userId, service, data);
                            userHandler.handle(user);
                        })
                        .onFailure(err -> userHandler.handle(null));
            } else {
                userHandler.handle(null);
            }
        }));
    }

    @Override
    protected void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionalAttributes) {
        user.setUser(data.getString(principalAttributeName));

        additionalAttributes.add(createTextElement(ID, data.getString(ID), doc));
        additionalAttributes.add(createTextElement(FIRSTNAME, data.getString(FIRSTNAME), doc));
        additionalAttributes.add(createTextElement(LASTNAME, data.getString(LASTNAME), doc));

        final JsonObject inGroups = data.getJsonObject(IN_GAR_GROUPS);

        if (data.containsKey(STRUCTURES)) {
            for (Object o : data.getJsonArray(STRUCTURES)) {
                JsonObject structNode = (JsonObject) o;
                if (structNode != null) {
                    Element rootElement = createElement(STRUCTURE, doc);
                    rootElement.appendChild(createTextElement(UAI, structNode.getString(UAI), doc));
                    boolean inGroup = inGroups.getBoolean(structNode.getString(ID)) != null ? inGroups.getBoolean(structNode.getString(ID)) : false;
                    rootElement.appendChild(createTextElement(IN_GAR_GROUP, String.valueOf(inGroup), doc));

                    if (data.getJsonArray(UFUNCTIONS) != null) {
                        String externalId = structNode.getString(EXTERNAL_ID);
                        List<String> ufunctions = extractFunctions(data.getJsonArray(UFUNCTIONS), externalId);

                        ufunctions.forEach(f -> rootElement.appendChild(createTextElement(FUNCTION, f, doc)));
                    }

                    additionalAttributes.add(rootElement);
                }
            }
        }

    }

    /**
     * Extract functions from ufunctions JsonArray
     * @param ufunctions
     * @param externalId
     * @return list of function
     */
    private List<String> extractFunctions(JsonArray ufunctions, String externalId) {
        Predicate<String> byExternalId = ufunction -> {
            String[] split = ufunction.split("\\$");
            if (split.length >= 1) {
                return Objects.equals(split[0], externalId);
            }
            return false;
        };
        Function<String, String> mapFunctionName = ufunction -> {
            String[] split = ufunction.split("\\$");
            String functionName = "";
            if (split.length >= 3) {
                functionName += split[2];
            }
            if (split.length >= 5) {
                functionName += " " + split[4];
            }
            return functionName;
        };
        return ((List<String>) ufunctions
                .getList())
                .stream()
                .filter(byExternalId)
                .map(mapFunctionName)
                .collect(Collectors.toList());
    }

    private Future<JsonObject> isInGroup(String userId, ArrayList<String> structureIds) {
        Promise<JsonObject> promise = Promise.promise();

        final String GAR_ADDRESS = "openent.mediacentre";
        final JsonObject action = new JsonObject()
                .put("action", "isInGarGroup")
                .put("structureIds", new JsonArray(structureIds))
                .put("userId", userId);

        eb.send(GAR_ADDRESS, action, handlerToAsyncHandler(event -> {

            if ("ok".equals(event.body().getString("status"))) {
                promise.complete(event.body().getJsonObject("message"));
                return;
            }

            log.error("Failed to retrieve gar resources", event.body().getString("message"));
            promise.fail("Failed to retrieve gar resources");
        }));

        return promise.future();
    }
}
