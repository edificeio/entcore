/*
 * Copyright © "Open Digital Education", 2016
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

 */

package org.entcore.cas.services;

import fr.wseduc.cas.entities.User;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

public class MilliwebRegisteredService extends EnglishAttackRegisteredService {

	private static final Logger log = LoggerFactory.getLogger(MilliwebRegisteredService.class);

	protected static final String EA_MEF = "ENTPersonMEF";

	protected static final String EA_PHONE_NUMBER = "ENTPersonTelephone";

	protected static final String EA_ADDRESS = "ENTPersonAdresse";

	protected static final String EA_ATTRIBUTES = "attributes";

	@Override
	protected void prepareUserCas20(User user, final String userId, String service, final JsonObject data, final Document doc, final List<Element> additionnalAttributes) {
		user.setUser(data.getString(principalAttributeName));

		if (log.isDebugEnabled()){
			log.debug("START : prepareUserCas20 userId : " + userId);
		}

		try {
			if (log.isDebugEnabled()){
				log.debug("DATA : prepareUserCas20 data : " + data);
			}

			Element rootAttributes = createElement(EA_ATTRIBUTES, doc);

			// Uid
			if (data.containsKey("externalId")) {
				rootAttributes.appendChild(createTextElement(EA_ID, data.getString("externalId"), doc));
			}

			// Lastname
			if (data.containsKey("lastName")) {
				rootAttributes.appendChild(createTextElement(EA_LASTNAME, data.getString("lastName"), doc));
			}

			// Firstname
			if (data.containsKey("firstName")) {
				rootAttributes.appendChild(createTextElement(EA_FIRSTNAME, data.getString("firstName"), doc));
			}

			// Structures
			for (Object o : data.getJsonArray("structures", new JsonArray()).getList()) {
				if (!(o instanceof JsonObject)) continue;
				JsonObject structure = (JsonObject) o;
				if (structure.containsKey("UAI")) {
					rootAttributes.appendChild(createTextElement(EA_STRUCTURE_UAI, structure.getString("UAI"), doc));
				}
			}

			// MEF - Module Elementaire de formation
			if (data.containsKey("module")) {
				rootAttributes.appendChild(createTextElement(EA_MEF, data.getString("module"), doc));
			}

			// Email
			if (data.containsKey("emailInternal")) {
				rootAttributes.appendChild(createTextElement(EA_EMAIL, data.getString("emailInternal"), doc));
			} else if (data.containsKey("emailAcademy")) {
				rootAttributes.appendChild(createTextElement(EA_EMAIL, data.getString("emailAcademy"), doc));
			} else if (data.containsKey("email")) {
				rootAttributes.appendChild(createTextElement(EA_EMAIL, data.getString("email"), doc));
			}

			// Phone number
			if (data.containsKey("mobile")) {
				rootAttributes.appendChild(createTextElement(EA_PHONE_NUMBER, data.getString("mobile"), doc));
			}else if (data.containsKey("homePhone")){
				rootAttributes.appendChild(createTextElement(EA_PHONE_NUMBER, data.getString("homePhone"), doc));
			}

			// Adress
			if (data.containsKey("address")) {
				String address = data.getString("address");
				if (data.containsKey("zipCode")) {
					address += ", "+data.getString("zipCode");
					if (data.containsKey("city")) {
						address += ", "+data.getString("city");
						rootAttributes.appendChild(createTextElement(EA_ADDRESS, address, doc));
					}
				}
			}

			// Profile
			switch(data.getString("type")) {
				case "Student" :
					rootAttributes.appendChild(createTextElement(EA_PROFILES, "National_1", doc));
					break;
				case "Teacher" :
					rootAttributes.appendChild(createTextElement(EA_PROFILES, "National_3", doc));
					break;
				case "Relative" :
					rootAttributes.appendChild(createTextElement(EA_PROFILES, "National_2", doc));
					break;
				case "Personnel" :
					rootAttributes.appendChild(createTextElement(EA_PROFILES, "National_4", doc));
					break;
			}

			additionnalAttributes.add(rootAttributes);

		} catch (Exception e) {
			log.error("Failed to transform User for EnglishAttack", e);
		}
	}
}
