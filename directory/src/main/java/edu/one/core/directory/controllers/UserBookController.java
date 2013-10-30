package edu.one.core.directory.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
import edu.one.core.infra.Neo;
import edu.one.core.infra.NotificationHelper;
import edu.one.core.infra.Server;
import edu.one.core.infra.http.HttpClientUtils;
import edu.one.core.infra.security.UserUtils;
import edu.one.core.infra.security.resources.UserInfos;
import edu.one.core.security.SecuredAction;

import java.util.Arrays;

public class UserBookController extends Controller {

	private Neo neo;
	private JsonObject config;
	private JsonObject userBookData;
	private HttpClient client;
	private final NotificationHelper notification;

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
			notification = new NotificationHelper(eb, container);
		}

	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void monCompte(HttpServerRequest request) {
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
							"MATCH n-[:COMMUNIQUE*1..2]->l-[?:COMMUNIQUE]->m<-[?:COMMUNIQUE_DIRECT]-n " +
							"WITH m " +
							"MATCH m-[?:USERBOOK]->u " +
							"WHERE has(m.id) AND has(m.ENTPersonLogin) " +
							"AND has(m.ENTPersonNomAffichage) " + filter +
							"RETURN distinct m.id as id, m.ENTPersonNomAffichage as displayName, " +
							"u.mood? as mood, u.userid? as userId, u.picture? as photo, m.type as type " +
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
					Map<String, Object> params = new HashMap<>();
					if (request.params().get("id") == null) {
						params.put("userId",user.getUserId());
						hobbyVisibility = "PUBLIC|PRIVE";
					} else {
						params.put("userId",request.params().get("id"));
						hobbyVisibility = "PUBLIC";
					}
					String query = "START n=node:node_auto_index(id={userId}) "
								+ "MATCH "
									+ "n-[?:APPARTIENT]->c<-[?:APPARTIENT]-e-[:EN_RELATION_AVEC*0..1]->n "
									+ "WHERE (c IS NULL) OR c.type='ETABEDUCNAT' WITH n, c "
								+ "MATCH "
									+ "(n)-[?:USERBOOK]->(u)-[v?:" + hobbyVisibility + "]->(h1), "
									+ "(n)-[?:EN_RELATION_AVEC]-(n2) "
								+ "WITH DISTINCT h1 as h, c, n, v, u, n2 "
								+ "RETURN DISTINCT "
									+ "n.id as id,"
									+ "n.ENTPersonLogin as login, "
									+ "n.ENTPersonNomAffichage as displayName,"
									+ "n.ENTPersonAdresse as address,"
									+ "n.ENTPersonMail? as email, "
									+ "n.ENTPersonTelPerson? as tel, "
									+ "n.ENTPersonDateNaissance? as birthdate, "
									+ "c.ENTStructureNomCourant? as schoolName, "
									+ "n2.ENTPersonNomAffichage? as relatedName, "
									+ "n2.id? as relatedId,"
									+ "n2.type as relatedType,"
									+ "u.userid? as userId,"
									+ "u.motto? as motto,"
									+ "COALESCE(u.picture?, {defaultAvatar}) as photo,"
									+ "COALESCE(u.mood?, {defaultMood}) as mood,"
									+ "u.health? as health,"
									+ "COLLECT(type(v)) as visibility,"
									+ "COLLECT(h.category?) as category,"
									+ "COLLECT(h.values?) as values";
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
							"MATCH n-[?:APPARTIENT]->c<-[?:APPARTIENT]-e-[:EN_RELATION_AVEC*0..1]->n " +
							"WITH c " +
							"MATCH c<-[:APPARTIENT]-m-[?:USERBOOK]->u " +
							"WHERE has(c.type) AND c.type = 'CLASSE' AND has(m.ENTPersonLogin) " +
							"RETURN distinct m.type as type, m.id as id, " +
							"m.ENTPersonNomAffichage as displayName, u.mood? as mood, " +
							"u.userid? as userId, u.picture? as photo " +
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
					notification.notifyTimeline(request, user, userIds,
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
						if (photoId != null && !photoId.trim().isEmpty()) {
							redirectPermanent(request, "/workspace/document/" + photoId);
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

}