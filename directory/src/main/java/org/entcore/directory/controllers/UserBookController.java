/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.directory.controllers;

import java.util.*;
import java.util.regex.Pattern;

import fr.wseduc.security.ActionType;
import fr.wseduc.webutils.Either;
import org.entcore.common.validation.StringValidation;
import org.entcore.directory.services.SchoolService;
import org.entcore.directory.services.impl.DefaultSchoolService;
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

import fr.wseduc.webutils.Controller;
import org.entcore.common.neo4j.Neo;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.http.HttpClientUtils;
import org.entcore.common.user.UserUtils;
import org.entcore.common.user.UserInfos;
import fr.wseduc.security.SecuredAction;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;
import static org.entcore.common.user.SessionAttributes.*;

public class UserBookController extends Controller {

	private Neo neo;
	private JsonObject config;
	private JsonObject userBookData;
	private HttpClient client;
	private SchoolService schoolService;

	public UserBookController(Vertx vertx, Container container,
		RouteMatcher rm, Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions, JsonObject config) {
			super(vertx, container, rm, securedActions);
			pathPrefix = "/userbook";
			this.neo = new Neo(Server.getEventBus(vertx),log);
			this.config = config;
			userBookData= config.getObject("user-book-data");
			schoolService = new DefaultSchoolService(neo, eb);
			client = vertx.createHttpClient()
							.setHost(config.getString("workspace-url"))
							.setPort(config.getInteger("workspace-port"))
							.setMaxPoolSize(16)
							.setKeepAlive(false);
		}

	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void monCompte(HttpServerRequest request) {
		renderView(request);
	}
	
	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void birthday(HttpServerRequest request) {
		renderView(request);
	}
	
	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void mood(HttpServerRequest request) {
		renderView(request);
	}

	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void annuaire(HttpServerRequest request) {
		renderView(request);
	}
	
	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void classAdmin(HttpServerRequest request) {
		renderView(request);
	}

	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void search(final HttpServerRequest request) {
		String name = request.params().get("name");
		String filter = "";
		JsonObject params = new JsonObject();
		if (name != null && !name.trim().isEmpty()) {
			filter = "WHERE visibles.displayName=~{regex} ";
			params.putString("regex", "(?i)^.*?" + Pattern.quote(name.trim()) + ".*?$");
		}
		String customReturn = filter +
				"OPTIONAL MATCH visibles-[:USERBOOK]->u " +
				"RETURN distinct visibles.id as id, visibles.displayName as displayName, " +
				"u.mood as mood, u.userid as userId, u.picture as photo, " +
				"profile.name as type " +
				"ORDER BY displayName";
		UserUtils.findVisibleUsers(eb, request, true, customReturn, params, new Handler<JsonArray>() {

			@Override
			public void handle(JsonArray users) {
				renderJson(request, users);
			}
		});
	}

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
								"WITH DISTINCT h, c, n, v, u, n2, n.address as address, " +
								"n.email as email, u.health as health, " +
								"n.homePhone as tel, n.birthDate as birthdate, " +
								"COLLECT(distinct [type(r0),type(r1),type(r2),type(r3),type(r4)]) as r ";
					} else {
						params.put("userId",request.params().get("id"));
						hobbyVisibility = "PUBLIC";
						personnalInfos = "OPTIONAL MATCH u-[:SHOW_EMAIL]->e " +
								"OPTIONAL MATCH u-[:SHOW_MAIL]->a " +
								"OPTIONAL MATCH u-[:SHOW_PHONE]->p " +
								"OPTIONAL MATCH u-[:SHOW_BIRTHDATE]->b " +
								"OPTIONAL MATCH u-[:SHOW_HEALTH]->s " +
								"WITH h, c, n, v, u, n2, a.address as address, " +
								"e.email as email, s.health as health, " +
								"p.homePhone as tel, b.birthDate as birthdate, " +
								"COLLECT([]) as r ";
					}
					String query = "MATCH (n:User) "
								+ "WHERE n.id = {userId} "
								+ "OPTIONAL MATCH n-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(c:Structure)"
								+ "OPTIONAL MATCH (n)-[:USERBOOK]->(u) "
								+ "OPTIONAL MATCH (u)-[v:" + hobbyVisibility + "]->(h1) "
								+ "OPTIONAL MATCH (n)-[:EN_RELATION_AVEC]-(n2) "
								+ "WITH DISTINCT h1 as h, c, n, v, u, n2 "
								+ personnalInfos
								+ "RETURN DISTINCT "
									+ "n.id as id,"
									+ "n.login as login, "
									+ "n.displayName as displayName,"
									+ "address,"
									+ "email, "
									+ "tel, "
									+ "birthdate, "
									+ "HEAD(r) as visibleInfos, "
									+ "c.name as schoolName, "
									+ "n2.displayName as relatedName, "
									+ "n2.id as relatedId,"
									+ "n2.type as relatedType,"
									+ "u.userid as userId,"
									+ "u.motto as motto,"
									+ "COALESCE(u.picture, {defaultAvatar}) as photo,"
									+ "COALESCE(u.mood, {defaultMood}) as mood,"
									+ "health,"
									+ "COLLECT(type(v)) as visibility,"
									+ "COLLECT(h.category) as category,"
									+ "COLLECT(h.values) as values";
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
						"RETURN DISTINCT c.id as id, c.name as name, c.level as level " +
						"ORDER BY name ASC ";
				UserUtils.findVisibleProfilsGroups(eb, request, customReturn, params, new Handler<JsonArray>() {
					@Override
					public void handle(JsonArray classes) {
						JsonObject result = new JsonObject()
								.putArray("users", personnel)
								.putArray("classes", classes);
						renderJson(request, result);
					}
				});
			}
		});
	}

	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void myClass(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					String classId = request.params().get("id");
					String matchClass;
					Map<String, Object> params = new HashMap<>();
					if (classId == null || classId.trim().isEmpty()) {
						matchClass = "(n:User {id : {userId}})-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(c:Class) ";
						params.put("userId", user.getUserId());
					} else {
						matchClass = "(c:Class {id : {classId}}) ";
						params.put("classId", classId);
					}
					String query =
							"MATCH " + matchClass +
							"WITH c " +
							"MATCH c<-[:DEPENDS]-(cpg:ProfileGroup)-[:DEPENDS]->(pg:ProfileGroup)" +
							"-[:HAS_PROFILE]->(p:Profile), cpg<-[:IN]-(m:User) " +
							"WHERE p.name IN ['Student','Teacher'] AND (NOT(HAS(m.blocked)) OR m.blocked = false) " +
							"OPTIONAL MATCH m-[:USERBOOK]->u " +
							"RETURN distinct p.name as type, m.id as id, " +
							"m.displayName as displayName, u.mood as mood, " +
							"u.userid as userId, u.picture as photo " +
							"ORDER BY type DESC, displayName ";
					neo.send(query, params, request.response());
				} else {
					unauthorized(request);
				}
			}
		});
	}

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
						UserUtils.removeSessionAttribute(eb, user.getUserId(), THEME_ATTRIBUTE, null);
					} else {
						badRequest(request);
					}
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void setVisibility(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request,new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				Map<String, Object> params = new HashMap<>();
				params.put("id", user.getUserId());
				params.put("category", request.params().get("category"));
				String visibility = "PUBLIC".equals(request.params().get("value")) ? "PUBLIC" : "PRIVE";
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

	public void initUserBookNode(final Message<JsonObject> message){
		JsonObject params = new JsonObject();
		params.putString("userId", message.body().getString("userId"));
		params.putString("avatar", userBookData.getString("default-avatar"));
		params.putString("theme", userBookData.getString("default-theme", ""));
		JsonArray queries = new JsonArray();
		String query = "MATCH (n:User) "
				+ "WHERE n.id = {userId} "
				+ "CREATE n-[:USERBOOK]->(m:UserBook {type:'USERBOOK', picture:{avatar}, "
				+ "motto:'', health:'', mood:'default', userid:{userId}, theme: {theme}})";
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
	}

	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void getAvatar(final HttpServerRequest request) {
		String id = request.params().get("id");
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
					request.response().sendFile("./public/img/no-avatar.jpg");
				}
			});
		} else {
			request.response().sendFile("./public/img/no-avatar.jpg");
		}
	}

	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void personBirthday(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>(){
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					Calendar c = Calendar.getInstance();
					int month = c.get(Calendar.MONTH);
					String [] monthRegex = {"12|01|02", "01|02|03", "02|03|04", "03|04|05", "04|05|06", "05|06|07",
							"06|07|08", "07|08|09", "08|09|10", "09|10|11", "10|11|12", "11|12|01"};
					String query =
							"MATCH (n:User)-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(c:Class) " +
							"WHERE n.id = {id} " +
							"WITH c " +
							"MATCH c<-[:DEPENDS]-(cpg:ProfileGroup)-[:DEPENDS]->(pg:ProfileGroup)" +
							"-[:HAS_PROFILE]->(p:Profile {name : 'Student'}), cpg<-[:IN]-(m:User) " +
							"WHERE m.birthDate=~{regex} " +
							"RETURN distinct m.id as id, m.displayName as username, " +
							"m.birthDate as birthDate, COLLECT(distinct [c.id, c.name]) as classes ";
					JsonObject params = new JsonObject();
					params.putString("id", user.getUserId());
					params.putString("regex", "^[0-9]{4}-(" + monthRegex[month] + ")-(3[01]|[12][0-9]|0[1-9])$");
					neo.execute(query, params, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							JsonArray res;
							if ("ok".equals(event.body().getString("status"))) {
								res = event.body().getArray("result", new JsonArray());
							} else {
								res = new JsonArray();
							}
							renderJson(request, res);
						}
					});
				} else {
					badRequest(request);
				}
			}
		});
	}

	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void editUserInfoVisibility(final HttpServerRequest request) {
		final List<String> infos = Arrays.asList("email", "mail", "phone", "birthdate", "health");
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
					neo.send(query, params, request.response());
				} else {
					unauthorized(request);
				}
			}
		});
	}

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

	@SecuredAction(value = "user.preference", type = ActionType.AUTHENTICATED)
	public void getPreference(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb,request,new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					String query =
							"MATCH (u:User {id:{userId}})-[:PREFERS]->(uac:UserAppConf)"
									+" RETURN uac."+ request.params().get("application") +" AS preference";
					neo.execute(query,
							new JsonObject().putString("userId", user.getUserId()),
							validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
								@Override
								public void handle(Either<String, JsonObject> result) {
									if (result.isRight()) {
										renderJson(request, result.right().getValue());
									} else {
										leftToResponse(request,result.left());
									}
								}
							}));
				} else {
					badRequest(request);
				}
			}
		});
	}

	@SecuredAction(value = "user.preference", type = ActionType.AUTHENTICATED)
	public void updatePreference(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>(){
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					final JsonObject params = new JsonObject().putString("userId", user.getUserId());
					final String application = request.params().get("application");
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


}
