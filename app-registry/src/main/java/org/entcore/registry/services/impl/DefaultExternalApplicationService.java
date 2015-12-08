package org.entcore.registry.services.impl;

import static fr.wseduc.webutils.Utils.defaultValidationParamsNull;
import static org.entcore.common.neo4j.Neo4jResult.validEmptyHandler;
import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;

import java.util.List;
import java.util.UUID;

import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.common.validation.StringValidation;
import org.entcore.registry.services.ExternalApplicationService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.collections.Joiner;

public class DefaultExternalApplicationService implements ExternalApplicationService {

	private final Neo4j neo = Neo4j.getInstance();

	@Override
	public void listExternalApps(String structureId, final Handler<Either<String, JsonArray>> handler) {
		String filter = "";
		JsonObject params = null;
		if (structureId != null && !structureId.trim().isEmpty()) {
			filter =
				", (s:Structure)-[:HAS_ATTACHMENT*0..]->(p:Structure) " +
				"WHERE HAS(app.structureId) AND s.id = {structure} AND p.id = app.structureId AND r.structureId = app.structureId " +
				"AND (app.inherits = true OR p = s) ";
			params = new JsonObject().putString("structure", structureId);
		}
		String query =
				"MATCH (app:Application:External)-[:PROVIDE]->(act:Action)<-[:AUTHORIZE]-(r:Role) " + filter +
				"WITH app, r, collect(distinct act) as roleActions " +
				"MATCH (app)-[:PROVIDE]->(action:Action) " +
				"RETURN distinct app as application, collect(action) as actions, collect(distinct {role: r, actions: roleActions}) as roles";
		neo.execute(query, params, validResultHandler(new Handler<Either<String, JsonArray>>(){
			public void handle(Either<String, JsonArray> event) {
				if(event.isLeft()){
					handler.handle(event);
					return;
				}
				JsonArray rows = event.right().getValue();
				for(Object objRow : rows){
					JsonObject row = (JsonObject) objRow;
					JsonObject application = row.getObject("application");
					JsonArray actions = row.getArray("actions");
					JsonArray roles = row.getArray("roles");

					JsonObject appData = application.getObject("data");
					JsonArray scope = appData.getArray("scope");
					if (scope != null && scope.size() > 0) {
						appData.putString("scope", Joiner.on(" ").join(scope.toArray()));
					} else {
						appData.putString("scope", "");
					}
					row.putObject("data", appData);
					row.removeField("application");

					JsonArray actionsCopy = new JsonArray();
					for(Object actionObj: actions){
						JsonObject action = (JsonObject) actionObj;
						JsonObject data = action.getObject("data");
						actionsCopy.add(data);
					}
					row.putArray("actions", actionsCopy);

					for(Object roleObj : roles){
						JsonObject role = (JsonObject) roleObj;
						JsonObject data = role.getObject("role").getObject("data");
						role.putObject("role", data);
						JsonArray acts = role.getArray("actions");
						JsonArray actsCopy = new JsonArray();
						for(Object actionObj : acts){
							JsonObject action = (JsonObject) actionObj;
							actsCopy.add(action.getObject("data"));
						}
						role.putArray("actions", actsCopy);

					}
				}

				handler.handle(event);
			}
		}));
	}

	@Override
	public void listCasConnectors(Handler<Either<String, JsonArray>> handler) {
		String query =
				"MATCH (app:External) " +
				"WHERE has(app.casType) " +
				"RETURN app.casType as service, COLLECT(app.pattern) as patterns";
		neo.execute(query, (JsonObject) null, validResultHandler(handler));
	}

	@Override
	public void deleteExternalApplication(String applicationId,  Handler<Either<String, JsonObject>> handler) {
		String query =
			"MATCH (n:Application:External {id : {id}}) " +
			"WHERE coalesce(n.locked, false) = false " +
			"MATCH n-[r1:PROVIDE]->(a:Action) " +
			"OPTIONAL MATCH a<-[r2:AUTHORIZE]-(r:Role) " +
			"OPTIONAL MATCH r<-[r3:AUTHORIZED]-(g:Group) " +
			"WHERE r.structureId = n.structureId " +
			"DELETE r1, r2, r3, n, a, r ";
		JsonObject params = new JsonObject().putString("id", applicationId);
		neo.execute(query, params, validEmptyHandler(handler));
	}

	@Override
	public void createExternalApplication(String structureId, JsonObject application, final Handler<Either<String, JsonObject>> handler) {
		if (defaultValidationParamsNull(
				handler,
				application,
				application.getString("name"),
				structureId,
				application.getString("address")))
			return;

		final String applicationName = application.getString("name");
		final String id = UUID.randomUUID().toString();
		application.putArray("scope", new JsonArray("[\"" + application.getString("scope", "").replaceAll("\\s", "\",\"") + "\"]"));
		application.putString("id", id);
		application.putString("structureId", structureId);

		/* App creation query */
		final String createApplicationQuery =
				"MATCH (n:Application) " +
				"WHERE n.name = {applicationName} " +
				"WITH count(*) AS exists " +
				"WHERE exists=0 " +
				"CREATE (m:Application:External {props}) " +
				"RETURN m.id as id";

		final StatementsBuilder b = new StatementsBuilder()
				.add(createApplicationQuery,
						new JsonObject().putString("applicationName", applicationName).putObject("props", application));

		/* Underlying action & role creation query */
		String createActionsAndRolesQuery =
				"MATCH (n:Application) " +
				"WHERE n.id = {id} " +
				"CREATE UNIQUE n-[r:PROVIDE]->(a:Action:WorkflowAction {type: {type}, " +
				"name:{name}, displayName:{displayName}}) " +
				"WITH a " +
				"CREATE UNIQUE (r:Role {id: {roleId}, name: {roleName}, structureId: {structureId}})-[:AUTHORIZE]->(a) " +
				"RETURN a.name as name";
		b.add(createActionsAndRolesQuery, new JsonObject()
				.putString("id", id)
				.putString("roleId", UUID.randomUUID().toString())
				.putString("type", "SECURED_ACTION_WORKFLOW")
				.putString("name", applicationName + "|address")
				.putString("roleName", applicationName + "- ACCESS ")
				.putString("structureId", structureId)
				.putString("displayName", applicationName + ".address"));

		neo.executeTransaction(b.build(), null, true, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				JsonArray results = m.body().getArray("results");
				if ("ok".equals(m.body().getString("status")) && results != null) {
					JsonArray r = results.get(0);
					JsonObject j;
					if (r.size() > 0) {
						j = r.get(0);
					} else {
						j = new JsonObject();
					}
					handler.handle(new Either.Right<String, JsonObject>(j));
				} else {
					handler.handle(new Either.Left<String, JsonObject>(m.body().getString("message")));
				}
			}
		});
	}

	@Override
	public void toggleLock(String structureId, Handler<Either<String, JsonObject>> handler) {
		if (defaultValidationParamsNull(handler, structureId)) return;
		String query =
				"MATCH (app:Application:External) WHERE app.id = {structureId} " +
				"SET app.locked = NOT coalesce(app.locked, false) " +
				"RETURN app.locked as locked";
		neo.execute(query, new JsonObject().putString("structureId", structureId), validUniqueResultHandler(handler));
	}

	@Override
	public void massAuthorize(String appId, List<String> profiles, final Handler<Either<String, JsonObject>> handler){
		String query =
			"MATCH (app:Application:External {id: {appId}})-[:PROVIDE]->(act:Action)<-[:AUTHORIZE]-(r:Role), " +
			"(appStruct:Structure)<-[:HAS_ATTACHMENT*1..]-(s:Structure)<-[:DEPENDS]-(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
			"WHERE coalesce(app.locked, false) = false AND appStruct.id = app.structureId AND r.structureId = app.structureId " +
			"AND p.name IN {profiles} AND NOT((pg)-[:AUTHORIZED]->(r)) " +
			"CREATE UNIQUE (pg)-[:AUTHORIZED]->(r) ";
		JsonObject params = new JsonObject()
				.putString("appId", appId)
				.putArray("profiles", new JsonArray(profiles.toArray()));

		neo.execute(query, params, validEmptyHandler(handler));
	}

	@Override
	public void massUnauthorize(String appId, List<String> profiles, final Handler<Either<String, JsonObject>> handler){
		String query =
				"MATCH (app:Application:External {id: {appId}})-[:PROVIDE]->(act:Action)<-[:AUTHORIZE]-(r:Role), " +
				"(appStruct:Structure)<-[:HAS_ATTACHMENT*1..]-(s:Structure)<-[:DEPENDS]-(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
				"WHERE coalesce(app.locked, false) = false AND appStruct.id = app.structureId AND r.structureId = app.structureId " +
				"AND p.name IN {profiles} " +
				"MATCH (r)<-[auth:AUTHORIZED]-(pg) " +
				"DELETE auth ";
			JsonObject params = new JsonObject()
					.putString("appId", appId)
					.putArray("profiles", new JsonArray(profiles.toArray()));

			neo.execute(query, params, validEmptyHandler(handler));
	}

}
