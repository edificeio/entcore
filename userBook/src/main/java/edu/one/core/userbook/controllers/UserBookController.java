package edu.one.core.userbook.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import edu.one.core.infra.Controller;
import edu.one.core.infra.Neo;
import edu.one.core.infra.NotificationHelper;
import edu.one.core.infra.Server;
import edu.one.core.infra.http.HttpClientUtils;
import static edu.one.core.infra.http.Renders.unauthorized;
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

	@SecuredAction("userbook.authent")
	public void monCompte(HttpServerRequest request) {
		renderView(request);
	}

	@SecuredAction("userbook.authent")
	public void annuaire(HttpServerRequest request) {
		renderView(request);
	}

	@SecuredAction("userbook.authent")
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

	@SecuredAction("userbook.authent")
	public void person(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request,new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					String query = "START n=node:node_auto_index(id={userId}) "
								+ "MATCH "
									+ "(n)-[?:USERBOOK]->(u)-[v?]->(h), "
									+ "(n)-[?:EN_RELATION_AVEC]-(n2) "
								+ "WHERE (v IS NULL) OR (type(v) IN {hobbyVisibility}) "
								+ "RETURN DISTINCT "
									+ "n.id as id,"
									+ "n.ENTPersonNomAffichage as displayName,"
									+ "n.ENTPersonAdresse as address,"
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

					Map<String, Object> params = new HashMap<>();
					if (request.params().get("id") == null) {
						params.put("userId",user.getUserId());
						params.put("hobbyVisibility", Arrays.asList("PUBLIC","PRIVE"));
					} else {
						params.put("userId",request.params().get("id"));
						params.put("hobbyVisibility","PUBLIC");
					}
					params.put("defaultAvatar", userBookData.getString("default-avatar"));
					params.put("defaultMood", userBookData.getString("default-mood"));

					neo.send(query, params, request.response());
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@SecuredAction("userbook.authent")
	public void myClass(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					String query =
							"START n=node:node_auto_index(id={id}) " +
							"MATCH n-[:APPARTIENT]->c " +
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

	@SecuredAction("userbook.authent")
	public void editUserBookInfo(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request,new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
				String neoRequest = "START n=node:node_auto_index(id='" + user.getUserId() + "') MATCH (n)-[USERBOOK]->(m)";
				if (request.params().contains("category")){
					neoRequest += ", (m)-->(p) WHERE has(p.category) "
					+ "AND p.category='" + request.params().get("category") + "' "
					+ "SET p.values='" + request.params().get("values") + "'";
				} else {
					neoRequest += " SET m." + request.params().get("prop") + "='" + request.params().get("value") + "'";
					if ("mood".equals(request.params().get("prop")) || "motto".equals(request.params().get("prop"))){
						notifyTimeline(request, user);
					}
				}
				neo.send(neoRequest, request.response());
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@SecuredAction("userbook.authent")
	public void setVisibility(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request,new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				neo.send("START n=node:node_auto_index(id='" + user.getUserId() + "') MATCH (n)-[USERBOOK]->(m)-[s]->(p) "
					+ "WHERE has(p.category) AND p.category='"+ request.params().get("category")
					+ "' DELETE s CREATE (m)-[j:"+ request.params().get("value") +"]->(p) "
					+ "RETURN n,m,j,p", request.response());
			}
		});
	}

	@SecuredAction("userbook.authent")
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

}