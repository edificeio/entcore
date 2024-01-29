package org.entcore.cas.services;

import fr.wseduc.cas.entities.User;
import io.vertx.core.json.JsonObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;


public class JupyterRegisteredService extends AbstractCas20ExtensionRegisteredService {

    protected static final String UID = "uid";

    @Override
    protected void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionnalAttributes) {
        user.setUser(data.getString(principalAttributeName));

        try {
            if (data.containsKey("externalId")) {
                additionnalAttributes.add(createTextElement(UID, data.getString("externalId"), doc));
            }
        } catch (Exception e) {
            log.error("JupyterConnector@Failed to transform User for JupyterRegistered service" + e.getMessage());
        }
    }


}
