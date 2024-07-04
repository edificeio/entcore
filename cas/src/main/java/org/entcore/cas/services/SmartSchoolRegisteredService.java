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

import java.util.ArrayList;
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
            List<String> rights = getRights(userFunctions, structureNodes, userProfiles);
            for (String right : rights) {
                rootElement.appendChild(createTextElement(RIGHT, right, doc));
            }
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

    private List<String> getRights(JsonArray functions, JsonArray structureNodes, JsonArray userProfiles) {
        List<String> rights = new ArrayList<>();
        List<String> dfFunctions = new ArrayList<>();
        List<String> otherFunctions = new ArrayList<>();
        filterFunctionsByHatStructure(functions,dfFunctions, otherFunctions);

        if (dfFunctions.contains("SuperAdmin") || otherFunctions.contains("SuperAdmin")) {
            rights.add(RIGHTS.ADMIN.toString());
            return rights;
        }

        Optional<JsonObject> digitaleFactoryStructure = structureNodes.stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .filter(structureNode -> DIGITAL_FACTORY_STRUCTURE_NAME.equals(structureNode.getString("name")))
                .findFirst();

        if (digitaleFactoryStructure.isPresent()) {
            if (dfFunctions.contains("AdminLocal")) {
               rights.add(RIGHTS.ADMINDF.toString());
            }
            if (userProfiles.contains("Guest")) {
                if (dfFunctions.contains("partenaireministere")) {
                    rights.add(RIGHTS.USERDF.toString());
                } else {
                    rights.add(RIGHTS.PARTNRINDUS.toString());
                }
            }
            if((userProfiles.contains("Personnel") || userProfiles.contains("Teacher"))
                    && structureNodes.size() > 1 && !rights.contains(RIGHTS.ADMINDF.toString())) { // in Campus's structure and DF
                rights.add(RIGHTS.USERDF.toString());
            }
        }

        if (containsFunction(otherFunctions, CHECKER_FUNCTIONS)) {
            rights.add(RIGHTS.CHECKER.toString());
        }

        if (containsFunction(otherFunctions, EDITOR_FUNCTIONS)) {
            rights.add(RIGHTS.EDITOR.toString());
        }

        if (!rights.contains(RIGHTS.EDITOR.toString()) && !rights.contains(RIGHTS.CHECKER.toString())) {
            if (!digitaleFactoryStructure.isPresent() || (structureNodes.size() > 1)) {
                rights.add(RIGHTS.READER.toString());
            }
        }

        return rights;
    }

    private void filterFunctionsByHatStructure(JsonArray allFunctionsByStructure, List<String> dfFunctions, List<String> otherFunctions) {
        for(int i = 0; i < allFunctionsByStructure.size(); i++){
            JsonObject userFunctionByStructure = allFunctionsByStructure.getJsonObject(i);
            if(userFunctionByStructure.containsKey("functions") && !userFunctionByStructure.getJsonArray("functions").isEmpty()) {
                List<String> allFunctions = userFunctionByStructure.getJsonArray("functions").getList();
                for (String function : allFunctions) {
                    if (userFunctionByStructure.containsKey("structureName") &&
                            (DIGITAL_FACTORY_STRUCTURE_NAME.equals(userFunctionByStructure.getString("structureName"))
                                    || userFunctionByStructure.getString("structureName") == null) ) {
                        if (!dfFunctions.contains(function)) dfFunctions.add(function);
                    } else {
                        if (!otherFunctions.contains(function)) otherFunctions.add(function);
                    }
                }
            }
        }
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
                    if (userFuture.failed()) {
                        log.error(String.format("Failed to retrieve directory user %s: %s", authCas.getUser(), userFuture.cause().getMessage()), userFuture.cause());
                    }
                    if (functionsFuture.failed()) {
                        log.error(String.format("Failed to retrieve user functions for %s: %s", authCas.getUser(), functionsFuture.cause().getMessage()), functionsFuture.cause());
                    }
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
                "OPTIONAL MATCH (u)-[:IN]->(f:FunctionGroup)-[:DEPENDS]->(s: Structure) " +
                "OPTIONAL MATCH (u)-[:HAS_FUNCTION]->(func:Function) " +
                "RETURN COLLECT(Distinct(func.name)) + COLLECT(Distinct(f.filter)) as functions, s.name as structureName;";
        JsonObject params = new JsonObject()
                .put("id", authCas.getUser());
        log.info("Requête neo: " + query + " params: " + params);

        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(either -> {
            if (either.isLeft()) promise.fail(either.left().getValue());
            else {
                promise.complete(either.right().getValue());
            }
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
