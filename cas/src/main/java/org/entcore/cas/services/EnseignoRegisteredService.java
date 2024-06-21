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

import fr.wseduc.cas.async.Handler;
import fr.wseduc.cas.entities.AuthCas;
import fr.wseduc.cas.entities.User;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.utils.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class EnseignoRegisteredService extends AbstractCas20ExtensionRegisteredService {

	private static final Logger log = LoggerFactory.getLogger(EnseignoRegisteredService.class);


	@Override
	public void configure(io.vertx.core.eventbus.EventBus eb, java.util.Map<String,Object> conf) {
		super.configure(eb, conf);
	}

	@Override
	public void getUser(final AuthCas authCas, final String service, final Handler<User> userHandler) {
		final String userId = authCas.getUser();
		JsonObject jo = new JsonObject();
		jo.put("action", directoryAction).put("userId", userId);
		eb.request("directory", jo, handlerToAsyncHandler(event -> {
			JsonObject res = event.body().getJsonObject("result");
			log.debug("res : " + res);
			if ("ok".equals(event.body().getString("status")) && res != null) {
				JsonObject joG = new JsonObject();
				joG.put("action", "getUserGoups").put("userId", userId);
				eb.request("directory", joG, handlerToAsyncHandler(groups -> {
					JsonArray groupsRes = groups.body().getJsonArray("result");
					log.debug("groups : " + groupsRes);
					if ("ok".equals(groups.body().getString("status")) && groupsRes != null) {
						res.put("userGroups",groupsRes);
						User user = new User();
						prepareUser(user, userId, service, res);
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

	@Override
	protected void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionnalAttributes) {
		user.setUser(userId);
		try {
			additionnalAttributes.add(createTextElement("user_nom", data.getString("lastName"), doc));
			additionnalAttributes.add(createTextElement("user_prenom", data.getString("firstName"), doc));
			additionnalAttributes.add(createTextElement("user_profil", data.getJsonArray("type").getString(0), doc));

			// Structures
			for (Object s : data.getJsonArray("structureNodes", new JsonArray()).getList()) {
				if (!(s instanceof JsonObject)) continue;
				JsonObject structure = (JsonObject) s;
				if (structure.containsKey("UAI")) {
					Element root = createElement("structure", doc);
					root.appendChild(createTextElement("UAI", structure.getString("UAI"), doc));
					JsonArray classes = new JsonArray();
					JsonArray groupes_enseignements = new JsonArray();
					JsonArray groupes_manuels = new JsonArray();
					// groups : classes, manual groups...
					for (Object o : data.getJsonArray("userGroups", new JsonArray())) {
						JsonObject group = (JsonObject) o;
						if(group.containsKey("structures") && group.getValue("structures") != null) {
							JsonObject structJson = group.getJsonArray("structures").getJsonObject(0);
							if(structJson.getString("id").equals(structure.getString("id"))) {
								if (group.containsKey("classes") && group.getValue("classes") != null) {
									for (Object c : group.getJsonArray("classes", new JsonArray())) {
										JsonObject classe = (JsonObject) c;
										if (classe.containsKey("name") && classe.getString("name") != null) {
											classes.add(classe.getString("name"));
										}
									}
								} else if (group.getString("type").equals("FunctionalGroup")) {
									groupes_enseignements.add(group.getString("name"));
								} else if (group.getString("type").equals("ManualGroup")) {
									groupes_manuels.add(group.getString("id") + ";" + group.getString("name"));
								}
							}
						}
					}
					//For ordering the attributes
					for (Object c : classes) {
						String className = (String) c;
						root.appendChild(createTextElement("classes", className, doc));
					}
					for (Object g : groupes_enseignements) {
						String groupName = (String) g;
						root.appendChild(createTextElement("groupes_enseignements", groupName, doc));
					}
					for (Object g : groupes_manuels) {
						String groupName = (String) g;
						root.appendChild(createTextElement("groupes_manuels", groupName, doc));
					}

					additionnalAttributes.add(root);
				}
			}

			// children
			for (Object o : data.getJsonArray("children", new JsonArray())) {
				JsonObject child = (JsonObject) o;
				if(child.getValue("id") != null){
					additionnalAttributes.add(createTextElement("childrens", child.getString("id"), doc));
				}
			}

			// parents
			for (Object o : data.getJsonArray("parents", new JsonArray())) {
				JsonObject parent = (JsonObject) o;
				if(parent.getValue("id") != null){
					additionnalAttributes.add(createTextElement("parents", parent.getString("id"), doc));
				}
			}


		} catch (Exception e) {
			log.error("Failed to transform User for Enseigno CAS extension", e);
		}
	}

}
