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

import fr.wseduc.cas.entities.AuthCas;
import fr.wseduc.cas.entities.User;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.*;
import java.util.stream.Collectors;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;

public class WebGerestRegisteredService extends AbstractCas20ExtensionRegisteredService {
	private static final Logger log = LoggerFactory.getLogger(WebGerestRegisteredService.class);
	protected static List<String> neoFunctions = new ArrayList<>();

	protected static final String WG_LASTNAME = "nom";
	protected static final String WG_FIRSTNAME = "prenom";
	protected static final String WG_ID = "id";
	protected static final String WG_STRUCTURE_UAI = "uai";
	protected static final String WG_PROFILES = "profils";
	protected static final String WG_FUNCTIONS = "fonctions";
	protected static final String WG_SUBFUNCTIONS = "sousFonctions";
	private static final String TEACHER = "Teacher";
	private static final String STUDENT = "Student";
	private static final String RELATIVE = "Relative";
	private static final String PERSONNEL = "Personnel";
	private static final String DOLLAR = "$";
	protected static final String ID = "id";
	protected static final String STRUCTURES = "structures";
	protected static final String UAI = "UAI";
	protected static final String STRUCTURE_NODES = "structureNodes";
	protected static final String EXTERNAL_ID = "externalId";
	protected static final String TYPE = "type";
	private static final String FIRSTNAME = "firstName";
	private static final String LASTNAME = "lastName";
	private static final String FUNCTIONS = "functions";
	private static final String SUBFUNCTIONS = "subfunctions";
	private static final String ACTION = "action";
	private static final String USERID = "userId";
	private static final String DIRECTORY = "directory";
	private static final String RESULT = "result";
	private static final String STATUS = "status";
	private static final String OK = "ok";

	@Override
	public void configure(io.vertx.core.eventbus.EventBus eb, Map<String,Object> conf) {
		super.configure(eb, conf);
		this.directoryAction = "getUser";
	}

	@Override
	public void getUser(final AuthCas authCas, final String service, final fr.wseduc.cas.async.Handler<User> userHandler) {
		final String userId = authCas.getUser();
		JsonObject jo = new JsonObject();
		jo.put(ACTION, directoryAction).put(USERID, userId);
		eb.request(DIRECTORY, jo, handlerToAsyncHandler(event -> {
			JsonObject res = event.body().getJsonObject(RESULT);
			log.debug("res : " + res);
			if (OK.equals(event.body().getString(STATUS)) && res != null) {
				getFunctions(res.getString(ID, null))
					.onSuccess(functions -> {
						formatNeoFunctions(functions);
						User user = new User();
						prepareUser(user, userId, service, res);
						userHandler.handle(user);
						createStatsEvent(authCas, res, service);
					})
					.onFailure(err -> {
						log.error("[WebGerestRegisteredService::getUser] " + err.getMessage());
						userHandler.handle(null);
					});
			} else {
				userHandler.handle(null);
			}
		}));
	}

	@Override
	protected void prepareUserCas20(User user, String userId, String service, JsonObject data, Document doc, List<Element> additionnalAttributes) {
		user.setUser(data.getString(principalAttributeName));
		data.remove(principalAttributeName);
		Map<String, String> attributes = new HashMap<>();

		try {
			// Uid
			if (data.containsKey(ID)) {
				attributes.put(WG_ID, data.getString(ID));
			}

			// Lastname
			if (data.containsKey(LASTNAME)) {
				attributes.put(WG_LASTNAME, data.getString(LASTNAME));
			}

			// Firstname
			if (data.containsKey(FIRSTNAME)) {
				attributes.put(WG_FIRSTNAME, data.getString(FIRSTNAME));
			}

			// Profiles
			JsonArray profiles = data.getJsonArray(TYPE);
			if (profiles.contains(STUDENT)) {
				attributes.put(WG_PROFILES, "National_1");
			}
			else if (profiles.contains(RELATIVE)) {
				attributes.put(WG_PROFILES, "National_2");
			}
			else if (profiles.contains(TEACHER)) {
				attributes.put(WG_PROFILES, "National_3");
			}
			else if (profiles.contains(PERSONNEL)) {
				attributes.put(WG_PROFILES, "National_4");
			}

			// Structures
			if (data.containsKey(STRUCTURES)) {
				List<String> structures = new ArrayList<>();
				for (Object o : data.getJsonArray(STRUCTURES, new JsonArray())) {
					if (o instanceof JsonObject) {
						JsonObject structure = (JsonObject) o;
						if (structure.containsKey(UAI)) {
							structures.add(structure.getString(UAI));
						}
					}
					else if (o instanceof String) {
						String uai = data.getJsonArray(STRUCTURE_NODES).stream()
								.map(JsonObject.class::cast)
								.filter(structNode -> structNode.getString(EXTERNAL_ID).equals((String) o))
								.findFirst()
								.orElse(null)
								.getString(UAI, null);

						structures.add(uai);
					}
				}
				attributes.put(WG_STRUCTURE_UAI, structures.size() == 1 ? structures.get(0) : structures.toString());
			}

			// Function and sub-function
			if (data.containsKey(FUNCTIONS) && data.containsKey(ID)) {
				fillAttributesFunctions(attributes);
			}

			user.setAttributes(attributes);
		}
		catch (Exception e) {
			log.error("[WebGerestRegisteredService::prepareUserCas20] Failed to transform User for Web Gerest", e);
		}
	}

	private Future<List<String>> getFunctions(String userId) {
		Promise<List<String>> promise = Promise.promise();

		if (userId == null) {
			promise.complete(new ArrayList<>());
			return promise.future();
		}

		String query = "MATCH (u:User) WHERE u.id = {id} RETURN u.functions;";
		Neo4j.getInstance().execute(query, new JsonObject().put(ID, userId), validResultHandler(event -> {
			if (event.isLeft()) {
				promise.fail(event.left().getValue());
				promise.future();
				return;
			}

			List<String> functions = event.right().getValue().stream()
					.filter(Objects::nonNull)
					.map(JsonObject.class::cast)
					.findFirst()
					.get()
					.getJsonArray("u.functions")
					.stream()
					.map(String.class::cast)
					.collect(Collectors.toList());
			promise.complete(functions);
		}));

		return promise.future();
	}

	private void fillAttributesFunctions(Map<String, String> attributes) {
		List<String> rightFunctions = filterRightFunctions();

		if (rightFunctions.isEmpty()) {
			attributes.put(WG_FUNCTIONS, "");
			attributes.put(WG_SUBFUNCTIONS, "");
		}

		JsonArray functions = new JsonArray();
		JsonArray subFunctions = new JsonArray();

		rightFunctions.forEach(function -> {
			int first$pos = function.indexOf(DOLLAR);
			int second$pos = function.indexOf(DOLLAR, first$pos + 1);
			String formatedFunction = function.substring(first$pos + 1, second$pos);
			if (!functions.contains(formatedFunction)) functions.add(formatedFunction);

			int last$pos = function.lastIndexOf(DOLLAR);
			String formatedSubfunction = function.substring(last$pos + 1);
			if (!subFunctions.contains(formatedSubfunction)) subFunctions.add(formatedSubfunction);
		});

		// Functions

		attributes.put(WG_FUNCTIONS, functions.size() == 1 ? functions.getString(0) : functions.toString());
		attributes.put(WG_SUBFUNCTIONS, subFunctions.size() == 1 ? subFunctions.getString(0) : subFunctions.toString());
	}

	private List<String> filterRightFunctions() {
		return getModelFunctions().stream()
				.filter(modelFunction -> neoFunctions.contains(modelFunction.toLowerCase()))
				.collect(Collectors.toList());
	}

	private List<String> getModelFunctions() {
		List<String> functions = new ArrayList<>();
		functions.add("ADE$AGENTS DEPARTEMENTAUX$1$Administrateur WebGerest");
		functions.add("ADE$AGENTS DEPARTEMENTAUX$2$Approvisionneur WebGerest");
		functions.add("ADC$AGENTS DEPARTEMENTAUX DES COLLEGES$3$Chef de cuisine");
		functions.add("ADC$AGENTS DEPARTEMENTAUX DES COLLEGES$7$Cuisinier");
		return functions;
	}

	private void formatNeoFunctions(List<String> functions) {
		neoFunctions = new ArrayList<>();
		for (String function : functions) {
			int first$pos = function.indexOf(DOLLAR);
			neoFunctions.add(function.substring(first$pos + 1).toLowerCase());
		}
	}
}
