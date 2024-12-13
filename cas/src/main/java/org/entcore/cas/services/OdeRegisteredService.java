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
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.*;

public class OdeRegisteredService extends AbstractCas20ExtensionRegisteredService {

	private static final Logger log = LoggerFactory.getLogger(OdeRegisteredService.class);
	private static final String ATTRIBUTES = "attributes";
	private static final String DOLLAR = "$";
	private static final String EMAIL = "email";
	private static final String EMAIL_ACADEMY = "emailAcademy";
	private static final String EMAIL_INTERNAL = "emailInternal";
	private static final String ENT_PERSON_FUNCTIONS = "ENTPersonFunctions";
	private static final String ENT_PERSON_MAIL = "ENTPersonMail";
	private static final String ENT_PERSON_PHONE_NUMBER = "ENTPersonTelephone";
	private static final String ENT_PERSON_PROFILES = "ENTPersonProfiles";
	private static final String EXTERNAL_ID = "externalId";
	private static final String FIRST_NAME = "firstName";
	private static final String FUNCTION = "function";
	private static final String HOME_PHONE= "homePhone";
	private static final String LAST_NAME = "lastName";
	private static final String MOBILE = "mobile";
	private static final String NATIONAL_1 = "National_1";
	private static final String NATIONAL_2 = "National_2";
	private static final String NATIONAL_3 = "National_3";
	private static final String NATIONAL_4 = "National_4";
	private static final String NOMS = "Nom";
	private static final String PERSONNEL = "Personnel";
	private static final String PRENOMS = "Prenoms";
	private static final String RELATIVE = "Relative";
	private static final String STRUCTURES = "structures";
	private static final String STUDENT = "Student";
	private static final String TEACHER = "Teacher";
	private static final String TYPE = "type";
	private static final String UAI = "UAI";
	private static final String UFUNCTIONS = "ufunctions";


	@Override
	public void configure(EventBus eb, Map<String,Object> conf) {
		super.configure(eb, conf);
		this.directoryAction = "getUserInfos";
	}

	@Override
	protected void prepareUserCas20(User user, final String userId, String service, final JsonObject data, final Document doc, final List<Element> additionnalAttributes) {
		user.setUser(data.getString(principalAttributeName)); // We use UID here

		if (log.isDebugEnabled()){
			log.debug("START : prepareUserCas20 userId : " + userId);
		}

		try {
			if (log.isDebugEnabled()){
				log.debug("DATA : prepareUserCas20 data : " + data);
			}

			Element rootAttributes = createElement(ATTRIBUTES, doc);

			// Lastname
			if (data.containsKey(LAST_NAME)) {
				rootAttributes.appendChild(createTextElement(NOMS, data.getString(LAST_NAME), doc));
			}

			// Firstname
			if (data.containsKey(FIRST_NAME)) {
				rootAttributes.appendChild(createTextElement(PRENOMS, data.getString(FIRST_NAME), doc));
			}

			// Functions
			if (data.containsKey(STRUCTURES) && data.containsKey(UFUNCTIONS)) {
				JsonArray structures = data.getJsonArray(STRUCTURES, new JsonArray());
				JsonArray ufunctions = data.getJsonArray(UFUNCTIONS, new JsonArray());

				Map<String, String> structuresExternalIdsUaisMap = buildStructuresExternalIdsUaisMap(structures);
				Map<String, List<String>> structuresExternalIdsFunctionsMap = buildStructuresExternalIdsFunctionsMap(ufunctions);

				structuresExternalIdsUaisMap.forEach((structureExternalId, structureUai) -> {
					List<String> functions = structuresExternalIdsFunctionsMap.getOrDefault(structureExternalId, new ArrayList<>());

					Element functionInfos = createElement(ENT_PERSON_FUNCTIONS, doc);
					functionInfos.appendChild(createTextElement(UAI, structureUai, doc)); // UAI
					functionInfos.appendChild(createTextElement(FUNCTION, functions.toString(), doc)); // Functions

					rootAttributes.appendChild(functionInfos);
				});
			}

			// Email
			if (data.containsKey(EMAIL_INTERNAL)) {
				rootAttributes.appendChild(createTextElement(ENT_PERSON_MAIL, data.getString(EMAIL_INTERNAL), doc));
			} else if (data.containsKey(EMAIL_ACADEMY)) {
				rootAttributes.appendChild(createTextElement(ENT_PERSON_MAIL, data.getString(EMAIL_ACADEMY), doc));
			} else if (data.containsKey(EMAIL)) {
				rootAttributes.appendChild(createTextElement(ENT_PERSON_MAIL, data.getString(EMAIL), doc));
			}

			// Profile
			switch(data.getString(TYPE)) {
				case STUDENT :
					rootAttributes.appendChild(createTextElement(ENT_PERSON_PROFILES, NATIONAL_1, doc));
					break;
				case RELATIVE :
					rootAttributes.appendChild(createTextElement(ENT_PERSON_PROFILES, NATIONAL_2, doc));
					break;
				case TEACHER :
					rootAttributes.appendChild(createTextElement(ENT_PERSON_PROFILES, NATIONAL_3, doc));
					break;
				case PERSONNEL :
					rootAttributes.appendChild(createTextElement(ENT_PERSON_PROFILES, NATIONAL_4, doc));
					break;
			}

			additionnalAttributes.add(rootAttributes);

		} catch (Exception e) {
			log.error("Failed to transform User for ODE", e);
		}
	}

	private Map<String, String> buildStructuresExternalIdsUaisMap(JsonArray structures) {
		Map<String, String> structuresExternalIdsUaisMap = new HashMap<>();

		if (structures != null && !structures.isEmpty()) {
			structures.stream()
				.filter(JsonObject.class::isInstance)
				.map(JsonObject.class::cast)
				.filter(structure -> structure.containsKey(EXTERNAL_ID) && structure.containsKey(UAI))
				.forEach(structure -> structuresExternalIdsUaisMap.put(structure.getString(EXTERNAL_ID), structure.getString(UAI)));
		}

		return structuresExternalIdsUaisMap;
	}

	private Map<String, List<String>> buildStructuresExternalIdsFunctionsMap(JsonArray functions) {
		Map<String, List<String>> structuresExternalIdsFunctionsMap = new HashMap<>();

		if (functions != null && !functions.isEmpty()) {
			functions.stream()
				.filter(String.class::isInstance)
				.map(String.class::cast)
				.forEach(rawFunction -> {
					int first$pos = rawFunction.indexOf(DOLLAR);
					String externalId = rawFunction.substring(0, first$pos);

					int last$pos = rawFunction.lastIndexOf(DOLLAR);
					String function = rawFunction.substring(last$pos + 1);

					if (!structuresExternalIdsFunctionsMap.containsKey(externalId)) {
						structuresExternalIdsFunctionsMap.put(externalId, new ArrayList<>());
					}
					structuresExternalIdsFunctionsMap.get(externalId).add(function);
				});
		}

		return structuresExternalIdsFunctionsMap;
	}
}
