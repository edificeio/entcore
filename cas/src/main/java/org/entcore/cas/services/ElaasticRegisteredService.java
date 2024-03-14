package org.entcore.cas.services;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import fr.wseduc.cas.entities.User;
import fr.wseduc.webutils.collections.JsonArray;
import io.vertx.core.json.JsonObject;

public class ElaasticRegisteredService extends DefaultRegisteredService {
    public static final String PROFILE = "profil";
    public static final String CLASSES = "ENTEleveClasses";
    public static final String LEVEL_FORMATION = "ENTEleveNivFormation";
    public static final String LEVEL = "level";
    protected static final String UAI = "rneCourant";
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void configure(io.vertx.core.eventbus.EventBus eb, java.util.Map<String, Object> conf) {
        super.configure(eb, conf);
    }

    @Override
    protected void prepareUser(final User user, final String userId, String service, final JsonObject data) {
        if (principalAttributeName != null) {
            user.setUser(data.getString(principalAttributeName));
        } else {
            user.setUser(userId);
        }

        user.setAttributes(new HashMap<String, String>());

        if (data.containsKey("birthDate")) {
            LocalDate date = LocalDate.parse(data.getString("birthDate"));
            String formattedDate = date.format(formatter);
            user.getAttributes().put("dateNaissance", formattedDate);
        }
        if (data.containsKey("structureNodes")) {
            JsonArray strutures = new JsonArray();
            for (Object obj : data.getJsonArray("structureNodes")) {
                if (obj instanceof JsonObject) {
                    strutures.add(((JsonObject) obj).getValue("UAI").toString());
                }
            }
            user.getAttributes().put(UAI, strutures.toString());
        }
        if (data.containsKey("email")) {
            user.getAttributes().put("mail", data.getValue("email").toString());
        }
        if (data.containsKey("lastName")) {
            user.getAttributes().put("nom", data.getValue("lastName").toString());
        }
        if (data.containsKey("firstName")) {
            user.getAttributes().put("prenom", data.getValue("firstName").toString());
        }
        if (data.containsKey("profiles"))
            switch ((String) data.getJsonArray("profiles").getList().get(0)) {
                case "Student":
                    user.getAttributes().put(PROFILE, "Eleve");
                    if (data.containsKey(LEVEL))
                        user.getAttributes().put(LEVEL_FORMATION, data.getString(LEVEL));
                    getClasses(data, user);
                    break;
                case "Teacher":
                    user.getAttributes().put(PROFILE, "Professeur");
                    getClasses(data, user);
                    break;
                case "Personnel":
                    user.getAttributes().put(PROFILE, "Personnel");
                    break;
                case "Relative":
                    user.getAttributes().put(PROFILE, "Parent");
                    break;
            }

    }

    private void getClasses(JsonObject data, User user) {
        if (data.containsKey("classes2D")) {
            user.getAttributes().put(CLASSES, data.getValue("classes2D").toString());
        } else if (data.containsKey("classes") && !data.containsKey("classes2D")) {
            user.getAttributes().put(CLASSES, data.getValue("classes").toString());
        }
    }
}
