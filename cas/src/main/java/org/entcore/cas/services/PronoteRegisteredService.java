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

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import fr.wseduc.cas.entities.User;

public class PronoteRegisteredService extends DefaultRegisteredService {

	private static final Logger log = LoggerFactory.getLogger(PronoteRegisteredService.class);

	@Override
	protected void prepareUser(User user, String userId, JsonObject data) {
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
			JsonArray types = data.getArray("type");
			for (Object type : types.toList()) {
				switch(type.toString()) {
				case "Student" :
					category = (category == null ? "" : ";") + "National_1";
					break;
				case "Teacher" :
					category = (category == null ? "" : ";") + "National_3";
					break;
				case "Relative" :
					category = (category == null ? "" : ";") + "National_2";
					break;
				case "Staff" :
					category = (category == null ? "" : ";") + "National_4";
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
}
