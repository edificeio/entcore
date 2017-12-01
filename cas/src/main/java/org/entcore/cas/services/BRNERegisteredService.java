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

import fr.wseduc.cas.entities.User;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class BRNERegisteredService extends DefaultRegisteredService {

    protected static final String PROFIL = "ENTPersonProfils";

    @Override
    public void configure(io.vertx.core.eventbus.EventBus eb, Map<String,Object> conf){
        super.configure(eb, conf);
    }

    @Override
    protected void prepareUser(User user, String userId, String service, JsonObject data) {
        //return the first uai as principalAttributeName
        for (Object s : data.getJsonArray("structureNodes", new JsonArray())) {
            if (s == null || !(s instanceof JsonObject)) continue;
            JsonObject structure = (JsonObject) s;
            String uai = structure.getString("UAI", "");
            if (!uai.isEmpty()) {
                user.setUser(uai);
                break;
            }
        }

        user.setAttributes(new HashMap<String, String>());

        // Profile
        JsonArray profiles = data.getJsonArray("type", new JsonArray());
        if (profiles.contains("Teacher")) {
            user.getAttributes().put(PROFIL, "National_3");
        } else if (profiles.contains("Student")) {
            user.getAttributes().put(PROFIL, "National_1");
        }
    }
}
