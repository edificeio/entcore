/*
 * Copyright © "Open Digital Education", 2014
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

import fr.wseduc.cas.async.Handler;
import fr.wseduc.cas.entities.AuthCas;
import fr.wseduc.cas.entities.User;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class PmbRegisteredService extends DefaultRegisteredService {

	private static final Logger log = LoggerFactory.getLogger(PmbRegisteredService.class);

	@Override
	public void getUser(final AuthCas authCas, final String service, final Handler<User> userHandler) {
		final String userId = authCas.getUser();
		JsonObject jo = new JsonObject();
		jo.put("action", directoryAction).put("userId", userId);
		eb.request("directory", jo, handlerToAsyncHandler(event -> {
			JsonObject res = event.body().getJsonObject("result");
			log.debug("res : " + res);
			if ("ok".equals(event.body().getString("status")) && res != null) {
				// extract uais
				JsonArray uaisToCheck = new JsonArray();
				for (Object o : res.getJsonArray("structureNodes", new JsonArray()).getList()) {
					if (!(o instanceof JsonObject)) continue;
					JsonObject structure = (JsonObject) o;
					if (structure.containsKey("UAI")) {
						uaisToCheck.add(structure.getString("UAI"));
					}
				}
				JsonObject uais = new JsonObject().put("uais",uaisToCheck);
				eb.request("fr.openent.pmb.controllers.PmbController|getPrincipalUAIs", uais, handlerToAsyncHandler(eventPmb -> {
					JsonArray uaisResponse = eventPmb.body().getJsonArray("result");
					log.debug("res : " + res);
					if ("ok".equals(eventPmb.body().getString("status")) && uaisResponse != null) {
						User user = new User();
						JsonObject smallData = new JsonObject().put(principalAttributeName,res.getString(principalAttributeName));
						smallData.put("structureNodes",new JsonArray());
						for(Object correctUAI : uaisResponse){
							smallData.getJsonArray("structureNodes").add(new JsonObject().put("UAI",(String) correctUAI));
						}
						prepareUser(user, userId, service, smallData);
						userHandler.handle(user);
						createStatsEvent(authCas, res, service);
					} else {
						userHandler.handle(null);
					}
				}));
			} else {
				userHandler.handle(null);
			}
		}));
	}
}
