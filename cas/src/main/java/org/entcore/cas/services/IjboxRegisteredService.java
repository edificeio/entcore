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

public class IjboxRegisteredService extends EnglishAttackRegisteredService {

	private static final Logger log = LoggerFactory.getLogger(IjboxRegisteredService.class);

	protected static final String EA_ATTRIBUTES = "attributes";
	protected static final String EA_RNE = "rne";


	@Override
	protected void prepareUserCas20(User user, final String userId, String service, final JsonObject data,
									final Document doc, final List<Element> additionnalAttributes) {
		//cas:user
		user.setUser(data.getString(principalAttributeName));
		//cas:uid
		Element root = createElement(EA_ID, doc);
		root.setTextContent(data.getString(principalAttributeName));
		additionnalAttributes.add(root);


		if (log.isDebugEnabled()){
			log.debug("START : prepareUserCas20 userId : " + userId);
		}

		try {
			if (log.isDebugEnabled()){
				log.debug("DATA : prepareUserCas20 data : " + data);
			}
			//cas:attributes
			Element rootAttributes = createElement(EA_ATTRIBUTES, doc);

			// Structures
			for (Object o : data.getJsonArray("structures", new JsonArray()).getList()) {
				if (!(o instanceof JsonObject)) continue;
				JsonObject structure = (JsonObject) o;
				if (structure.containsKey("UAI")) {
					//cas:rne
					rootAttributes.appendChild(createTextElement(EA_RNE, structure.getString("UAI"), doc));
				}
			}

			additionnalAttributes.add(rootAttributes);

		} catch (Exception e) {
			log.error("Failed to transform User for EnglishAttack", e);
		}
	}
}
