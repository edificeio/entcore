package edu.one.core.registry.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import edu.one.core.infra.AbstractService;
import edu.one.core.infra.Neo;

public class AppRegistryService extends AbstractService {

	private final Neo neo;

	public AppRegistryService(Vertx vertx, Container container, RouteMatcher rm) {
		super(vertx, container, rm);
		neo = new Neo(vertx.eventBus(), log);
	}

	public void listApplications(HttpServerRequest request) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("type","APPLICATION");
		neo.send(
			"START n=node:node_auto_index(type={type}) " +
			"RETURN n.id as id, n.name as name",
			params,
			request.response()
		);
	}

	public void listApplicationActions(HttpServerRequest request) {
		String name = request.params().get("name");
		if (name != null && !name.trim().isEmpty()) {
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("name", name);
			neo.send(
					"START n=node:node_auto_index(name={name}) " +
					"MATCH n-[:PROVIDE]->a " +
					"RETURN a.name as name, a.displayName as displayName, a.type as type",
				params,
				request.response()
			);
		} else {
			request.response().setStatusCode(400).end();
		}
	}

	public void collectApps(final Message<JsonObject> message) {
		final String application = message.body().getString("application");
		JsonArray securedActions = message.body().getArray("actions");
		if (application != null && securedActions != null && !application.trim().isEmpty()) {
			final JsonArray toQuery = new JsonArray();
			for (Object o: securedActions) {
				JsonObject json = (JsonObject) o;
				String name = json.getString("name");
				String displayName = json.getString("displayName");
				String type = json.getString("type", "WORKFLOW");
				if (name != null && displayName != null &&
						!name.trim().isEmpty() && !displayName.trim().isEmpty()) {
					JsonArray tmp = new JsonArray();
					tmp.addString(name).addString(displayName).addString("SECURED_ACTION_" + type);
					if (tmp.size() > 0) {
						toQuery.addArray(tmp);
					}
				}
			}
			neo.send(
				"START n=node:node_auto_index(name='" + application + "') "+
				"WITH count(*) AS exists " +
				"WHERE exists=0" +
				"CREATE (m {id:'" + UUID.randomUUID().toString() + "', " +
				"type:'APPLICATION', " +
				"name:'" + application +
				"'}) " +
				"RETURN m.id as id",
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						if ("ok".equals(event.body().getString("status"))) {
							String actions = toQuery.encode();
							neo.send(
								"START n=node:node_auto_index(name='" + application + "') " +
								"FOREACH (action in " + actions + " : " +
								"CREATE UNIQUE n-[:PROVIDE]->(a {type:head(tail(tail(action))), " +
								"name:head(action), displayName:head(tail(action))})) " +
								"RETURN n"
							);
						message.reply(event.body());
						}
					}
				}
			);
		}
	}

}
