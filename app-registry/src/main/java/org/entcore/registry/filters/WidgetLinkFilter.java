package org.entcore.registry.filters;

import java.util.Map;

import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.webutils.http.Binding;

public class WidgetLinkFilter implements ResourcesProvider{

	private final Neo4j neo4j = Neo4j.getInstance();

	@Override
	public void authorize(HttpServerRequest request, Binding binding, UserInfos user, final Handler<Boolean> handler) {

		Map<String, UserInfos.Function> functions = user.getFunctions();

		if (functions == null || functions.isEmpty()) {
			handler.handle(false);
			return;
		}
		if (functions.containsKey(DefaultFunctions.SUPER_ADMIN)) {
			handler.handle(true);
			return;
		}
		final UserInfos.Function adminLocal = functions.get(DefaultFunctions.ADMIN_LOCAL);
		if (adminLocal == null || adminLocal.getScope() == null) {
			handler.handle(false);
			return;
		}

		final String groupId = request.params().get("groupId");
		if(groupId == null || groupId.trim().isEmpty()){
			handler.handle(false);
			return;
		}

		String query =
			"MATCH (s:Structure)<-[:BELONGS*0..1]-()<-[:DEPENDS]-(g:Group {id : {groupId}}) "+
			"WHERE s.id IN {adminScope} " +
			"RETURN count(g) = 1 as exists";
		JsonObject params = new JsonObject()
			.putString("groupId", groupId)
			.putArray("adminScope", new JsonArray(adminLocal.getScope().toArray()));

		neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> event) {
				JsonArray r = event.body().getArray("result");
				handler.handle(
					"ok".equals(event.body().getString("status")) &&
							r != null && r.size() == 1 &&
							((JsonObject) r.get(0)).getBoolean("exists", false)
				);
			}
		});
	}

}
