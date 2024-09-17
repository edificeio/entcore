/* Copyright © "Open Digital Education", 2014
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

 *
 */

package org.entcore.directory.controllers;

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;
import static org.entcore.common.user.SessionAttributes.PERSON_ATTRIBUTE;
import static org.entcore.common.user.SessionAttributes.BIRTHDAYS_ATTRIBUTE;
import static org.entcore.common.user.SessionAttributes.THEME_ATTRIBUTE;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.CookieHelper;
import io.vertx.core.shareddata.LocalMap;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.http.response.DefaultResponseHandler;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.notification.ConversationNotification;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.validation.StringValidation;
import org.entcore.directory.services.SchoolService;
import org.entcore.directory.services.UserBookService;
import org.entcore.common.user.position.UserPositionService;
import org.entcore.common.user.position.UserPosition;
import org.vertx.java.core.http.RouteMatcher;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.HttpClientUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class UserBookController extends BaseController {

	private Neo neo;
	private JsonObject config;
	private JsonObject userBookData;
	private HttpClient client;
	private SchoolService schoolService;
	private UserBookService userBookService;
	private UserPositionService userPositionService;
	private EventStore eventStore;
	private ConversationNotification conversationNotification;
	protected enum DirectoryEvent { ACCESS }
	protected static final String ANNUAIRE_MODULE = "Annuaire";
	private Map<String, Map<String, String>> activationWelcomeMessage;

	public void setUserBookService(UserBookService userBookService) {
		this.userBookService = userBookService;
	}
	
	public void setUserPositionService(UserPositionService userPositionService) {
		this.userPositionService = userPositionService;
	}

	@Override
	public void init(final Vertx vertx, JsonObject config, RouteMatcher rm,
					 Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		pathPrefix = "/userbook";
		super.init(vertx, config, rm, securedActions);
		this.neo = new Neo(vertx, Server.getEventBus(vertx),log);
		this.config = config;
		userBookData= config.getJsonObject("user-book-data");
		final HttpClientOptions options = new HttpClientOptions()
				.setDefaultHost(config.getString("workspace-url"))
				.setDefaultPort(config.getInteger("workspace-port"))
				.setMaxPoolSize(16)
				.setKeepAlive(false);
		client = vertx.createHttpClient(options);
		getWithRegEx(".*", "proxyDocument");
		eventStore = EventStoreFactory.getFactory().getEventStore(ANNUAIRE_MODULE);
		if (config.getBoolean("activation-welcome-message", false)) {
			activationWelcomeMessage = new HashMap<>();
			String assetsPath = (String) vertx.sharedData().getLocalMap("server").get("assetPath");
			Map<String, String> skins = vertx.sharedData().getLocalMap("skins");
			if (skins != null) {
				activationWelcomeMessage = new HashMap<>();
				for (final Map.Entry<String, String> e: skins.entrySet()) {
					String path = assetsPath + "/assets/themes/" + e.getValue() + "/template/directory/welcome/";
					vertx.fileSystem().readDir(path, new Handler<AsyncResult<List<String>>>() {
						@Override
						public void handle(AsyncResult<List<String>> event) {
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
		LocalMap<Object, Object> serverMap = vertx.sharedData().getLocalMap("server");
		JsonObject configuration = new JsonObject().put("hidePersonalData", serverMap.get("hidePersonalData"));
		renderView(request, configuration, "mon-compte.html", null);
		eventStore.createAndStoreEvent(DirectoryEvent.ACCESS.name(), request, new JsonObject().put("override-module", "MyAccount"));
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
			params.put("profile", profile);
		}
		if(structure != null && !structure.trim().isEmpty()){
			filter += "AND (m)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(:Structure {id: {structureId}}) ";
			params.put("structureId", structure);
		}
		String preFilter = "AND m.displayNameSearchField CONTAINS {search} " + filter;
		params.put("search", StringValidation.sanitize(name));
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
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null) {
				final String userId = request.params().get("id");
				final boolean forceReload = "true".equals(request.params().get("force"));
				if (userId == null) {
					userBookService.getCurrentUserInfos(user, forceReload, defaultResponseHandler(request));
				}else{
					userBookService.getPersonInfos(userId, defaultResponseHandler(request));
				}
			} else {
				unauthorized(request);
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
		final JsonObject params = new JsonObject().put("structId", structureId);
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
									.put("users", personnel)
									.put("classes", classesAndProfileGroups.getJsonObject(0).getJsonArray("classes", new fr.wseduc.webutils.collections.JsonArray()))
									.put("profileGroups", classesAndProfileGroups.getJsonObject(0).getJsonArray("profileGroups", new fr.wseduc.webutils.collections.JsonArray()))
									.put("manualGroups", manualGroups);
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
		final JsonObject params = new JsonObject().put("groupId", groupId);
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
		UserUtils.getAuthenticatedUserInfos(eb, request)
				.onSuccess(userInfos -> {
					String queryVisibleUsers = "RETURN DISTINCT visibles.id as user ";
					UserUtils.findVisibleUsers(eb, request, true, true, queryVisibleUsers, new JsonObject(), visibles -> {
						String matchClass;
						JsonObject params = new JsonObject();
						if (classId == null || classId.trim().isEmpty()) {
							matchClass = "(n:User {id : {userId}})-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(c:Class) ";
							params.put("userId", userInfos.getUserId());
						} else {
							matchClass = "(c:Class {id : {classId}}) ";
							params.put("classId", classId);
						}

						List<String> ids = visibles
								.stream()
								.map(v -> ((JsonObject)v).getString("user"))
								.collect(Collectors.toList());
						params.put("visibles", ids);

						String queryClassUsers = "MATCH " + matchClass +
								"WITH c " +
								"MATCH c<-[:DEPENDS]-(cpg:ProfileGroup)<-[:IN]-(m:User) " +
								"WHERE head(m.profiles) IN ['Student','Teacher'] " +
								"OPTIONAL MATCH m-[:USERBOOK]->u " +
								"RETURN distinct head(m.profiles) as type, m.id as id, " +
								"m.displayName as displayName, u.mood as mood, " +
								"u.userid as userId, u.picture as photo, (m.id IN {visibles}) as isVisible " +
								"ORDER BY type DESC, displayName ";

						Neo4j.getInstance().execute(
								queryClassUsers,
								params,
								Neo4jResult.validResultHandler(
										DefaultResponseHandler.arrayResponseHandler(request)
								)
						);
					});
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
					if (prop != null && (prop.startsWith("theme") || "userPreferencesBirthdayClass".equals(prop))) {
						final boolean isTheme = prop.startsWith("theme");
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
								if(isTheme){
									CookieHelper.set("themeVersion", System.currentTimeMillis()+"", request);
								}
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
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null) {
				final String category = request.params().get("category");
				if (!UserBookService.ALLOWED_HOBBIES.contains(category)) {
					badRequest(request);
					return;
				}
				final String visibility = request.params().get("value");
				userBookService.setHobbyVisibility(user, category, visibility, defaultResponseHandler(request));
			} else {
				unauthorized(request);
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
			.put("type", "file")
			.put("content", defaultImg);
		}
		HttpClientUtils.proxy(request, client, "\\" + pathPrefix,
				config.getString("workspace-prefix"), defaultContent);
	}

	@SecuredAction("userbook.show.motto.mood")
	public void userBookMottoMood(){}

	@SecuredAction("userbook.switch.theme")
	public void userBookSwitchTheme(){}

	@BusAddress("activation.ack")
	public void initUserBookNode(final Message<JsonObject> message){
		//prepare params
		final String userId = message.body().getString("userId");
		final String theme = message.body().getString("theme");
		final JsonObject headers = message.body().getJsonObject("request", new JsonObject()).getJsonObject("headers", new JsonObject());
		final String defaultLanguage = userBookData.getString("default-language", "fr");
		final JsonArray languages = I18n.getInstance().getLanguages(headers.getString("Host"));
		final String lang = headers.getString("Accept-Language", defaultLanguage);
		final String locale = Locale.forLanguageTag(lang.split(",")[0].split("-")[0]).toString();
		final String localeAvailable = (languages.contains(locale)) ? locale : defaultLanguage;
		final JsonObject uacLanguage = new JsonObject().put(I18n.DEFAULT_DOMAIN, localeAvailable);
		//init userbook
		userBookService.initUserbook(userId, theme, uacLanguage);
		welcomeMessage(message);
	}

	@BusAddress("send.welcome.message")
	public void welcomeMessage(Message<JsonObject> message) {
		if (activationWelcomeMessage != null) {
			final HttpServerRequest request = new JsonHttpServerRequest(message.body().getJsonObject("request", new JsonObject()));
			Map<String, String> messages = activationWelcomeMessage.get(getHost(request));
			if (messages != null) {
				String welcomeMessage = messages.get(message.body().getString("profile"));
				if (welcomeMessage == null) {
					welcomeMessage = messages.get("default");
				}
				if (welcomeMessage != null) {
					conversationNotification.notify(request, "", new fr.wseduc.webutils.collections.JsonArray().add(message.body().getString("userId")),
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
		final HttpServerRequest request = new JsonHttpServerRequest(message.body().getJsonObject("request"));
		final String application = message.body().getString("application");
		final String action = message.body().getString("action");

		if (action == null) {
			log.warn("[@BusAddress](userbook.preferences) Invalid action.");
			message.reply(new JsonObject().put("status", "error")
					.put("message", "Invalid action."));
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
										.put("status", "error")
										.put("message", result.left().getValue()));
								} else {
									message.reply(new JsonObject()
										.put("status", "ok")
										.put("value", result.right().getValue()));
								}
							}
						});
					}
				});
				break;
			case "get.userlist":
				final JsonArray userIds = message.body().getJsonArray("userIds", new fr.wseduc.webutils.collections.JsonArray());
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
					new JsonObject().put("userIds", userIds),
					Neo4jResult.validResultHandler(new Handler<Either<String,JsonArray>>() {
						public void handle(Either<String, JsonArray> event) {
							if(event.isLeft()){
								message.reply(new JsonObject().put("status", "error")
									.put("message", event.left().getValue()));
								return;
							}
							JsonArray results = (event.right().getValue().getJsonObject(0)).getJsonArray("preferences", new fr.wseduc.webutils.collections.JsonArray());
							for(Object resultObj : results){
								JsonObject result = (JsonObject) resultObj;
								JsonObject prefs = new JsonObject();
								try {
									prefs = new JsonObject(getOrElse(getOrElse(getOrElse(result.getJsonObject("preferences"), new JsonObject(), false)
											.getJsonObject("data"), new JsonObject(), false).getString(application), "{}", false));
								} catch(Exception e) {
									log.error("UserId [" + result.getString("userId", "") + "] - Bad application preferences format");
								}
								result.put("preferences", prefs);
							}
							message.reply(new JsonObject().put("status", "ok")
								.put("results", results));
						}
					}));
				break;
			default:
				message.reply(new JsonObject().put("status", "error")
						.put("message", "Invalid action."));
				break;
		}


	}

	@Get("/avatar/:id")
	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void getAvatar(final HttpServerRequest request) {
		final String id = request.params().get("id"); 
		String thumbnail = request.params().get("thumbnail");
		String defAVatar = userBookData.getString("default-avatar");
		this.userBookService.getAvatar(id, Optional.ofNullable(thumbnail), defAVatar, request);
	}

	@Get("/person/birthday")
	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void personBirthday(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request,new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					Object birthdays = user.getAttribute(BIRTHDAYS_ATTRIBUTE);
					if (birthdays != null) {
						renderJson(request, new JsonArray(birthdays.toString()));
						return;
					}

					Calendar c = Calendar.getInstance();
					int month = c.get(Calendar.MONTH);
					String[] monthRegex = {"01", "02", "03", "04", "05", "06",
							"07", "08", "09", "10", "11", "12"};

					final String preFilter = "AND HEAD(m.profiles) = 'Student' AND m.birthDate=~{regex} ";

					String query =
							"MATCH (u:User {id:{userId}})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class) " +
									"WITH DISTINCT c, profile, visibles " +
									"MATCH visibles-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c) " +
									"RETURN distinct visibles.id as id, visibles.displayName as username, " +
									"visibles.birthDate as birthDate, COLLECT(distinct [c.id, c.name]) as classes ";
					JsonObject params = new JsonObject();
					params.put("regex", "^[0-9]{4}-" + monthRegex[month] + "-(3[01]|[12][0-9]|0[1-9])$");
					UserUtils.findVisibleUsers(eb, request, true, true, preFilter, query, params, new Handler<JsonArray>() {
						@Override
						public void handle(JsonArray users) {
							UserUtils.addSessionAttribute(eb, user.getUserId(), BIRTHDAYS_ATTRIBUTE, users.encode(), null);
							renderJson(request, users);
						}
					});
				} else {
					unauthorized(request);
				}
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
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null) {
				final String state = request.params().get("state");
				userBookService.setInfosVisibility(user, state, info, defaultResponseHandler(request));
			} else {
				unauthorized(request);
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
					final JsonObject cache = session.getJsonObject("cache");

					if(cache.containsKey("preferences")){
						handler.handle(new Either.Right<String, JsonObject>(
								new JsonObject().put("preference", cache.getJsonObject("preferences").getString(application))));
					} else {
						refreshPreferences(user, request, new Handler<Either<String, JsonObject>>(){
							public void handle(Either<String, JsonObject> event) {
								if(event.isLeft()) {
									log.error(event.left().getValue());
									handler.handle(new Either.Left<String, JsonObject>("refresh.preferences.failed"));
								} else {
									handler.handle(new Either.Right<String, JsonObject>(
										new JsonObject().put("preference", event.right().getValue().getString(application))));
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
			new JsonObject().put("userId", user.getUserId()),
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
					final JsonObject params = new JsonObject().put("userId", user.getUserId());
					final String application = request.params().get("application").replaceAll("\\W+", "");
					request.bodyHandler(new Handler<Buffer>() {
						@Override
						public void handle(Buffer body) {
							params.put("conf", body.toString("UTF-8"));
							String query =
									"MATCH (u:User {id:{userId}})"
											+"MERGE (u)-[:PREFERS]->(uac:UserAppConf)"
											+" ON CREATE SET uac."+ application +" = {conf}"
											+" ON MATCH SET uac."+ application +" = {conf}";
							neo.execute(query, params, validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
								@Override
								public void handle(Either<String, JsonObject> result) {
									if (result.isRight()) {
										if("language".equals(application)){
											CookieHelper.set("langVersion", System.currentTimeMillis()+"", request);
										}
										if("theme".equals(application)){
											CookieHelper.set("themeVersion", System.currentTimeMillis()+"", request);
										}
										renderJson(request, result.right().getValue());

										UserUtils.getSession(eb, request, new Handler<JsonObject>() {
											public void handle(JsonObject session) {
												final JsonObject cache = session.getJsonObject("cache");

												if(cache.containsKey("preferences")){
													JsonObject prefs = cache.getJsonObject("preferences");
													prefs.put(application, params.getString("conf"));
													if ("theme".equals(application)) {
														prefs.remove(THEME_ATTRIBUTE + getHost(request));
													}
													UserUtils.addSessionAttribute(eb, user.getUserId(), "preferences", prefs, new Handler<Boolean>() {
														public void handle(Boolean event) {
															UserUtils.removeSessionAttribute(eb,  user.getUserId(), THEME_ATTRIBUTE + getHost(request), null);
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

	@Get("/search/criteria")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void searchCriteria(HttpServerRequest request) {
		final boolean getClassesMonoStructureOnly = Boolean.parseBoolean(request.params().get("getClassesMonoStructureOnly"));
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null) {
				userPositionService.getUserPositions(user)
				.onComplete( ar -> {
					Set<UserPosition> positions;
					if(ar.failed()) {
						log.debug("Unable to get UserPositions, considering it empty.");
						positions = Collections.emptySet();
					} else {
						positions = ar.result();
					}

					schoolService.searchCriteria(
						user.getStructures(), 
						getClassesMonoStructureOnly, 
						(Either<String, JsonObject> either) -> {
							if (either.isRight()) {
								JsonObject result = either.right().getValue();
								if(result != null) {
									// Add UserPositions to the resulting JsonObject
									final JsonArray positionsArray = new JsonArray();
									positions.forEach(position -> positionsArray.add(position.toJsonObject()));
									result.put("positions", positionsArray);
								}
							}
							defaultResponseHandler(request).handle(either);
						}
					);
			 	});
			} else {
				badRequest(request, "invalid.user");
			}
		});
	}

	@Get("/search/criteria/:structureId/classes")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void getClasses(HttpServerRequest request) {
		String structureId = request.getParam("structureId");
		if (structureId != null) {
			schoolService.getClasses(structureId, defaultResponseHandler(request));
		} else {
			badRequest(request, "structureId is empty");
			return;
		}
	}

	public void setSchoolService(SchoolService schoolService) {
		this.schoolService = schoolService;
	}

	public void setConversationNotification(ConversationNotification conversationNotification) {
		this.conversationNotification = conversationNotification;
	}

}
