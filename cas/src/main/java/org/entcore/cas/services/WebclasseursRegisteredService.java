package org.entcore.cas.services;

import java.util.List;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fr.wseduc.cas.entities.User;

public class WebclasseursRegisteredService extends AbstractCas20ExtensionRegisteredService {

	private static final Logger log = LoggerFactory.getLogger(WebclasseursRegisteredService.class);

	protected static final String WEBCLASSEURS_LOGIN = "user_login";
	protected static final String WEBCLASSEURS_PROFILE = "user_profile";

	@Override
	protected void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionnalAttributes) {
		user.setUser(data.getString(principalAttributeName));

		try {
			// Lastname
			if (data.containsField("login")) {
				additionnalAttributes.add(createTextElement(WEBCLASSEURS_LOGIN, data.getString("login"), doc));
			}

			// Profile
			JsonArray profiles = data.getArray("type");
			if (profiles.contains("Teacher") || profiles.contains("Personnel")) {
				// Teacher and Personnel seen alike for Webclasseurs
				additionnalAttributes.add(createTextElement(WEBCLASSEURS_PROFILE, "National_3", doc));
			}
			else if (profiles.contains("Student")) {
				additionnalAttributes.add(createTextElement(WEBCLASSEURS_PROFILE, "National_1", doc));
			}
			else if (profiles.contains("Relative")) {
				additionnalAttributes.add(createTextElement(WEBCLASSEURS_PROFILE, "National_2", doc));
			}

		} catch (Exception e) {
			log.error("Failed to transform User for Webclasseurs", e);
		}
	}
}
