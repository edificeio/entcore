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
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NameCRIFRegisteredService extends AbstractCas20ExtensionRegisteredService {

	private static final Logger log = LoggerFactory.getLogger(NameCRIFRegisteredService.class);

	@Override
	public void configure(org.vertx.java.core.eventbus.EventBus eb, java.util.Map<String,Object> conf) {
		super.configure(eb, conf);
	};

	// Map of ID's academies
	private static final Map<String , String> SUFFIXE_CRIF = new HashMap<String , String>() {{
		put("PARIS","020");
		put("CRETEIL","009");
		put("VERSAILLES","027");
	}};


	private static final String ENT = "^ENT\\$([0-9]*)$";
	private static final String ACADEMY_MATCHES = "^(PARIS|CRETEIL|VERSAILLES)-([0-9]*)$";

	private static final Pattern ACADEMY_PATERN = Pattern.compile(ACADEMY_MATCHES);


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

		try {
			// Define attribute : cas:user
			if (data.containsField("externalId")) {
				String initialExternalId = data.getString("externalId");

				log.debug("PrepareUserCas20 : ExternalId Initial : " + initialExternalId);

				// Replace first ENT$ if it's at the beginning.
				initialExternalId = initialExternalId.replaceFirst(ENT,"$1");

				// We verify REXEXP
				Matcher academyMatcher = ACADEMY_PATERN.matcher(initialExternalId);
				String externalId= "";
				if (academyMatcher.matches()){
					externalId = academyMatcher.group(2) + SUFFIXE_CRIF.get(academyMatcher.group(1));
					log.debug("ExternalId Final formaté : " + externalId);
				} else {
					externalId = initialExternalId;
					log.debug("ExternalId n'a pu être décomposé : retour à l'ExternalId Initial : " + initialExternalId);
				}
				log.debug("ExternalId Final : " + externalId);
				user.setUser(externalId);
			}

		} catch (Exception e) {
			log.error("Failed to transform User for Uid service : ", e);
		}
	}

}
