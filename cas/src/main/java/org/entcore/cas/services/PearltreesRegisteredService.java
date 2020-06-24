package org.entcore.cas.services;

import fr.wseduc.cas.entities.User;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PearltreesRegisteredService extends AbstractCas20ExtensionRegisteredService {

    private static final Logger log = LoggerFactory.getLogger(PearltreesRegisteredService.class);

    private static final String TEACH = "Teacher";
    private static final String STUD = "Student";

    @Override
    public void configure(EventBus eb, Map<String, Object> conf) {
        super.configure(eb, conf);
    }

    @Override
    protected void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionnalAttributes) {
        user.setUser(data.getString(principalAttributeName));
        user.setAttributes(new HashMap<String, String>());

        try{
            //Type
            String type = (String)data.getJsonArray("type").getList().get(0);
            user.getAttributes().put("type", type);

            attributesCommonAllUsers(user, data);

            //Profiles + specific elements
            switch (type){
                case TEACH :
                    attributesSpecificTeacher(user, data);
                    break;

                case STUD :
                    attributesSpecificStudent(user, data);
                    break;
            }

        } catch (Exception e){
            log.error("Failed to transform User for Pearltrees", e);
        }

    }

    private void attributesCommonAllUsers(User user, JsonObject data){
        //ExternalId
        String externalId = data.getString("externalId");
        if(externalId != null) {
            user.getAttributes().put("externalId", externalId);
        }

        //Id
        String id = data.getString("id");
        if(id != null){
            user.getAttributes().put("id", id);
        }

        //FirstName
        String firstName = data.getString("firstName");
        if(firstName != null){
            user.getAttributes().put("firstName", firstName);
        }

        //LastName
        String lastName = data.getString("lastName");
        if(lastName != null){
            user.getAttributes().put("lastName", lastName);
        }

        //Structures
        if(data.containsKey("structures")) {
            for (Object o : data.getJsonArray("structures")) {
                String struct = (String) o;
                if (struct != null) {
                    user.getAttributes().put("structures", struct);
                }
            }
        }

        //AdministrativeStructures
        if(data.containsKey("administrativeStructures")) {
            for (Object o : data.getJsonArray("administrativeStructures")) {
                JsonObject admin = (JsonObject) o;
                String adminId = admin.getString("id");
                if (adminId != null) {
                    user.getAttributes().put("administrativeStructures", adminId);
                }
            }
        }

        //StructureNodes
        if(data.containsKey("structureNodes")) {
            for (Object o : data.getJsonArray("structureNodes")) {
                JsonObject structNode = (JsonObject) o;
                if (structNode != null) {
                    user.getAttributes().put("structureNode", structNode.toString());
                }
            }
        }
    }

    private void attributesSpecificTeacher(User user, JsonObject data){
        //Profile
        String[] profile = {"National_3"};
        user.getAttributes().put("profiles", Arrays.toString(profile));

        //Email
        String email = data.getString("email");
        if(email != null){
            user.getAttributes().put("email", email);
        }

        //EmailAcademy
        String emailAcademy = data.getString("emailAcademy");
        if(emailAcademy != null){
            user.getAttributes().put("emailAcademy", emailAcademy);
        }
    }

    private void attributesSpecificStudent(User user, JsonObject data){
        //Profile
        String[] profile = {"National_1"};
        user.getAttributes().put("profiles", Arrays.toString(profile));

        //Classes
        JsonArray classe = data.getJsonArray("classes");
        if(classe.size() != 0){
            user.getAttributes().put("classes", classe.toString());
        }
    }
}
