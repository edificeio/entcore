package edu.one.core.userbook.controllers;

import java.util.HashMap;
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
import edu.one.core.infra.Server;
import edu.one.core.infra.http.HttpClientUtils;
import edu.one.core.infra.security.UserUtils;
import edu.one.core.infra.security.resources.UserInfos;
import edu.one.core.security.SecuredAction;


public class UserBookController extends Controller {

	private Neo neo;
	private JsonObject config;
	private JsonObject userBookData;
	private HttpClient client;

	public UserBookController(Vertx vertx, Container container,
		RouteMatcher rm, Map<String, edu.one.core.infra.security.SecuredAction> securedActions, JsonObject config) {
			super(vertx, container, rm, securedActions);
			this.neo = new Neo(Server.getEventBus(vertx),log);
			this.config = config;
			userBookData= config.getObject("user-book-data");
			client = vertx.createHttpClient()
							.setHost(config.getString("workspace-url"))
							.setPort(config.getInteger("workspace-port"));
		}

	@SecuredAction("userbook.authent")
	public void monCompte(HttpServerRequest request) {
		renderView(request, config);
	}

	@SecuredAction("userbook.authent")
	public void annuaire(HttpServerRequest request) {
		renderView(request, config);
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
							"WHERE has(m.id) AND m.id <> {id} AND has(m.ENTPersonLogin) " +
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
				String personRequest = "";
				String personRequestStart = "START n=node:node_auto_index(id='" + request.params().get("id") + "')";
				String personRequestReturn= " RETURN distinct n.ENTPersonNomAffichage as displayName, "
						+ "n.id as id,n.ENTPersonAdresse as address, u.ENTPersonNomAffichage? as relatedName, "
						+ "u.id? as relatedId,u.type as relatedType,u.userid? as userId, u.motto? as motto, u.picture? as photo, u.mood? as mood, "
						+ "u.health? as health, p.category? as category, p.values? as values;";

				switch(request.params().get("type")){
					case "ELEVE":
						personRequest = personRequestStart + ",u=node:node_auto_index('type:USERBOOK OR type:PERSRELELEVE') "
							+ "MATCH (n)-[:EN_RELATION_AVEC|USERBOOK]->(u)-[PUBLIC]->(p)" + personRequestReturn;
						break;
					case "ENSEIGNANT":
						personRequest = personRequestStart + " MATCH (n)-[USERBOOK]->(m)-[PUBLIC]->(p) "
							+ "RETURN distinct n.ENTPersonNomAffichage as displayName, n.id as id, "
							+ "n.ENTPersonAdresse as address,m.motto? as motto, m.picture? as photo, m.mood? as mood, "
							+ "m.health? as health, m.userid? as userId, p.category? as category, p.values? as values;";
						break;
					case "PERSRELELEVE":
						personRequest = personRequestStart + ",u=node:node_auto_index('type:USERBOOK OR type:ELEVE') "
							+ "MATCH (p)<-[PUBLIC]-(m)<-[USERBOOK?]-(n)<-[EN_RELATION_AVEC]-(u) "
							+ "RETURN distinct n.ENTPersonNomAffichage as displayName, n.id as id, "
							+ "n.ENTPersonAdresse as address, u.ENTPersonNomAffichage as relatedName, "
							+ "u.id as relatedId,u.type as relatedType,m.userid? as userId, m.motto? as motto, m.picture? as photo, "
							+ "m.mood? as mood, m.health? as health, p.category? as category, p.values? as values;";
						break;
				}
				neo.send(personRequest,request.response());
			}
		});

	}

	@SecuredAction("userbook.authent")
	public void account(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request,new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				String personRequest  = "START n=node:node_auto_index(id='" + user.getUserId() + "'), "
						+ "m=node:node_auto_index(type='USERBOOK'), p=node:node_auto_index(type='HOBBIES') "
						+ "MATCH n-[USERBOOK]->m-[r]->p "
						+ "RETURN n.ENTPersonNomAffichage as displayName, n.id as id, "
						+ "m.motto as motto, m.health as health, m.picture as photo, m.userid as userId, m.mood as mood, "
						+ "type(r) as visibility, p.category as category, p.values as values;";
				neo.send(personRequest,request.response());
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
			public void handle(UserInfos user) {
				String neoRequest = "START n=node:node_auto_index(id='" + user.getUserId() + "') MATCH (n)-[USERBOOK]->(m)";
				if (request.params().contains("category")){
					neoRequest += ", (m)-->(p) WHERE has(p.category) "
					+ "AND p.category='" + request.params().get("category") + "' "
					+ "SET p.values='" + request.params().get("values") + "'";
				} else {
					neoRequest += " SET m." + request.params().get("prop") + "='" + request.params().get("value") + "'";
					if ("mood".equals(request.params().get("prop")) || "motto".equals(request.params().get("prop"))){
						notifyShare(request.params().get("value"), user, null);
					}
				}
				neo.send(neoRequest, request.response());
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
		HttpClientUtils.proxy(request, client, "\\" + pathPrefix, config.getString("workspace-prefix"));
	}

	public void initUserBookNode(final Message<JsonObject> message){
		String  userId = message.body().getString("userId");
		neo.send("START n=node:node_auto_index(id='"+ userId + "') "
			+ "CREATE (m {type:'USERBOOK',picture:'" + userBookData.getString("picture") + "',"
			+ "motto:'', health:'', mood:'default', userid:'" + userId + "'}), n-[:USERBOOK]->m ");
		JsonArray hobbies = userBookData.getArray("hobbies");
		for (Object hobby : hobbies) {
			JsonObject jo = (JsonObject)hobby;
			neo.send("START n=node:node_auto_index(id='"+ userId + "'),m=node(*) MATCH n-[r]->m WHERE "
				+ "type(r)='USERBOOK' CREATE (p {type:'HOBBIES',category:'" + jo.getString("code")
				+ "', values:''}), m-[:PUBLIC]->p");
		}
	}


		// TODO extract in external helper class
		// TODO get sharedArray (of people who are allowed to see me)
	private void notifyShare(String resource, UserInfos user, JsonArray sharedArray) {
		JsonArray recipients = new JsonArray();
		recipients.addString(user.getUserId());
		JsonObject event = new JsonObject()
		.putString("action", "add")
		.putString("resource", resource)
		.putString("sender", user.getUserId())
		.putString("message", "<a href=\"http://localhost:8101/annuaire#"
				+  user.getUserId() + "#" + user.getType() + "\">" + user.getUsername() + "</a> a modifié son profil : " + resource)
		.putArray("recipients", recipients);
		eb.send("wse.timeline", event);
	}

}