/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.directory.controllers;

import java.io.File;
import java.util.*;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;

import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.notification.ConversationNotification;
import org.entcore.common.validation.StringValidation;
import org.entcore.directory.services.SchoolService;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.neo4j.Neo4jResult;

import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.http.HttpClientUtils;

import org.entcore.common.user.UserUtils;
import org.entcore.common.user.UserInfos;

import fr.wseduc.security.SecuredAction;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;
import static org.entcore.common.user.SessionAttributes.*;

public class UserBookController extends BaseController {

	private Neo neo;
	private JsonObject config;
	private JsonObject userBookData;
	private HttpClient client;
	private SchoolService schoolService;
	private EventStore eventStore;
	private ConversationNotification conversationNotification;
	private enum DirectoryEvent { ACCESS }
	private static final String ANNUAIRE_MODULE = "Annuaire";
	private Map<String, Map<String, String>> activationWelcomeMessage;

	@Override
	public void init(final Vertx vertx, Container container, RouteMatcher rm,
					 Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		pathPrefix = "/userbook";
		super.init(vertx, container, rm, securedActions);
		this.neo = new Neo(vertx, Server.getEventBus(vertx),log);
		this.config = container.config();
		userBookData= config.getObject("user-book-data");
		client = vertx.createHttpClient()
						.setHost(config.getString("workspace-url"))
						.setPort(config.getInteger("workspace-port"))
						.setMaxPoolSize(16)
						.setKeepAlive(false);
		getWithRegEx(".*", "proxyDocument");
		eventStore = EventStoreFactory.getFactory().getEventStore(ANNUAIRE_MODULE);
		if (config.getBoolean("activation-welcome-message", false)) {
			activationWelcomeMessage = new HashMap<>();
			String assetsPath = (String) vertx.sharedData().getMap("server").get("assetPath");
			Map<String, String> skins = vertx.sharedData().getMap("skins");
			if (skins != null) {
				activationWelcomeMessage = new HashMap<>();
				for (final Map.Entry<String, String> e: skins.entrySet()) {
					String path = assetsPath + "/assets/themes/" + e.getValue() + "/template/directory/welcome/";
					vertx.fileSystem().readDir(path, new AsyncResultHandler<String[]>() {
						@Override
						public void handle(AsyncResult<String[]> event) {
							if (event.succeeded()) {
								final Map<String, String> messages = new HashMap<>();
								activationWelcomeMessage.put(e.getKey(), messages);
								for (final String file : event.result()) {
									vertx.fileSystem().readFile(file, new Handler<AsyncResult<Buffer>>() {
										@Override
										public void handle(AsyncResult<Buffer> event) {
											if (event.succeeded()) {
												String filename = file.substring(
														file.lastIndexOf(File.separator) + 1, file.lastIndexOf("."));
												messages.put(filename, event.result().toString());
												if (log.isDebugEnabled()) {
													log.debug("Load welcome message " + file + " as " + filename);
												}
											}
										}
									});
								}
							}
						}
					});
				}
			}
		}
	}

	@Get("/mon-compte")
	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void monCompte(HttpServerRequest request) {
		renderView(request);
	}

	@Get("/birthday")
	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void birthday(HttpServerRequest request) {
		renderView(request);
	}

	@Get("/mood")
	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void mood(HttpServerRequest request) {
		renderView(request);
	}

	@Get("/annuaire")
	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void annuaire(HttpServerRequest request) {
		renderView(request);
		eventStore.createAndStoreEvent(DirectoryEvent.ACCESS.name(), request);
	}

	@Get("/api/search")
	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void search(final HttpServerRequest request) {
		String name = request.params().get("name");
		String structure = request.params().get("structure");
		String profile = request.params().get("profile");
		String filter = "";
		JsonObject params = new JsonObject();
		if (name == null || name.trim().isEmpty()) {
			badRequest(request, "empty.name");
			return;
		}
		if(profile != null && !profile.trim().isEmpty()){
			filter += "AND HEAD(m.profiles) = {profile} ";
			params.putString("profile", profile);
		}
		if(structure != null && !structure.trim().isEmpty()){
			filter += "AND (m)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(:Structure {id: {structureId}}) ";
			params.putString("structureId", structure);
		}
		String preFilter = "AND m.displayNameSearchField CONTAINS {search} " + filter;
		params.putString("search", StringValidation.removeAccents(name.trim()).toLowerCase());
		String customReturn =
				"OPTIONAL MATCH visibles-[:USERBOOK]->u " +
				"RETURN distinct visibles.id as id, visibles.displayName as displayName, " +
				"u.mood as mood, u.userid as userId, u.picture as photo, " +
				"HEAD(visibles.profiles) as type " +
				"ORDER BY displayName";
		UserUtils.findVisibleUsers(eb, request, false, false, preFilter, customReturn, params, new Handler<JsonArray>() {
			@Override
			public void handle(JsonArray users) {
				renderJson(request, users);
			}
		});
	}

	@Get("/api/person")
	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void person(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request,new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					String hobbyVisibility;
					String personnalInfos;
					Map<String, Object> params = new HashMap<>();
					if (request.params().get("id") == null) {
						Object person = user.getAttribute(PERSON_ATTRIBUTE);
						if (person != null) {
							renderJson(request, new JsonObject(person.toString()));
							return;
						}
						params.put("userId",user.getUserId());
						hobbyVisibility = "PUBLIC|PRIVE";
						personnalInfos =
								"OPTIONAL MATCH u-[r0:SHOW_EMAIL]->() " +
								"OPTIONAL MATCH u-[r1:SHOW_BIRTHDATE]->() " +
								"OPTIONAL MATCH u-[r2:SHOW_PHONE]->() " +
								"OPTIONAL MATCH u-[r3:SHOW_MAIL]->() " +
								"OPTIONAL MATCH u-[r4:SHOW_HEALTH]->u " +
								"OPTIONAL MATCH u-[r5:SHOW_MOBILE]->() " +
								"WITH DISTINCT h, s, c, n, v, u, n2, p, n.address as address, " +
								"n.email as email, u.health as health, " +
								"n.homePhone as tel, n.birthDate as birthdate, n.mobile as mobile, " +
								"COLLECT(distinct [type(r0),type(r1),type(r2),type(r3),type(r4),type(r5)]) as r ";
					} else {
						params.put("userId",request.params().get("id"));
						hobbyVisibility = "PUBLIC";
						personnalInfos = "OPTIONAL MATCH u-[:SHOW_EMAIL]->e " +
								"OPTIONAL MATCH u-[:SHOW_MAIL]->a " +
								"OPTIONAL MATCH u-[:SHOW_PHONE]->ph " +
								"OPTIONAL MATCH u-[:SHOW_MOBILE]->mo " +
								"OPTIONAL MATCH u-[:SHOW_BIRTHDATE]->b " +
								"OPTIONAL MATCH u-[:SHOW_HEALTH]->st " +
								"WITH h, s, c, n, v, u, n2, p, a.address as address, " +
								"e.email as email, st.health as health, " +
								"ph.homePhone as tel, b.birthDate as birthdate, mo.mobile as mobile, " +
								"COLLECT([]) as r ";
					}
					String query = "MATCH (n:User) "
								+ "WHERE n.id = {userId} "
								+ "OPTIONAL MATCH n-[:IN]->(:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) "
								+ "OPTIONAL MATCH n-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure) "
								+ "OPTIONAL MATCH n-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class)-[:BELONGS]->(s) "
								+ "OPTIONAL MATCH (n)-[:USERBOOK]->(u) "
								+ "OPTIONAL MATCH (u)-[v:" + hobbyVisibility + "]->(h1) "
								+ "OPTIONAL MATCH (n)-[:RELATED]-(n2) "
								+ "WITH DISTINCT h1 as h, s, collect(distinct c.name) as c, n, v, u, n2, p "
								+ personnalInfos
								+ "WITH COLLECT(DISTINCT {name: s.name, classes: c}) as schools, "
								+ "n, u, n2, address, email, health, tel, mobile, birthdate, r,  COLLECT(p.name) as type, "
								+ "COLLECT(DISTINCT {visibility: type(v), category: h.category, values: h.values}) as hobbies "
								+ "RETURN DISTINCT "
									+ "n.id as id,"
									+ "n.login as login, "
									+ "n.displayName as displayName,"
									+ "type,"
									+ "address,"
									+ "email, "
									+ "tel, "
									+ "mobile, "
									+ "birthdate, "
									+ "HEAD(r) as visibleInfos, "
									+ "schools, "
									+ "n2.displayName as relatedName, "
									+ "n2.id as relatedId,"
									+ "n2.type as relatedType,"
									+ "u.userid as userId,"
									+ "u.motto as motto,"
									+ "COALESCE(u.picture, {defaultAvatar}) as photo,"
									+ "COALESCE(u.mood, {defaultMood}) as mood,"
									+ "health,"
									+ "hobbies";
					params.put("defaultAvatar", userBookData.getString("default-avatar"));
					params.put("defaultMood", userBookData.getString("default-mood"));
					neo.send(query, params, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> message) {
							JsonObject r = message.body();
							if (request.params().get("id") == null) {
								UserUtils.addSessionAttribute(eb, user.getUserId(), PERSON_ATTRIBUTE, r.encode(), null);
							}
							renderJson(request, r);
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Get("/structures")
	@SecuredAction(value = "userbook.my.structures", type = ActionType.AUTHENTICATED)
	public void showStructures(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					schoolService.listByUserId(user.getUserId(), arrayResponseHandler(request));
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Get("/structure/:structId")
	@SecuredAction(value = "userbook.structure.classes.personnel", type = ActionType.AUTHENTICATED)
	public void showStructure(final HttpServerRequest request) {
		String structureId = request.params().get("structId");
		String customReturn =
				"MATCH (s:Structure { id : {structId}})<-[:DEPENDS]-(pg:ProfileGroup)" +
				"-[:HAS_PROFILE]->(p:Profile {name : 'Personnel'}), visibles-[:IN]->pg " +
				"OPTIONAL MATCH visibles-[:USERBOOK]->(u:UserBook) " +
				"RETURN DISTINCT p.name as type, visibles.id as id, " +
				"visibles.displayName as displayName, u.mood as mood, " +
				"u.picture as photo " +
				"ORDER BY type DESC, displayName ";
		final JsonObject params = new JsonObject().putString("structId", structureId);
		UserUtils.findVisibleUsers(eb, request, true, customReturn, params, new Handler<JsonArray>() {

			@Override
			public void handle(final JsonArray personnel) {
				String customReturn =
						"MATCH profileGroup-[:DEPENDS]->(c:Class)-[:BELONGS]->(s:Structure { id : {structId}}) " +
						"RETURN collect(distinct {id: c.id, name: c.name, level: c.level}) as classes, " +
						"collect(distinct {id: profileGroup.id, name: profileGroup.name, groupDisplayName: profileGroup.groupDisplayName }) as profileGroups";
				UserUtils.findVisibleProfilsGroups(eb, request, customReturn, params, new Handler<JsonArray>() {
					@Override
					public void handle(final JsonArray classesAndProfileGroups) {
						String customReturn =
								"MATCH manualGroup-[:DEPENDS]->(c)-[:BELONGS*0..1]->(s:Structure { id : {structId}}) " +
								"WHERE ALL(label IN labels(c) WHERE label IN [\"Structure\", \"Class\"]) " +
								"RETURN DISTINCT manualGroup.id as id, manualGroup.name as name, manualGroup.groupDisplayName as groupDisplayName " +
								"ORDER BY name ASC ";
						UserUtils.findVisibleManualGroups(eb, request, customReturn, params, new Handler<JsonArray>() {
							@Override
							public void handle(JsonArray manualGroups) {
								JsonObject result = new JsonObject()
									.putArray("users", personnel)
									.putArray("classes", ((JsonObject) classesAndProfileGroups.get(0)).getArray("classes", new JsonArray()))
									.putArray("profileGroups", ((JsonObject) classesAndProfileGroups.get(0)).getArray("profileGroups", new JsonArray()))
									.putArray("manualGroups", manualGroups);
								renderJson(request, result);
							}
						});

					}
				});
			}
		});
	}

	@Get("/visible/users/:groupId")
	@SecuredAction(value = "userbook.visible.users.group", type = ActionType.AUTHENTICATED)
	public void visibleUsersGroup(final HttpServerRequest request) {
		String groupId = request.params().get("groupId");
		if (groupId == null || groupId.trim().isEmpty()) {
			badRequest(request, "invalid.groupId");
			return;
		}
		String customReturn =
				"MATCH (s:Group { id : {groupId}})<-[:IN]-(visibles) " +
				"RETURN DISTINCT HEAD(visibles.profiles) as type, visibles.id as id, " +
				"visibles.displayName as displayName, visibles.login as login " +
				"ORDER BY type DESC, displayName ";
		final JsonObject params = new JsonObject().putString("groupId", groupId);
		UserUtils.findVisibleUsers(eb, request, true, false, customReturn, params, new Handler<JsonArray>() {

			@Override
			public void handle(final JsonArray users) {
				renderJson(request, users);
			}
		});
	}

	@Get("/api/class")
	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void myClass(final HttpServerRequest request) {
		String classId = request.params().get("id");
		String matchClass;
		JsonObject params = new JsonObject();
		if (classId == null || classId.trim().isEmpty()) {
			matchClass = "(n:User {id : {userId}})-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(c:Class) " +
					"WITH c, profile, visibles MATCH";
		} else {
			matchClass = "(c:Class {id : {classId}}),";
			params.putString("classId", classId);
		}
		String query =
				"MATCH " + matchClass + " visibles-[:IN]->(:ProfileGroup)-[:DEPENDS]->c " +
				"WHERE profile.name IN ['Student', 'Teacher'] " +
				"OPTIONAL MATCH visibles-[:USERBOOK]->u " +
				"RETURN distinct profile.name as type, visibles.id as id, " +
				"visibles.displayName as displayName, u.mood as mood, " +
				"u.userid as userId, u.picture as photo " +
				"ORDER BY type DESC, displayName ";
		UserUtils.findVisibleUsers(eb, request, true, true, query, params, new Handler<JsonArray>() {
			@Override
			public void handle(JsonArray users) {
				renderJson(request, users);
			}
		});
	}

	@Get("/api/edit-userbook-info")
	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void editUserBookInfo(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request,new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					String prop = request.params().get("prop");
					if ("theme".equals(prop) || "userPreferencesBirthdayClass".equals(prop)) {
						String attr = prop.replaceAll("\\W+", "");
						String neoRequest =
								"MATCH (n:User)-[:USERBOOK]->(m:UserBook)" +
								"WHERE n.id = {id} SET m." + attr + "={value}";
						Map<String, Object> params = new HashMap<>();
						params.put("id", user.getUserId());
						params.put("value", request.params().get("value"));
						neo.send(neoRequest, params, new Handler<Message<JsonObject>>() {

							@Override
							public void handle(Message<JsonObject> res) {
								renderJson(request, res.body());
							}
						});
						UserUtils.removeSessionAttribute(eb, user.getUserId(), THEME_ATTRIBUTE + getHost(request), null);
					} else {
						badRequest(request);
					}
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Get("/api/set-visibility")
	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void setVisibility(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request,new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				Map<String, Object> params = new HashMap<>();
				params.put("id", user.getUserId());
				params.put("category", request.params().get("category"));
				String visibility = "PUBLIC".equals(request.params().get("value")) ? "PUBLIC" : "PRIVE";
				UserUtils.removeSessionAttribute(eb, user.getUserId(), PERSON_ATTRIBUTE, null);
				neo.send("MATCH (n:User)-[:USERBOOK]->(m)-[s]->(p) "
					+ "WHERE n.id = {id} AND p.category={category} "
					+ "DELETE s CREATE (m)-[j:"+ visibility +"]->(p) "
					+ "RETURN n,m,j,p", params, request.response());
			}
		});
	}

	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void proxyDocument(final HttpServerRequest request) {
		String defaultImg = request.params().get("userbook-dimg");
		JsonObject defaultContent = null;
		log.debug(defaultImg);
		if (defaultImg != null) {
			defaultContent = new JsonObject()
			.putString("type", "file")
			.putString("content", defaultImg);
		}
		HttpClientUtils.proxy(request, client, "\\" + pathPrefix,
				config.getString("workspace-prefix"), defaultContent);
	}

	@SecuredAction("userbook.show.motto.mood")
	public void userBookMottoMood(){}

	@BusAddress("activation.ack")
	public void initUserBookNode(final Message<JsonObject> message){
		JsonObject params = new JsonObject();
		params.putString("userId", message.body().getString("userId"));
		params.putString("avatar", userBookData.getString("default-avatar"));
		params.putString("theme", userBookData.getString("default-theme", ""));
		JsonArray queries = new JsonArray();
		String query =
				"MERGE (m:UserBook { userid : {userId}}) " +
				"SET m.type = 'USERBOOK', m.picture = {avatar}, m.motto = '', " +
				"m.health = '', m.mood = 'default', m.theme =  {theme} " +
				"WITH m "+
				"MATCH (n:User {id : {userId}}) " +
				"CREATE UNIQUE n-[:USERBOOK]->m";
		queries.add(Neo.toJsonObject(query, params));
		String query2 = "MATCH (n:User)-[:USERBOOK]->m "
				+ "WHERE n.id = {userId} "
				+ "CREATE m-[:PUBLIC]->(c:Hobby {category: {category}, values: {values}})";
		for (Object hobby : userBookData.getArray("hobbies")) {
			JsonObject j = params.copy();
			j.putString("category", (String)hobby);
			j.putString("values", "");
			queries.add(Neo.toJsonObject(query2, j));
		}
		neo.sendBatch(queries, (Handler<Message<JsonObject>>) null);

		welcomeMessage(message);
	}

	@BusAddress("send.welcome.message")
	public void welcomeMessage(Message<JsonObject> message) {
		if (activationWelcomeMessage != null) {
			final HttpServerRequest request = new JsonHttpServerRequest(message.body().getObject("request"));
			Map<String, String> messages = activationWelcomeMessage.get(getHost(request));
			if (messages != null) {
				String welcomeMessage = messages.get(message.body().getString("profile"));
				if (welcomeMessage == null) {
					welcomeMessage = messages.get("default");
				}
				if (welcomeMessage != null) {
					conversationNotification.notify(request, "", new JsonArray().add(message.body().getString("userId")),
							null, I18n.getInstance().translate("welcome.subject", getHost(request), I18n.acceptLanguage(request)),
							welcomeMessage, new Handler<Either<String, JsonObject>>() {

								@Override
								public void handle(Either<String, JsonObject> r) {
									if (r.isLeft()) {
										log.error(r.left().getValue());
									}
								}
							});
				}
			}
		}
	}

	@BusAddress("userbook.preferences")
	public void getUserPreferences(final Message<JsonObject> message){
		final HttpServerRequest request = new JsonHttpServerRequest(message.body().getObject("request"));
		final String application = message.body().getString("application");
		final String action = message.body().getString("action");

		if (action == null) {
			log.warn("[@BusAddress](userbook.preferences) Invalid action.");
			message.reply(new JsonObject().putString("status", "error")
					.putString("message", "Invalid action."));
			return;
		}

		switch(action){
			case "get.currentuser":
				UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
					public void handle(UserInfos user) {
						getUserPrefs(user, request, application, new Handler<Either<String, JsonObject>>() {
							public void handle(Either<String, JsonObject> result) {
								if(result.isLeft()){
									message.reply(new JsonObject()
										.putString("status", "error")
										.putString("message", result.left().getValue()));
								} else {
									message.reply(new JsonObject()
										.putString("status", "ok")
										.putObject("value", result.right().getValue()));
								}
							}
						});
					}
				});
				break;
			case "get.userlist":
				final JsonArray userIds = message.body().getArray("userIds", new JsonArray());
				String query =
						"MATCH (u:User) " +
						message.body().getString("additionalMatch", "") +
						"WHERE u.id IN {userIds} AND u.activationCode IS NULL " +
						message.body().getString("additionalWhere", "") +
						"OPTIONAL MATCH (u)-[:PREFERS]->(uac:UserAppConf)  " +
						"RETURN COLLECT(DISTINCT {userId: u.id, userMail: u.email, lastDomain: u.lastDomain, preferences: uac"+
						message.body().getString("additionalCollectFields", "") +
						"}) AS preferences";
				neo.execute(query,
					new JsonObject().putArray("userIds", userIds),
					Neo4jResult.validResultHandler(new Handler<Either<String,JsonArray>>() {
						public void handle(Either<String, JsonArray> event) {
							if(event.isLeft()){
								message.reply(new JsonObject().putString("status", "error")
									.putString("message", event.left().getValue()));
								return;
							}
							JsonArray results = ((JsonObject) event.right().getValue().get(0)).getArray("preferences", new JsonArray());
							for(Object resultObj : results){
								JsonObject result = (JsonObject) resultObj;
								JsonObject prefs = new JsonObject();
								try {
									prefs = new JsonObject(result.getObject("preferences", new JsonObject())
											.getObject("data", new JsonObject()).getString(application, "{}"));
								} catch(Exception e) {
									log.error("UserId [" + result.getString("userId", "") + "] - Bad application preferences format");
								}
								result.putObject("preferences", prefs);
							}
							message.reply(new JsonObject().putString("status", "ok")
								.putArray("results", results));
						}
					}));
				break;
			default:
				message.reply(new JsonObject().putString("status", "error")
						.putString("message", "Invalid action."));
				break;
		}


	}

	@Get("/avatar/:id")
	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void getAvatar(final HttpServerRequest request) {
		String id = request.params().get("id");
		final String assetsPath = (String) vertx.sharedData().getMap("server").get("assetPath") +
				"/assets/themes/" + vertx.sharedData().getMap("skins").get(getHost(request));
		final String defaultAvatarPath = assetsPath + "/img/illustrations/no-avatar.svg";

		if (id != null && !id.trim().isEmpty()) {
			String query =
					"MATCH (n:User)-[:USERBOOK]->(u:UserBook) " +
					"WHERE n.id = {id} " +
					"RETURN distinct u.picture as photo";
			Map<String, Object> params = new HashMap<>();
			params.put("id", id);
			neo.send(query, params, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					if ("ok".equals(event.body().getString("status"))) {
						String photo = event.body().getObject("result", new JsonObject())
								.getObject("0", new JsonObject()).getString("photo");
						if (StringValidation.isAbsoluteDocumentUri(photo)) {
							redirectPermanent(request, photo + "?" + request.query());
							return;
						}
					}
					request.response().sendFile(defaultAvatarPath);
				}
			});
		} else {
			request.response().sendFile(defaultAvatarPath);
		}
	}

	@Get("/person/birthday")
	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void personBirthday(final HttpServerRequest request) {
		Calendar c = Calendar.getInstance();
		int month = c.get(Calendar.MONTH);
		String [] monthRegex = {"12|01|02", "01|02|03", "02|03|04", "03|04|05", "04|05|06", "05|06|07",
				"06|07|08", "07|08|09", "08|09|10", "09|10|11", "10|11|12", "11|12|01"};
		String query =
				"MATCH (u:User {id:{userId}})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class) " +
				"WITH DISTINCT c, profile, visibles " +
				"MATCH visibles-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c) " +
				"WHERE profile.name = 'Student' AND visibles.birthDate=~{regex} " +
				"RETURN distinct visibles.id as id, visibles.displayName as username, " +
				"visibles.birthDate as birthDate, COLLECT(distinct [c.id, c.name]) as classes ";
		JsonObject params = new JsonObject();
		params.putString("regex", "^[0-9]{4}-(" + monthRegex[month] + ")-(3[01]|[12][0-9]|0[1-9])$");
		UserUtils.findVisibleUsers(eb, request, true, true, query, params, new Handler<JsonArray>() {
			@Override
			public void handle(JsonArray users) {
				renderJson(request, users);
			}
		});
	}

	@Get("/api/edit-user-info-visibility")
	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void editUserInfoVisibility(final HttpServerRequest request) {
		final List<String> infos = Arrays.asList("email", "mail", "phone", "mobile", "birthdate", "health");
		final String info = request.params().get("info");
		if (info == null || !infos.contains(info)) {
			badRequest(request);
			return;
		}
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					Map<String, Object> params = new HashMap<>();
					params.put("id", user.getUserId());
					String relationship = "SHOW_" + info.toUpperCase();
					String query = "";
					if ("public".equals(request.params().get("state"))) {
						query += "MATCH (n:User)-[:USERBOOK]->u WHERE n.id = {id} ";
						if ("health".equals(info)) {
							query += "CREATE u-[r:" + relationship + "]->u ";
						} else {
							query += "CREATE u-[r:" + relationship + "]->n ";
						}
					} else {
						query += "MATCH (n:User)-[:USERBOOK]->u-[r:" + relationship + "]->() " +
								 "WHERE n.id = {id} " +
								 "DELETE r";
					}
					UserUtils.removeSessionAttribute(eb, user.getUserId(), PERSON_ATTRIBUTE, null);
					neo.send(query, params, request.response());
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Get("/user-preferences")
	@SecuredAction(value = "userbook.preferences", type = ActionType.AUTHENTICATED)
	public void userPreferences(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					Map<String, Object> params = new HashMap<>();
					params.put("id", user.getUserId());
					String query =
							"MATCH (n:User)-[:USERBOOK]->u " +
							"WHERE n.id = {id} " +
							"RETURN u.userPreferencesBirthdayClass as userPreferencesBirthdayClass";
					neo.send(query, params, request.response());
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Get("/preference/:application")
	@SecuredAction(value = "user.preference", type = ActionType.AUTHENTICATED)
	public void getPreference(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					final String application = request.params().get("application").replaceAll("\\W+", "");
					getUserPrefs(user, request, application, new  Handler<Either<String, JsonObject>>(){
						public void handle(Either<String, JsonObject> event) {
							if(event.isLeft()){
								badRequest(request, event.left().getValue());
							} else {
								renderJson(request, event.right().getValue());
							}
						}
					});
				} else {
					badRequest(request);
				}
			}
		});
	}

	private void getUserPrefs(final UserInfos user, final HttpServerRequest request, final String application, final Handler<Either<String, JsonObject>> handler){
		if (user != null) {
			UserUtils.getSession(eb, request, new Handler<JsonObject>() {
				public void handle(JsonObject session) {
					final JsonObject cache = session.getObject("cache");

					if(cache.containsField("preferences")){
						handler.handle(new Either.Right<String, JsonObject>(
								new JsonObject().putString("preference", cache.getObject("preferences").getString(application))));
					} else {
						refreshPreferences(user, request, new Handler<Either<String, JsonObject>>(){
							public void handle(Either<String, JsonObject> event) {
								if(event.isLeft()) {
									log.error(event.left().getValue());
									handler.handle(new Either.Left<String, JsonObject>("refresh.preferences.failed"));
								} else {
									handler.handle(new Either.Right<String, JsonObject>(
										new JsonObject().putString("preference", event.right().getValue().getString(application))));
								}
							}
						});
					}
				}
			});
		} else {
			handler.handle(new Either.Left<String, JsonObject>("bad.user"));
		}
	}

	private void refreshPreferences(final UserInfos user, final HttpServerRequest request, final Handler<Either<String, JsonObject>> handler){
		String query =
				"MATCH (u:User {id:{userId}})-[:PREFERS]->(uac:UserAppConf)"
						+" RETURN uac AS preferences";

		neo.execute(query,
			new JsonObject().putString("userId", user.getUserId()),
			Neo4jResult.fullNodeMergeHandler("preferences", new Handler<Either<String, JsonObject>>() {
				@Override
				public void handle(final Either<String, JsonObject> result) {
					if (result.isRight()) {
						UserUtils.addSessionAttribute(eb, user.getUserId(), "preferences", result.right().getValue(), new Handler<Boolean>() {
							public void handle(Boolean event) {
								if(Boolean.FALSE.equals(event)) {
									log.error("Could not add preferences attribute to session.");
								}
							}
						});
					}
					handler.handle(result);
				}
		}));
	}

	@Put("/preference/:application")
	@SecuredAction(value = "user.preference", type = ActionType.AUTHENTICATED)
	public void updatePreference(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>(){
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					final JsonObject params = new JsonObject().putString("userId", user.getUserId());
					final String application = request.params().get("application").replaceAll("\\W+", "");
					request.bodyHandler(new Handler<Buffer>() {
						@Override
						public void handle(Buffer body) {
							params.putString("conf", body.toString("UTF-8"));
							String query =
									"MATCH (u:User {id:{userId}})"
											+"MERGE (u)-[:PREFERS]->(uac:UserAppConf)"
											+" ON CREATE SET uac."+ application +" = {conf}"
											+" ON MATCH SET uac."+ application +" = {conf}";
							neo.execute(query, params, validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
								@Override
								public void handle(Either<String, JsonObject> result) {
									if (result.isRight()) {
										renderJson(request, result.right().getValue());

										UserUtils.getSession(eb, request, new Handler<JsonObject>() {
											public void handle(JsonObject session) {
												final JsonObject cache = session.getObject("cache");

												if(cache.containsField("preferences")){
													JsonObject prefs = cache.getObject("preferences");
													prefs.putString(application, params.getString("conf"));

													UserUtils.addSessionAttribute(eb, user.getUserId(), "preferences", prefs, new Handler<Boolean>() {
														public void handle(Boolean event) {
															if(!event)
																log.error("Could not add preferences attribute to session.");
														}
													});
												}
											}
										});
									} else {
										leftToResponse(request,result.left());
									}
								}
							}));
						}
					});
				} else {
					badRequest(request);
				}
			}
		});
	}


	public void setSchoolService(SchoolService schoolService) {
		this.schoolService = schoolService;
	}

	public void setConversationNotification(ConversationNotification conversationNotification) {
		this.conversationNotification = conversationNotification;
	}

}
