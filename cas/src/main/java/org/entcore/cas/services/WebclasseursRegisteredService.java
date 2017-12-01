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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
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
			if (data.containsKey("login")) {
				additionnalAttributes.add(createTextElement(WEBCLASSEURS_LOGIN, data.getString("login"), doc));
			}

			// Profile
			JsonArray profiles = data.getJsonArray("type");
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
