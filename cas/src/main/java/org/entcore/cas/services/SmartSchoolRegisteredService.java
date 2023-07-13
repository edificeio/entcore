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
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class SmartSchoolRegisteredService extends AbstractCas20ExtensionRegisteredService{

    public enum RIGHTS {
        ADMIN,
        CHECKER,
        EDITOR,
        READER,
        ADMINDF,
        USERDF,
        PARTNRINDUS
    }

    private static final Logger log = LoggerFactory.getLogger(SmartSchoolRegisteredService.class);

    private static final String EA_ATTRIBUTES = "attributes";

    private static final String FIRSTNAME = "firstName";
    private static final String LASTNAME = "lastName";
    private static final String EMAIL = "email";
    private static final String STRUCTURE = "structure";
    private static final String RIGHT = "right";
    private static final String ACTIVE_STRUCTURE = "active_structure";
    private static final String DIGITAL_FACTORY_STRUCTURE_NAME = "Digital Factory";

    private static final List<String> CHECKER_FUNCTIONS = Arrays.asList("gestionnaireressources", "gestionnaireformation", "offcomecole");
    private static final List<String> EDITOR_FUNCTIONS = Arrays.asList("enseignantinstructeur", "experttice", "encadrementapprenant");

    @Override
    protected void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionnalAttributes) {
        try {
            JsonArray userFunctions = data.getJsonArray("functions", new JsonArray());
            JsonArray structureNodes = data.getJsonArray("structureNodes", new JsonArray());
            JsonArray userProfiles = data.getJsonArray("profiles", new JsonArray());

            user.setUser(data.getString("id"));
            Element rootElement = createElement(EA_ATTRIBUTES, doc);
            rootElement.appendChild(createTextElement(FIRSTNAME, data.getString(FIRSTNAME, ""), doc));
            rootElement.appendChild(createTextElement(LASTNAME, data.getString(LASTNAME, ""), doc));
            rootElement.appendChild(createTextElement(EMAIL, data.getString(EMAIL, ""), doc));
            rootElement.appendChild(createTextElement(RIGHT, getRight(userFunctions, structureNodes, userProfiles), doc));
            rootElement.appendChild(createTextElement(ACTIVE_STRUCTURE, data.getJsonArray("structures", new JsonArray()).getString(0), doc));
            addStructures(structureNodes, doc, rootElement);
            additionnalAttributes.add(rootElement);
        } catch (Exception e) {
            log.error("Failed to transform user for SmartSchool CAS response", e);
        }
    }

    private void addStructures(JsonArray structures, Document doc, Element rootElement) {
        for (int i = 0; i < structures.size(); i++) {
            JsonObject structure = structures.getJsonObject(i);
            rootElement.appendChild(createTextElement(STRUCTURE, structure.getString("id"), doc));
        }
    }

    private String getRight(JsonArray functions, JsonArray structureNodes, JsonArray userProfiles) {
        if (functions.contains("SuperAdmin")) {
            return RIGHTS.ADMIN.toString();
        }

        Optional<JsonObject> digitaleFactoryStructure = structureNodes.stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .filter(structureNode -> DIGITAL_FACTORY_STRUCTURE_NAME.equals(structureNode.getString("name")))
                .findFirst();

        if (digitaleFactoryStructure.isPresent()) {
            if (functions.contains("AdminLocal")) {
                return RIGHTS.ADMINDF.toString();
            }
            if (userProfiles.contains("Guest")) {
                if (functions.contains("partenaireministere")) {
                    return RIGHTS.USERDF.toString();
                } else {
                    return RIGHTS.PARTNRINDUS.toString();
                }
            }
        }

        List<String> userFunctions = functions.getList();
        if (containsFunction(userFunctions, CHECKER_FUNCTIONS)) {
            return RIGHTS.CHECKER.toString();
        }

        if (containsFunction(userFunctions, EDITOR_FUNCTIONS)) {
            return RIGHTS.EDITOR.toString();
        }

        return RIGHTS.READER.toString();
    }

    private boolean containsFunction(List<String> functions, List<String> rightFunctions) {
        for (String function : functions) {
            if (rightFunctions.contains(function.trim().replaceAll("\\s+","").toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void getUser(AuthCas authCas, String service, Handler<User> userHandler) {
        Future<JsonObject> userFuture = getDirectoryUser(authCas);
        Future<JsonArray> functionsFuture =  getUserFunctions(authCas);
        List<Future> futures = Arrays.asList(userFuture, functionsFuture);
        CompositeFuture.all(futures)
                .onFailure(throwable -> {
                    log.error(String.format("Failed to retrieve user %s", authCas.getUser(), throwable));
                    userHandler.handle(null);
                })
                .onSuccess(unused -> {
                    JsonObject userData = userFuture.result();
                    JsonArray userFunctions = functionsFuture.result();
                    userData.put("functions", userFunctions);
                    User user = new User();
                    prepareUser(user, authCas.getUser(), service, userData);
                    userHandler.handle(user);
                });
    }

    private Future<JsonArray> getUserFunctions(AuthCas authCas) {
        Promise<JsonArray> promise = Promise.promise();
        String query = "MATCH(u:User) WHERE u.id = {id} " +
                "OPTIONAL MATCH (u)-[:IN]->(f:FunctionGroup) " +
                "OPTIONAL MATCH (u)-[:HAS_FUNCTION]->(func:Function) " +
                "RETURN COLLECT(func.name) + COLLECT(f.filter) as functions;";
        JsonObject params = new JsonObject()
                .put("id", authCas.getUser());

        Neo4j.getInstance().execute(query, params, Neo4jResult.validUniqueResultHandler(either -> {
            if (either.isLeft()) promise.fail(either.left().getValue());
            else promise.complete(either.right().getValue().getJsonArray("functions", new JsonArray()));
        }));

        return promise.future();
    }

    private Future<JsonObject> getDirectoryUser(AuthCas authCas) {
        Promise<JsonObject> promise = Promise.promise();
        JsonObject action = new JsonObject()
                .put("action", directoryAction)
                .put("userId", authCas.getUser());

        eb.send("directory", action, handlerToAsyncHandler(event -> {
            JsonObject res = event.body().getJsonObject("result");
            if ("ok".equals(event.body().getString("status")) && res != null) {
                promise.complete(res);
            } else {
                promise.fail("User not found");
            }
        }));

        return promise.future();
    }
}
