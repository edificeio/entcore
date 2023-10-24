/*
 * Copyright © "Open Digital Education", 2023
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
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static fr.wseduc.webutils.Utils.getOrElse;

public class IkigaiRegisteredService extends AbstractCas20ExtensionRegisteredService {

	private static final Logger log = LoggerFactory.getLogger(IkigaiRegisteredService.class);

	protected static final String EA_ID = "uid";
	protected static final String EA_STRUCTURE = "ENTPersonStructRattach";
	protected static final String EA_STRUCTURE_UAI = "ENTPersonStructRattachRNE";
	protected static final String EA_STRUCTURE_NAME = "ENTPersonStructRattachName";
	protected static final String EA_PROFILES = "ENTPersonProfils";
	protected static final String EA_CLASSE = "ENTClasse";
	protected static final String EA_EMAIL = "ENTPersonMail";
	protected static final String EA_LASTNAME = "Nom";
	protected static final String EA_FIRSTNAME = "Prenom";

	@Override
	public void configure(EventBus eb, Map<String,Object> conf) {
		super.configure(eb, conf);
		this.directoryAction = "getUserStructuresClasses";
	};

	@Override
	protected void prepareUserCas20(User user,final String userId, String service,final JsonObject data, final Document doc,final List<Element> additionnalAttributes) {
		user.setUser(data.getString(principalAttributeName));

		if (log.isDebugEnabled()){
			log.debug("START : prepareUserCas20 userId : " + userId);
		}

		try {
			if (log.isDebugEnabled()){
				log.debug("DATA : prepareUserCas20 data : " + data);
			}

			// Uid
			if (data.containsKey("externalId")) {
				additionnalAttributes.add(createTextElement(EA_ID, data.getString("externalId"), doc));
			}

			// Structures
			Element rootStructures = createElement(EA_STRUCTURE+"s", doc);
			final Set<String> totalClasses = new HashSet<>();
			for (Object o : data.getJsonArray("structuresWithClasses", new JsonArray()).getList()) {
				if (!(o instanceof JsonArray)) continue;
				final JsonArray structureWithClasses = (JsonArray) o;
				Element rootStructure = createElement(EA_STRUCTURE, doc);
				final String uai = getOrElse(structureWithClasses.getString(0), "");
				final String name = getOrElse(structureWithClasses.getString(1), "");
				final JsonArray classes = structureWithClasses.getJsonArray(2);

				rootStructure.appendChild(createTextElement(EA_STRUCTURE_UAI, uai, doc));
				rootStructure.appendChild(createTextElement(EA_STRUCTURE_NAME, name, doc));

				Element rootClasses = createElement(EA_CLASSE+"s", doc);
				for (Object oc: classes) {
					rootClasses.appendChild(createTextElement(EA_CLASSE, oc.toString(), doc));
					totalClasses.add(oc.toString());
				}
				rootStructure.appendChild(rootClasses);
				rootStructures.appendChild(rootStructure);
			}
			additionnalAttributes.add(rootStructures);

			// Classes
			Element rootClasses = createElement(EA_CLASSE+"s", doc);
			for (String oc: totalClasses) {
				rootClasses.appendChild(createTextElement(EA_CLASSE, oc, doc));
			}
			additionnalAttributes.add(rootClasses);

			// Profile
			switch(data.getString("profile")) {
				case "Student" :
					additionnalAttributes.add(createTextElement(EA_PROFILES, "Eleve", doc));
					// Email
					if (data.containsKey("email")) {
						additionnalAttributes.add(createTextElement(EA_EMAIL, data.getString("email"), doc));
					}
					break;
				case "Teacher" :
					additionnalAttributes.add(createTextElement(EA_PROFILES, "Enseignant", doc));

					// Email
					if (data.containsKey("emailAcademy")) {
						additionnalAttributes.add(createTextElement(EA_EMAIL, data.getString("emailAcademy"), doc));
					} else if (data.containsKey("email")) {
						additionnalAttributes.add(createTextElement(EA_EMAIL, data.getString("email"), doc));
					}
					break;
				case "Relative" :
					additionnalAttributes.add(createTextElement(EA_PROFILES, "Parent", doc));
					break;
				case "Personnel" :
					additionnalAttributes.add(createTextElement(EA_PROFILES, "Personnel", doc));
					break;
			}

			// Lastname
			if (data.containsKey("lastName")) {
				additionnalAttributes.add(createTextElement(EA_LASTNAME, data.getString("lastName"), doc));
			}

			// Firstname
			if (data.containsKey("firstName")) {
				additionnalAttributes.add(createTextElement(EA_FIRSTNAME, data.getString("firstName"), doc));
			}
		} catch (Exception e) {
			log.error("Failed to transform User for Ikigai", e);
		}
	}

}
