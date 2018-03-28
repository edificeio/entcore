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

import fr.wseduc.cas.entities.User;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AcademicSuffixRegisteredService extends AbstractCas20ExtensionRegisteredService {

	private static final Logger log = LoggerFactory.getLogger(AcademicSuffixRegisteredService.class);

	@Override
	public void configure(io.vertx.core.eventbus.EventBus eb, java.util.Map<String,Object> conf) {
		super.configure(eb, conf);
		if (conf.containsKey("academicSuffix") && conf.containsKey("academicPattern")) {
			ACADEMIC_SUFFIX.clear();

			final List<String> academicSuffixString =  (List<String>) conf.get("academicSuffix");
			for (final String as : academicSuffixString) {
				final JsonObject jo = new JsonObject(as);
				ACADEMIC_SUFFIX.put(jo.getString("source"), jo.getString("target"));
			}

			this.academicConfPattern = (String) conf.get("academicPattern");
			this.academicPattern = Pattern.compile(academicConfPattern);
		}

		if (conf.containsKey("externalIdPrefix")) {
			this.externalIdPrefix = (String) conf.get("externalIdPrefix");
		}

	};

	// Default Map of ID's academies
	private static final Map<String , String> ACADEMIC_SUFFIX = new HashMap<String , String>() {{
		put("PARIS","020");
		put("PARIS-CRIF","");
		put("CRETEIL","009");
		put("CRETEIL-CRIF","");
		put("VERSAILLES","027");
		put("VERSAILLES-CRIF","");
		put("AGRICOLE","028");
	}};

	private String externalIdPrefix = "^ENT\\$([0-9]*)$";
	private String academicConfPattern = "^(PARIS|CRETEIL|VERSAILLES|PARIS-CRIF|VERSAILLES-CRIF|CRETEIL-CRIF|AGRICOLE)-([a-zA-Z0-9-]*)$";

	private  Pattern academicPattern = Pattern.compile(academicConfPattern);


	/**
	 * Replace or format cas-user attribute response with formated externalId
	 * In all case : remove prefix ENT$  if is at the beginning
	 * Split externalId with - character, if we have only one - : remove the prefix and add the correct suffix
	 * In other case, keep the same externalId
	 * @param user : authenticated user
	 * @param userId
	 * @param service
	 * @param data
	 * @param doc
	 * @param additionnalAttributes
	 */
	@Override
	protected void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionnalAttributes) {
		user.setUser(data.getString(principalAttributeName));
		log.debug("conf : eip : " + externalIdPrefix + ", acp : " + academicConfPattern + ", map : " + ACADEMIC_SUFFIX.toString());

		try {
			// Define attribute : cas:user
			if (data.containsKey("externalId")) {
				String initialExternalId = data.getString("externalId");

				log.debug("PrepareUserCas20 : ExternalId Initial : " + initialExternalId);

				// Replace first conf prefix if it's at the beginning.
				initialExternalId = initialExternalId.replaceFirst(this.externalIdPrefix,"$1");

				// We verify REXEXP
				Matcher academyMatcher = this.academicPattern.matcher(initialExternalId);
				String externalId= "";
				if (academyMatcher.matches()){
					externalId = academyMatcher.group(2) + ACADEMIC_SUFFIX.get(academyMatcher.group(1));
					log.debug("ExternalId target : " + externalId);
				} else {
					externalId = initialExternalId;
					log.debug("External id could not be processed, Initial External id is used : " + initialExternalId);
				}
				log.debug("ExternalId Final : " + externalId);
				user.setUser(externalId);
			}

		} catch (Exception e) {
			log.error("Failed to transform User for Uid service : ", e);
		}
	}

}
