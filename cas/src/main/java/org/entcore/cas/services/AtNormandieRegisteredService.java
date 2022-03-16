package org.entcore.cas.services;

import fr.wseduc.cas.entities.User;
import io.vertx.core.json.JsonObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;


public class AtNormandieRegisteredService extends AbstractCas20ExtensionRegisteredService {

    protected static final String EA_ID = "uid";
    protected static final String EA_FIRSTNAME = "firstName";
    protected static final String EA_LASTNAME = "lastName";
    protected static final String EA_BIRTHDATE = "birthDate";

    @Override
    public void configure(io.vertx.core.eventbus.EventBus eb, java.util.Map<String,Object> conf){
        super.configure(eb, conf);
        this.directoryAction = "getUserInfos";
    }

    @Override
    protected void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionnalAttributes) {
        user.setUser(data.getString(principalAttributeName));

        if (log.isDebugEnabled()){
            log.debug("START : prepareUserCas20 userId : " + userId);
        }
        try{
            // Lastname
            if (data.containsKey(EA_LASTNAME)) {
                additionnalAttributes.add(createTextElement(EA_LASTNAME, data.getString("lastName"), doc));
            }

            // Firstname
            if (data.containsKey(EA_FIRSTNAME)) {
                additionnalAttributes.add(createTextElement(EA_FIRSTNAME, data.getString("firstName"), doc));
            }

            // birthDate
            if (data.containsKey(EA_BIRTHDATE)) {
                additionnalAttributes.add(createTextElement(EA_BIRTHDATE, data.getString("birthDate"), doc));
            }

        } catch (Exception e) {
            log.error("Failed to transform User for AtoutNormandie", e);
        }

    }
}
