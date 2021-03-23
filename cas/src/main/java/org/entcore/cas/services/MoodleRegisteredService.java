package org.entcore.cas.services;

import fr.wseduc.cas.entities.User;
import io.vertx.core.json.JsonObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

public class MoodleRegisteredService extends UuidRegisteredService {
    @Override
    protected void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionnalAttributes) {
        super.prepareUserCas20(user, userId, service, data, doc, additionnalAttributes);
    }
}