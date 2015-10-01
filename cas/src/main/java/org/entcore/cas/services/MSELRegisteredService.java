package org.entcore.cas.services;

import java.util.List;
import java.util.Map;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fr.wseduc.cas.entities.User;

public class MSELRegisteredService extends AbstractCas20ExtensionRegisteredService {
    
    private static final Logger log = LoggerFactory.getLogger(MSELRegisteredService.class);
    
    protected static final String MSEL_ID = "uid";
    protected static final String MSEL_STRUCTURE_UAI = "ENTPersonStructRattachRNE";
    protected static final String MSEL_PROFILES = "ENTPersonProfils";
    
    @Override
    public void configure(org.vertx.java.core.eventbus.EventBus eb, java.util.Map<String,Object> conf){
        super.configure(eb, conf);
        this.directoryAction = "getUserInfos";
    }

  
    @Override
    protected void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionnalAttributes) {
        // TODO Auto-generated method stub
        user.setUser(data.getString(principalAttributeName));
        try{
            //uid
            if (data.containsField("externalId")) {
                additionnalAttributes.add(createTextElement(MSEL_ID, data.getString("externalId"), doc));
            }
            // Structures
            for (Object o : data.getArray("structures", new JsonArray()).toList()) {
                Map<String, Object> structure = ((Map<String, Object>) o);
                if (structure.containsKey("UAI")) {
                    additionnalAttributes.add(createTextElement(MSEL_STRUCTURE_UAI, structure.get("UAI").toString(), doc));
                }
            }
            
            // Profile
            switch(data.getString("type")) {
            case "Student" :
                additionnalAttributes.add(createTextElement(MSEL_PROFILES, "National_1", doc));
                break;
            case "Teacher" :
                additionnalAttributes.add(createTextElement(MSEL_PROFILES, "National_3", doc));
                break;
            case "Relative" :
                additionnalAttributes.add(createTextElement(MSEL_PROFILES, "National_2", doc));
                break;
            case "Personnel" :
                additionnalAttributes.add(createTextElement(MSEL_PROFILES, "National_4", doc));
                break;
            }
        } catch (Exception e) {
            log.error("Failed to transform User for Mon stage en ligne", e);
        }
    }

}
