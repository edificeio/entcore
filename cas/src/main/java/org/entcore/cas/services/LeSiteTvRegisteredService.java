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

import java.util.List;
import java.util.Map;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fr.wseduc.cas.entities.User;

public class LeSiteTvRegisteredService extends AbstractCas20ExtensionRegisteredService {

	private static final Logger log = LoggerFactory.getLogger(LeSiteTvRegisteredService.class);

	protected static final String LSTV_ROOT = "FRANCE5";
	protected static final String LSTV_ID = "Identifiant";
	protected static final String LSTV_STRUCTURE_UAI = "CodeRNE";
	protected static final String LSTV_PROFILES = "Profil";
	protected static final String LSTV_LASTNAME = "Nom";
	protected static final String LSTV_FIRSTNAME = "Prenom";
	protected static final String LSTV_EMAIL = "Mail";

	@Override
	public void configure(io.vertx.core.eventbus.EventBus eb, java.util.Map<String,Object> conf) {
		super.configure(eb, conf);
		this.directoryAction = "getUserInfos";
	};

	@Override
	protected void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionnalAttributes) {
		user.setUser(data.getString(principalAttributeName));

		try {
			Element root = createElement(LSTV_ROOT, doc);

			// Uid
			if (data.containsKey("externalId")) {
				root.appendChild(createTextElement(LSTV_ID, data.getString("externalId"), doc));
			}

			// Structures
			for (Object o : data.getJsonArray("structures", new JsonArray()).getList()) {
				if (o == null || !(o instanceof JsonObject)) continue;
				JsonObject structure = (JsonObject) o;
				if (structure.containsKey("UAI")) {
					root.appendChild(createTextElement(LSTV_STRUCTURE_UAI, structure.getString("UAI"), doc));
				}
			}

			// Profile
			switch(data.getString("type")) {
			case "Student" :
				root.appendChild(createTextElement(LSTV_PROFILES, "National_1", doc));
				break;
			case "Teacher" :
				root.appendChild(createTextElement(LSTV_PROFILES, "National_3", doc));
				break;
			case "Relative" :
				root.appendChild(createTextElement(LSTV_PROFILES, "National_2", doc));
				break;
			case "Personnel" :
				root.appendChild(createTextElement(LSTV_PROFILES, "National_4", doc));
				break;
			}

			// Lastname
			if (data.containsKey("lastName")) {
				root.appendChild(createTextElement(LSTV_LASTNAME, data.getString("lastName"), doc));
			}

			// Firstname
			if (data.containsKey("firstName")) {
				root.appendChild(createTextElement(LSTV_FIRSTNAME, data.getString("firstName"), doc));
			}

			// Email
			if (data.containsKey("email")) {
				root.appendChild(createTextElement(LSTV_EMAIL, data.getString("email"), doc));
			}

			additionnalAttributes.add(root);

		} catch (Exception e) {
			log.error("Failed to transform User for LeSite.tv", e);
		}
	}

}
