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

public class RoleGroupFilter implements ResourcesProvider {

	private final Neo4j neo4j = Neo4j.getInstance();

	@Override
	public void authorize(HttpServerRequest request, Binding binding, UserInfos user, final Handler<Boolean> handler) {
		Map<String, UserInfos.Function> functions = user.getFunctions();
		final String groupId = request.params().get("groupId");
		final String roleId = request.params().get("roleId");

		if(groupId == null || groupId.trim().isEmpty() || roleId == null || roleId.trim().isEmpty()){
			handler.handle(false);
			return;
		}

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

		final JsonObject params = new JsonObject()
			.putString("groupId", groupId)
			.putString("roleId", roleId)
			.putArray("scopedStructures", new JsonArray(adminLocal.getScope().toArray()));

		final String regularQuery =
				"MATCH (s:Structure)<-[:BELONGS*0..1]-()<-[:DEPENDS]-(:Group {id: {groupId}}), (r:Role) " +
				"WHERE s.id IN {scopedStructures} AND r.id = {roleId} " +
				"AND NOT((:Application {locked: true})-[:PROVIDE]->(:Action)<-[:AUTHORIZE]-(r)) " +
				"OPTIONAL MATCH (ext:Application:External)-[:PROVIDE]->(:Action)<-[:AUTHORIZE]-(r) " +
				"RETURN count(distinct r) = 1 as exists, count(distinct ext) as externalApps";
		final String externalQuery =
				"MATCH (r:Role {id : {roleId}}), " +
				"(ext:Application:External)-[:PROVIDE]->(:Action)<-[:AUTHORIZE]-(r), " +
				"(s:Structure)-[:HAS_ATTACHMENT*0..]->(p:Structure) " +
				"WHERE s.id IN {scopedStructures} AND p.id = ext.structureId AND (ext.inherits = true OR p = s) " +
				"RETURN (count(distinct r) = 1 AND count(distinct ext) = {nbExt}) as exists";

		neo4j.execute(regularQuery, params, new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> event) {
				JsonArray r = event.body().getArray("result");
				if("ok".equals(event.body().getString("status")) && r != null && r.size() == 1){
					boolean exists = ((JsonObject) r.get(0)).getBoolean("exists", false);
					int nbExt = ((JsonObject) r.get(0)).getInteger("nbExt", 0);
					if(!exists){
						handler.handle(false);
					} else if(nbExt == 0){
						handler.handle(true);
					} else {
						neo4j.execute(externalQuery, params.putNumber("nbExt", nbExt), new Handler<Message<JsonObject>>() {
							public void handle(Message<JsonObject> event) {
								JsonArray r = event.body().getArray("result");
								if("ok".equals(event.body().getString("status")) && r != null && r.size() == 1){
									handler.handle(((JsonObject) r.get(0)).getBoolean("exists", false));
								} else {
									handler.handle(false);
								}
							}
						});
					}
				} else {
					handler.handle(false);
				}

			}
		});

	}

}
