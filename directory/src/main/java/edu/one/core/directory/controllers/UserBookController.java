package edu.one.core.directory.controllers;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import edu.one.core.common.notification.TimelineHelper;
import edu.one.core.security.ActionType;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import edu.one.core.datadictionary.validation.RegExpValidator;
import edu.one.core.infra.Controller;
import edu.one.core.common.neo4j.Neo;
import edu.one.core.infra.Server;
import edu.one.core.infra.http.HttpClientUtils;
import edu.one.core.common.user.UserUtils;
import edu.one.core.infra.security.resources.UserInfos;
import edu.one.core.security.SecuredAction;

public class UserBookController extends Controller {

	private static final String NOTIFICATION_TYPE = "USERBOOK";
	private Neo neo;
	private JsonObject config;
	private JsonObject userBookData;
	private HttpClient client;
	private final TimelineHelper notification;
	private static final String UUID_REGEX = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$";

	public UserBookController(Vertx vertx, Container container,
		RouteMatcher rm, Map<String, edu.one.core.infra.security.SecuredAction> securedActions, JsonObject config) {
			super(vertx, container, rm, securedActions);
			pathPrefix = "/userbook";
			this.neo = new Neo(Server.getEventBus(vertx),log);
			this.config = config;
			userBookData= config.getObject("user-book-data");
			client = vertx.createHttpClient()
							.setHost(config.getString("workspace-url"))
							.setPort(config.getInteger("workspace-port"))
							.setMaxPoolSize(16)
							.setKeepAlive(false);
			notification = new TimelineHelper(eb, container);
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
	public void annuaire(HttpServerRequest request) {
		renderView(request);
	}

	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void search(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					String name = request.params().get("name");
					String filter = "";
					if (name != null && !name.trim().isEmpty()) {
						filter = "AND m.ENTPersonNomAffichage=~{regex} ";
					}
					String query =
							"START n=node:node_auto_index(id={id}) " +
							"MATCH p=n-[r:COMMUNIQUE|COMMUNIQUE_DIRECT]->t-[:COMMUNIQUE*0..2]->m " +
							"WHERE ((type(r) = 'COMMUNIQUE_DIRECT' AND length(p) = 1) " +
							"XOR (type(r) = 'COMMUNIQUE' AND length(p) >= 2)) " +
							"AND NOT(m.ENTPersonLogin IS NULL) " + filter +
							"OPTIONAL MATCH m-[:USERBOOK]->u " +
							"RETURN distinct m.id as id, m.ENTPersonNomAffichage as displayName, " +
							"u.mood as mood, u.userid as userId, u.picture as photo, m.type as type " +
							"ORDER BY displayName";
					Map<String, Object> params = new HashMap<>();
					params.put("id", user.getUserId());
					params.put("regex", "(?i)^.*?" + Pattern.quote(name.trim()) + ".*?$");
					neo.send(query, params, request.response());
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void person(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request,new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					String hobbyVisibility;
					String personnalInfos;
					Map<String, Object> params = new HashMap<>();
					if (request.params().get("id") == null) {
						params.put("userId",user.getUserId());
						hobbyVisibility = "PUBLIC|PRIVE";
						personnalInfos =
								"OPTIONAL MATCH u-[r0:SHOW_EMAIL]->() " +
								"OPTIONAL MATCH u-[r1:SHOW_BIRTHDATE]->() " +
								"OPTIONAL MATCH u-[r2:SHOW_PHONE]->() " +
								"OPTIONAL MATCH u-[r3:SHOW_MAIL]->() " +
								"OPTIONAL MATCH u-[r4:SHOW_HEALTH]->u " +
								"WITH DISTINCT h, c, n, v, u, n2, n.ENTPersonAdresse as address, " +
								"n.ENTPersonMail as email, u.health as health, " +
								"n.ENTPersonTelPerso as tel, n.ENTPersonDateNaissance as birthdate, " +
								"COLLECT(distinct [type(r0),type(r1),type(r2),type(r3),type(r4)]) as r ";
					} else {
						params.put("userId",request.params().get("id"));
						hobbyVisibility = "PUBLIC";
						personnalInfos = "OPTIONAL MATCH u-[:SHOW_EMAIL]->e " +
								"OPTIONAL MATCH u-[:SHOW_MAIL]->a " +
								"OPTIONAL MATCH u-[:SHOW_PHONE]->p " +
								"OPTIONAL MATCH u-[:SHOW_BIRTHDATE]->b " +
								"OPTIONAL MATCH u-[:SHOW_HEALTH]->s " +
								"WITH h, c, n, v, u, n2, a.ENTPersonAdresse as address, " +
								"e.ENTPersonMail as email, s.health as health, " +
								"p.ENTPersonTelPerso as tel, b.ENTPersonDateNaissance as birthdate, " +
								"COLLECT([]) as r ";
					}
					String query = "START n=node:node_auto_index(id={userId}) "
								+ "MATCH n<-[:EN_RELATION_AVEC*0..1]-e-[:APPARTIENT]->c "
								+ "WHERE (c IS NULL) OR c.type='ETABEDUCNAT' "
								+ "OPTIONAL MATCH (n)-[:USERBOOK]->(u)-[v:" + hobbyVisibility + "]->(h1) "
								+ "OPTIONAL MATCH (n)-[:EN_RELATION_AVEC]-(n2) "
								+ "WITH DISTINCT h1 as h, c, n, v, u, n2 "
								+ personnalInfos
								+ "RETURN DISTINCT "
									+ "n.id as id,"
									+ "n.ENTPersonLogin as login, "
									+ "n.ENTPersonNomAffichage as displayName,"
									+ "address,"
									+ "email, "
									+ "tel, "
									+ "birthdate, "
									+ "HEAD(r) as visibleInfos, "
									+ "c.ENTStructureNomCourant as schoolName, "
									+ "n2.ENTPersonNomAffichage as relatedName, "
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

					neo.send(query, params, request.response());
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void myClass(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					String query =
							"START n=node:node_auto_index(id={id}) " +
							"MATCH n<-[:EN_RELATION_AVEC*0..1]-e-[:APPARTIENT]->c " +
							"WHERE c.type = 'CLASSE' " +
							"WITH c " +
							"MATCH c<-[:APPARTIENT]-m " +
							"WHERE NOT(m.ENTPersonLogin IS NULL) " +
							"OPTIONAL MATCH m-[:USERBOOK]->u " +
							"RETURN distinct m.type as type, m.id as id, " +
							"m.ENTPersonNomAffichage as displayName, u.mood as mood, " +
							"u.userid as userId, u.picture as photo " +
							"ORDER BY type DESC, displayName ";
					Map<String, Object> params = new HashMap<>();
					params.put("id", user.getUserId());
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
					Map<String, Object> params = new HashMap<>();
					params.put("id", user.getUserId());
					String neoRequest =
							"START n=node:node_auto_index(id={id}) " +
							"MATCH (n)-[USERBOOK]->(m)";
					String category = request.params().get("category");
					String prop = request.params().get("prop");
					if (category != null && !category.trim().isEmpty()){
						neoRequest += ", (m)-->(p) WHERE has(p.category) "
								+ "AND p.category={category} "
								+ "SET p.values={values} ";
						params.put("category", request.params().get("category"));
						params.put("values", request.params().get("values"));
					} else if (prop != null && !prop.trim().isEmpty()) {
						String attr = prop.replaceAll("\\W+", "");
						neoRequest += " SET m." + attr + "={value}";
						params.put("value", request.params().get("value"));
					} else {
						badRequest(request);
						return;
					}
					neo.send(neoRequest, params, new Handler<Message<JsonObject>>() {

						@Override
						public void handle(Message<JsonObject> res) {
							if ("ok".equals(res.body().getString("status"))) {
								if ("mood".equals(request.params().get("prop")) ||
										"motto".equals(request.params().get("prop"))){
									notifyTimeline(request, user);
								}
							}
							renderJson(request, res.body());
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void editUserInfo(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request,new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					if ("email".equals(request.params().get("prop"))) {
						String email = request.params().get("value");
						try {
							if (RegExpValidator.instance("email").test(email)) {
								String query =
										"START n=node:node_auto_index(id={id}) " +
										"SET n.ENTPersonMail={email} ";
								Map<String, Object> params = new HashMap<>();
								params.put("id", user.getUserId());
								params.put("email", email);
								neo.send(query, params, request.response());
							} else {
								badRequest(request);
							}
						} catch (Exception e) {
							log.error("Save email", e);
							renderError(request);
						}
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
				neo.send("START n=node:node_auto_index(id={id}) "
					+ "MATCH (n)-[USERBOOK]->(m)-[s]->(p) "
					+ "WHERE has(p.category) AND p.category={category} "
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
		JsonArray queries = new JsonArray();
		String query = "START n=node:node_auto_index(id={userId}) "
				+ "CREATE n-[:USERBOOK]->(m {type:'USERBOOK', picture:{avatar}, "
				+ "motto:'', health:'', mood:'default', userid:{userId}})";
		queries.add(Neo.toJsonObject(query, params));
		String query2 = "START n=node:node_auto_index(id={userId}) "
				+ "MATCH n-[:USERBOOK]->m "
				+ "CREATE m-[:PUBLIC]->(c {category: {category}, type: {type}, values: {values}})";
		for (Object hobby : userBookData.getArray("hobbies")) {
			JsonObject j = params.copy();
			j.putString("type", "HOBBIES" );
			j.putString("category", (String)hobby);
			j.putString("values", "");
			queries.add(Neo.toJsonObject(query2, j));
		}
		neo.sendBatch(queries, (Handler<Message<JsonObject>>) null);
	}

	private void notifyTimeline(final HttpServerRequest request, final UserInfos user) {
		UserUtils.findUsersCanSeeMe(eb, request, new Handler<JsonArray>() {

			@Override
			public void handle(JsonArray users) {
				String action = request.params().get("prop");
				List<String> userIds = new ArrayList<>();
				for (Object o: users) {
					JsonObject u = (JsonObject) o;
					userIds.add(u.getString("id"));
				}
				JsonObject params = new JsonObject()
				.putString("uri", container.config().getString("host") + pathPrefix +
						"/annuaire#" + user.getUserId() + "#" + user.getType())
				.putString("username", user.getUsername())
				.putString("motto", request.params().get("value"))
				.putString("moodImg", request.params().get("value"));
				try {
					notification.notifyTimeline(request, user, NOTIFICATION_TYPE,
							NOTIFICATION_TYPE + "_" + action.toUpperCase(), userIds,
							user.getUserId()+System.currentTimeMillis()+action,
							"notify-" + action + ".html", params);
				} catch (IOException e) {
					log.error("Unable to send timeline notification", e);
				}
			}
		});
	}

	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void getAvatar(final HttpServerRequest request) {
		String id = request.params().get("id");
		if (id != null && !id.trim().isEmpty()) {
			String query =
					"START n=node:node_auto_index(id={id}) " +
					"MATCH n-[:USERBOOK]->u " +
					"WHERE has(u.type) AND u.type = 'USERBOOK' " +
					"RETURN distinct u.picture as photo";
			Map<String, Object> params = new HashMap<>();
			params.put("id", id);
			neo.send(query, params, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					if ("ok".equals(event.body().getString("status"))) {
						String photoId = event.body().getObject("result", new JsonObject())
								.getObject("0", new JsonObject()).getString("photo");
						if (photoId != null && photoId.matches(UUID_REGEX)) {
							redirectPermanent(request, "/workspace/document/" + photoId + "?" + request.query());
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
		Calendar c = Calendar.getInstance();
		int month = c.get(Calendar.MONTH);
		String [] monthRegex = {"12|01|02", "01|02|03", "02|03|04", "03|04|05", "04|05|06", "05|06|07",
				"06|07|08", "07|08|09", "08|09|10", "09|10|11", "10|11|12", "11|12|01"};
		String customReturn =
				"MATCH visibles<-[:EN_RELATION_AVEC*0..1]-e-[:APPARTIENT]->c " +
				"WHERE c.type = 'CLASSE' AND visibles.ENTPersonDateNaissance=~{regex} " +
				"RETURN distinct visibles.id as id, visibles.ENTPersonNomAffichage as username, " +
						"visibles.ENTPersonDateNaissance as birthDate, COLLECT(distinct c.ENTGroupeNom) as classes ";
		JsonObject params = new JsonObject();
		params.putString("regex", "^[0-9]{4}-(" + monthRegex[month] + ")-(3[01]|[12][0-9]|0[1-9])$");
		UserUtils.findVisibleUsers(eb, request, customReturn, params, new Handler<JsonArray>() {
			@Override
			public void handle(JsonArray users) {
				renderJson(request, users);
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
					String query = "START n=node:node_auto_index(id={id}) ";
					if ("public".equals(request.params().get("state"))) {
						query += "MATCH n-[:USERBOOK]->u ";
						if ("health".equals(info)) {
							query += "CREATE u-[r:" + relationship + "]->u ";
						} else {
							query += "CREATE u-[r:" + relationship + "]->n ";
						}
					} else {
						query += "MATCH n-[:USERBOOK]->u-[r:" + relationship + "]->() " +
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
							"START n=node:node_auto_index(id={id}) " +
							"MATCH n-[:USERBOOK]->u " +
							"RETURN u.userPreferencesBirthdayClass? as userPreferencesBirthdayClass";
					neo.send(query, params, request.response());
				} else {
					unauthorized(request);
				}
			}
		});
	}

}