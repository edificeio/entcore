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
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.utils.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class LabomepRegisteredService extends AbstractCas20ExtensionRegisteredService {

	private static final Logger log = LoggerFactory.getLogger(LabomepRegisteredService.class);

	@Override
	public void configure(io.vertx.core.eventbus.EventBus eb, Map<String,Object> conf) {
		super.configure(eb, conf);
		this.directoryAction = "getUserInfos";
	};

	@Override
	protected void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionnalAttributes) {
		user.setUser(data.getString(principalAttributeName));

		try {
			// Uid
			if (data.containsKey("externalId")) {
				additionnalAttributes.add(createTextElement("uid", data.getString("externalId"), doc));
			}

			// administratives Structures first
			final List<String> uaiList = new ArrayList<>();

			for (Object o : data.getJsonArray("administratives", new fr.wseduc.webutils.collections.JsonArray())) {
				JsonObject structure = (JsonObject) o;
				final String uai = structure.getString("UAI");
				if (!StringUtils.isEmpty(uai)) {
					uaiList.add(uai);
					additionnalAttributes.add(createTextElement("structures", uai, doc));
				}
			}

			for (Object o : data.getJsonArray("structures", new fr.wseduc.webutils.collections.JsonArray())) {
				JsonObject structure = (JsonObject) o;
				final String uai = structure.getString("UAI");
				if (!StringUtils.isEmpty(uai) && !uaiList.contains(uai)) {
					additionnalAttributes.add(createTextElement("structures", uai, doc));
				}
			}

			// classes
			for (Object o : data.getJsonArray("classes", new fr.wseduc.webutils.collections.JsonArray())) {
				JsonObject classe = (JsonObject) o;
				additionnalAttributes.add(createTextElement("classes", classe.getString("name"), doc));
			}

			// Profile
			switch(data.getString("type")) {
				case "Student" :
					additionnalAttributes.add(createTextElement("profile", "National_1", doc));
					break;
				case "Teacher" :
					additionnalAttributes.add(createTextElement("profile", "National_3", doc));
					break;
				case "Relative" :
					additionnalAttributes.add(createTextElement("profile", "National_2", doc));
					break;
				case "Personnel" :
					additionnalAttributes.add(createTextElement("profile", "National_4", doc));
					break;
			}

			// Lastname
			if (data.containsKey("lastName")) {
				additionnalAttributes.add(createTextElement("nom", data.getString("lastName"), doc));
			}

			// Firstname
			if (data.containsKey("firstName")) {
				additionnalAttributes.add(createTextElement("prenom", data.getString("firstName"), doc));
			}

			// Email
			if (data.containsKey("email")) {
				additionnalAttributes.add(createTextElement("email", data.getString("email"), doc));
			}

		} catch (Exception e) {
			log.error("Failed to extract user's attributes for Labomep", e);
		}
	}

}
