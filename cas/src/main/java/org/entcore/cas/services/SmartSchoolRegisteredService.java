package org.entcore.cas.services;

import fr.wseduc.cas.entities.User;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

public class SmartSchoolRegisteredService extends AbstractCas20ExtensionRegisteredService{

    public enum RIGHTS {
        ADMIN,
        CHECKER,
        EDITOR,
        READER
    }

    private static final Logger log = LoggerFactory.getLogger(SmartSchoolRegisteredService.class);

    private static final String EA_ATTRIBUTES = "attributes";

    private static final String FIRSTNAME = "firstName";
    private static final String LASTNAME = "lastName";
    private static final String EMAIL = "email";
    private static final String STRUCTURE = "structure";
    private static final String FUNCTION = "function";
    private static final String RIGHT = "right";
    private static final String ACTIVE_STRUCTURE = "active_structure";

    @Override
    protected void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionnalAttributes) {
        try {
//            JsonArray userFunctions = data.getJsonArray("functions", new JsonArray()).getJsonArray(0);

            user.setUser(data.getString("id"));
            Element rootElement = createElement(EA_ATTRIBUTES, doc);
            rootElement.appendChild(createTextElement(FIRSTNAME, data.getString(FIRSTNAME, ""), doc));
            rootElement.appendChild(createTextElement(LASTNAME, data.getString(LASTNAME, ""), doc));
            rootElement.appendChild(createTextElement(EMAIL, data.getString(EMAIL, ""), doc));
            rootElement.appendChild(createTextElement(RIGHT, getRight(data.getJsonArray("profiles")), doc));
            rootElement.appendChild(createTextElement(ACTIVE_STRUCTURE, data.getJsonArray("structures", new JsonArray()).getString(0), doc)); //TODO Retrieve active structure from data
            addStructures(data.getJsonArray("structureNodes", new JsonArray()), doc, rootElement);
//            addStringArray(FUNCTION,  userFunctions, doc, rootElement);
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

    private void addStringArray(String casLabel, JsonArray data, Document doc, Element rootElement){
//        Element root = createElement(casLabel+"s", doc);
        for(Object item: data){
            if (item != null) rootElement.appendChild(createTextElement(casLabel, (String) item, doc));
        }

//        rootElement.appendChild(root);
    }

    private String getRight(JsonArray functions) {
        if (functions.contains("Personnel")) {
            return RIGHTS.ADMIN.toString();
        }

        if (functions.contains("Teacher")) {
            return RIGHTS.CHECKER.toString();
        }

        if (functions.contains("Student")) {
            return RIGHTS.EDITOR.toString();
        }

        return RIGHTS.READER.toString();
    }
}
