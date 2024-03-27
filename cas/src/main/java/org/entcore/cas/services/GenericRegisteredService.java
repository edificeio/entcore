package org.entcore.cas.services;

import fr.wseduc.cas.entities.User;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class GenericRegisteredService extends AbstractCas20ExtensionRegisteredService {

    private static final Logger log = LoggerFactory.getLogger(GenericRegisteredService.class);

    protected static final String DF_ID = "user";
    protected static final String DF_STRUCTURE = "school";
    protected static final String DF_STRUCTURE_UAI = "UAI";
    protected static final String DF_STRUCTURE_ID = "externalId";
    protected static final String DF_PROFILES = "profile";
    protected static final String DF_CLASS = "classe";
    protected static final String DF_DISCIPLINE = "discipline";
    protected static final String DF_EMAIL = "email";
    protected static final String DF_LASTNAME = "lastName";
    protected static final String DF_BIRTHDATE = "birthDate";
    protected static final String DF_DISPLAYNAME = "displayName";
    protected static final String DF_FIRSTNAME = "fistName";
    protected static final String DF_FONCTIONAL_GROUP = "functionalGroup";
    protected static final String DF_TYPE = "type";
    protected static final String DF_STRUCTURES_NODES = "structureNodes";
    protected static final String DF_ADMINISTRATIVE_STRUCTURES = "administrativeStructures";
    protected static final String DF_NAME = "name";
    protected static final String DF_CLASS_CATEGORY = "classCategories";

    @Override
    public void configure(EventBus eb, Map<String, Object> conf) {
        super.configure(eb, conf);
        this.directoryAction = "getUserStructuresGroup";
    };

    @Override
    protected void prepareUserCas20(User user, final String userId, String service, final JsonObject data,
            final Document doc, final List<Element> additionnalAttributes) {
        Element rootStructures = createElement(DF_STRUCTURE + "s", doc);
        Element rootAttributes = createElement("attributes", doc);
        Element rootUserAttributes = createElement("userAttributes", doc);
        Element rootGroups = createElement(DF_FONCTIONAL_GROUP + "s", doc);

        try {
            // uid
            if (data.containsKey(DF_STRUCTURE_ID) && data.getValue(DF_STRUCTURE_ID) != null) {
                additionnalAttributes
                        .add(createTextElement(DF_ID,
                                data.getString(DF_STRUCTURE_ID), doc));
            }
            // date
            rootAttributes
                    .appendChild(createTextElement("longTermAuthenticationRequestTokenUsed", "false", doc));
            rootAttributes.appendChild(
                    createTextElement("authenticationdate", java.time.LocalDateTime.now().toString(), doc));
            rootAttributes.appendChild(createTextElement("isfromnewlogin",
                    data.getValue("lastLogin") == null ? "true" : "false", doc));

            // uid-login
            if (data.containsKey("login") && data.getValue("login") != null) {
                rootUserAttributes
                        .appendChild(createTextElement("login",
                                data.getString("login"), doc));
            }
            // Lastname
            if (data.containsKey(DF_LASTNAME) && data.getValue(DF_LASTNAME) != null) {
                rootUserAttributes
                        .appendChild(createTextElement(DF_LASTNAME, data.getString(DF_LASTNAME), doc));
            }

            // Firstname
            if (data.containsKey(DF_FIRSTNAME) && data.getValue(DF_FIRSTNAME) != null) {
                rootUserAttributes
                        .appendChild(createTextElement(DF_FIRSTNAME, data.getString(DF_FIRSTNAME), doc));
            }
            // DisplayName
            if (data.containsKey(DF_DISPLAYNAME) && data.getValue(DF_DISPLAYNAME) != null) {
                rootUserAttributes
                        .appendChild(createTextElement(DF_DISPLAYNAME, data.getString(DF_DISPLAYNAME), doc));
            }
            // birthDate
            if (data.containsKey(DF_BIRTHDATE) && data.getValue(DF_BIRTHDATE) != null) {
                rootUserAttributes
                        .appendChild(createTextElement(DF_BIRTHDATE, data.getString(DF_BIRTHDATE), doc));
            }
            // Email
            if (data.containsKey(DF_EMAIL) && data.getValue(DF_EMAIL) != null) {
                rootUserAttributes.appendChild(createTextElement(DF_EMAIL, data.getString(DF_EMAIL), doc));
            }
            // Profile
            if (data.containsKey(DF_TYPE) && data.getValue(DF_TYPE).toString() != null) {
                switch (data.getJsonArray(DF_TYPE).getList().get(0).toString()) {
                    case "Student":
                        rootUserAttributes.appendChild(createTextElement(DF_PROFILES, "Student", doc));
                        break;
                    case "Teacher":
                        rootUserAttributes.appendChild(createTextElement(DF_PROFILES, "Teacher", doc));
                        break;
                    case "Relative":
                        rootUserAttributes.appendChild(createTextElement(DF_PROFILES, "Relative", doc));
                        break;
                    case "Personnel":
                        rootUserAttributes.appendChild(createTextElement(DF_PROFILES, "Personnel", doc));
                        break;
                }
            }
            // Structures
            if (data.containsKey(DF_STRUCTURES_NODES) && data.getValue(DF_STRUCTURES_NODES) != null) {
                for (Object o : data.getJsonArray(DF_STRUCTURES_NODES).getList()) {
                    if (o == null || !(o instanceof JsonObject))
                        continue;
                    JsonObject structure = (JsonObject) o;
                    Element rootStructure = createElement(DF_STRUCTURE, doc);
                    Element rootStructureClass = createElement(DF_CLASS + "s", doc);
                    if (structure.containsKey(DF_NAME)) {
                        rootStructure.setAttribute(DF_NAME, structure.getString(DF_NAME));
                    }
                    if (structure.containsKey("UAI")) {
                        rootStructure.setAttribute(DF_STRUCTURE_ID, structure.getString("UAI"));
                    }
                    if (data.containsKey(DF_ADMINISTRATIVE_STRUCTURES)
                            && data.getValue(DF_ADMINISTRATIVE_STRUCTURES) != null
                            && data.getValue(DF_ADMINISTRATIVE_STRUCTURES) instanceof JsonArray) {
                        JsonObject administrativeStructures = new JsonObject();
                        if (data.getJsonArray(DF_ADMINISTRATIVE_STRUCTURES).getList().get(0) instanceof JsonObject) {
                            administrativeStructures = (JsonObject) data.getJsonArray(DF_ADMINISTRATIVE_STRUCTURES)
                                    .getList().get(0);
                        }
                        Boolean main = structure.getString("id")
                                .equals(administrativeStructures.getString("id"));
                        rootStructure.setAttribute("main", main.toString());
                    }
                    if (structure.containsKey(DF_TYPE)) {
                        rootStructure.setAttribute(DF_TYPE, structure.getString(DF_TYPE));
                    }
                    // class
                    if (data.containsKey(DF_CLASS + "s2D")) {
                        addString(rootStructureClass, DF_CLASS,
                                getClassCurrentStructures(data, structure.getString(DF_STRUCTURE_ID),
                                        DF_CLASS + "s2D"),
                                doc);
                    } else if (data.containsKey(DF_CLASS + "s") && !data.containsKey(DF_CLASS + "s2D")) {
                        addString(rootStructureClass, DF_CLASS,
                                getClassCurrentStructures(data, structure.getString(DF_STRUCTURE_ID),
                                        DF_CLASS + "s2D"),
                                doc);
                    }
                    // functionalGroups
                    if (data.containsKey(DF_FONCTIONAL_GROUP + "s")) {
                        for (Object group : data.getJsonArray(DF_FONCTIONAL_GROUP + "s").getList()) {
                            if (group instanceof JsonObject && ((JsonObject) group).containsKey(DF_NAME)
                                    && ((JsonObject) group).getString("functionalGroup") != null
                                    && structure.getString(DF_STRUCTURE_ID)
                                            .equals(((JsonObject) group).getString("structureExternalId"))) {
                                rootGroups.appendChild(
                                        createTextElement(DF_FONCTIONAL_GROUP, ((JsonObject) group).getString(DF_NAME),
                                                doc));
                                rootStructure.appendChild(rootGroups);
                            }

                        }
                    }
                    rootStructure.appendChild(rootStructureClass);
                    rootStructures.appendChild(rootStructure);
                }
                rootUserAttributes.appendChild(rootStructures);
            }

            rootAttributes.appendChild(rootUserAttributes);
            additionnalAttributes.add(rootAttributes);

        } catch (

        Exception e) {
            log.error("Failed to transform User ", e);
        }
    }

    private void addString(Element root, String casLabel, List<JsonObject> data, Document doc) {
        for (JsonObject classStructure : data) {
            if (data != null) {
                Element rootClass = createTextElement(casLabel, classStructure.getString("className"), doc);
                rootClass.setAttribute("externalId", classStructure.getString("ClassExternalId"));
                root.appendChild(rootClass);
            }
        }
    }

    private List<JsonObject> getClassCurrentStructures(JsonObject data, String structureExternalId, String ClassTitle) {
        JsonArray classList = new JsonArray();
        classList.addAll(data.getJsonArray(ClassTitle));
        List<JsonObject> classCurrentStructure = new ArrayList<>();
        for (Object c : classList) {
            if (c instanceof String && ((String) c).contains("$")) {
                String[] idClass = ((String) c).split("\\$");
                if (idClass[0].equals(structureExternalId)) {
                    classCurrentStructure.add(new JsonObject()
                            .put("externalId", idClass[0])
                            .put("className", idClass[1])
                            .put("ClassExternalId", ((String) c)));
                }
            } else {
                log.info("the class name is not in the correct format");
            }
        }
        return classCurrentStructure;
    }

}