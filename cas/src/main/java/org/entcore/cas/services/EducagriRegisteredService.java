/*
 * Copyright © WebServices pour l'Éducation, 2016
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.cas.services;

import java.util.List;
import java.util.Map;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fr.wseduc.cas.entities.User;

public class EducagriRegisteredService extends AbstractCas20ExtensionRegisteredService {
    
    private static final Logger log = LoggerFactory.getLogger(EducagriRegisteredService.class);
    
    protected static final String EA_ID = "uid";
    protected static final String EA_STRUCTURE_UAI = "ENTPersonStructRattachRNE";
    protected static final String EA_PROFILES = "ENTPersonProfils";
    
    @Override
    public void configure(io.vertx.core.eventbus.EventBus eb, java.util.Map<String,Object> conf){
        super.configure(eb, conf);
        this.directoryAction = "getUserInfos";
    }

    @Override
    protected void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionnalAttributes) {
        user.setUser(data.getString(principalAttributeName));
        try{
            //uid
            if (data.containsKey("externalId")) {
                additionnalAttributes.add(createTextElement(EA_ID, data.getString("externalId"), doc));
            }
            
            // Structures
            for (Object o : data.getJsonArray("structures", new fr.wseduc.webutils.collections.JsonArray()).getList()) {
                Map<String, Object> structure = ((Map<String, Object>) o);
                if (structure.containsKey("UAI")) {
                    additionnalAttributes.add(createTextElement(EA_STRUCTURE_UAI, structure.get("UAI").toString(), doc));
                }
            }
            
            // Profile
            switch(data.getString("type")) {
            case "Student" :
                additionnalAttributes.add(createTextElement(EA_PROFILES, "National_1", doc));
                break;
            case "Teacher" :
                additionnalAttributes.add(createTextElement(EA_PROFILES, "National_3", doc));
                break;
            case "Personnel" :
                additionnalAttributes.add(createTextElement(EA_PROFILES, "National_4", doc));
                break;
            }
        } catch (Exception e) {
            log.error("Failed to transform User for Educagri", e);
        }
    }

}
