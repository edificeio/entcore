/*
 * Copyright © WebServices pour l'Éducation, 2014
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

import java.util.HashMap;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import fr.wseduc.cas.entities.User;

public class PronoteRegisteredService extends DefaultRegisteredService {

	private static final Logger log = LoggerFactory.getLogger(PronoteRegisteredService.class);

	@Override
	protected void prepareUser(User user, String userId, String service, JsonObject data) {
		user.setUser(data.getString(principalAttributeName));
		user.setAttributes(new HashMap<String, String>());

		try {
			if (data.getString("lastName") != null && data.getString("firstName") != null) {
				user.getAttributes().put("nom", data.getString("lastName"));
				user.getAttributes().put("prenom", data.getString("firstName"));
			}

			if (data.getString("birthDate") != null) {
				user.getAttributes().put("dateNaissance", data.getString("birthDate").replaceAll("([0-9]+)-([0-9]+)-([0-9]+)", "$3/$2/$1"));
			}
			if (data.getString("postalCode") != null) {
				user.getAttributes().put("codePostal", data.getString("postalCode"));
			}

			String category = null;
			JsonArray types = data.getJsonArray("type");
			for (Object type : types.getList()) {
			    switch(type.toString()) {
	                case "Student" :
	                    category = checkProfile(category,"National_1");
	                    break;
	                case "Teacher" :
	                    category = checkProfile(category,"National_3");
	                    break;
	                case "Relative" :
	                    category = checkProfile(category,"National_2");
	                    break;
	                case "Personnel" :
	                    category = checkProfile(category,"National_4");
	                    break;
	                }
			}
			if (category != null) {
				user.getAttributes().put("categories", category);
			}
		}
		catch (Exception e) {
			log.error("Failed to transform User for Pronote");
		}
	}
	private String checkProfile(String category, String national){
	    if(category== null){
            return national;
        }else{
           if(category.contains(national)){
               return category;
           }else{
               return category+";"+national;
           }
        }
    }
}
