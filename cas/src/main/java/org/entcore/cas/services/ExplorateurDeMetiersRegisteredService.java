package org.entcore.cas.services;

import fr.wseduc.cas.entities.User;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Map;

public class ExplorateurDeMetiersRegisteredService extends AbstractCas20ExtensionRegisteredService {

    private static final Logger log = LoggerFactory.getLogger(ExplorateurDeMetiersRegisteredService.class);

    private static final String TEACH = "Teacher";
    private static final String STUD = "Student";
    private static final String RELA = "Relative";
    private static final String PERS = "Personnel";

    @Override
    public void configure(EventBus eb, Map<String, Object> conf) {
        super.configure(eb, conf);
    }

    @Override
    protected void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionnalAttributes) {
        user.setUser(data.getString(principalAttributeName));

        try{
            addAttributes(additionnalAttributes, doc, data);
        } catch (Exception e){
            log.error("Failed to transform User for Explorateur de Metiers", e);
        }
    }

    private void addAttributes(List<Element> additionnalAttributes, Document doc, JsonObject data){
        //Uid
        String uid = data.getString("externalId");
        if(uid != null) {
            additionnalAttributes.add(createTextElement("uid", uid, doc));
        }

        //RNE
        if(data.containsKey("structureNodes")) {
            JsonArray RNE = new JsonArray();
            for (Object o : data.getJsonArray("structureNodes")) {
                JsonObject structNode = (JsonObject) o;
                if (structNode != null) {
                    RNE.add(structNode.getString("UAI"));
                }
            }
            additionnalAttributes.add(createTextElement("ENTPersonStructRattachRNE", RNE.toString(), doc));
        }

        //Profile
        String type = (String)data.getJsonArray("type").getList().get(0);
        if(type != null) {
            switch (type) {
                case STUD:
                    additionnalAttributes.add(createTextElement("ENTPersonProfils", "National_1", doc));
                    break;
                case TEACH:
                    additionnalAttributes.add(createTextElement("ENTPersonProfils", "National_3", doc));
                    break;
                case RELA:
                    additionnalAttributes.add(createTextElement("ENTPersonProfils", "National_2", doc));
                    break;
                case PERS:
                    additionnalAttributes.add(createTextElement("ENTPersonProfils", "National_4", doc));
                    break;
            }
        }
    }
}
