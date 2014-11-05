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
