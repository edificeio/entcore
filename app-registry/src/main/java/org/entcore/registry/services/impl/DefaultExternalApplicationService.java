/*
 * Copyright © "Open Digital Education", 2016
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

 */

package org.entcore.registry.services.impl;

import static fr.wseduc.webutils.Utils.defaultValidationParamsNull;
import static org.entcore.common.neo4j.Neo4jResult.validEmptyHandler;
import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.registry.services.ExternalApplicationService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

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
			params = new JsonObject().put("structure", structureId);
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
					JsonObject application = row.getJsonObject("application");
					JsonArray actions = row.getJsonArray("actions");
					JsonArray roles = row.getJsonArray("roles");

					JsonObject appData = application.getJsonObject("data");
					JsonArray scope = appData.getJsonArray("scope");
					if (scope != null && scope.size() > 0) {
						appData.put("scope", Joiner.on(" ").join(scope));
					} else {
						appData.put("scope", "");
					}
					row.put("data", appData);
					row.remove("application");

					JsonArray actionsCopy = new fr.wseduc.webutils.collections.JsonArray();
					for(Object actionObj: actions){
						JsonObject action = (JsonObject) actionObj;
						JsonObject data = action.getJsonObject("data");
						actionsCopy.add(data);
					}
					row.put("actions", actionsCopy);

					for(Object roleObj : roles){
						JsonObject role = (JsonObject) roleObj;
						JsonObject data = role.getJsonObject("role").getJsonObject("data");
						role.put("role", data);
						JsonArray acts = role.getJsonArray("actions");
						JsonArray actsCopy = new fr.wseduc.webutils.collections.JsonArray();
						for(Object actionObj : acts){
							JsonObject action = (JsonObject) actionObj;
							actsCopy.add(action.getJsonObject("data"));
						}
						role.put("actions", actsCopy);

					}
				}

				handler.handle(event);
			}
		}));
	}

	@Override
	public void listExternalApplicationRolesWithGroups(String structureId, String connectorId, Handler<Either<String, JsonArray>> handler) {
		String query =
            "MATCH (e:External{id: {connectorId}})-[:PROVIDE]->()<-[:AUTHORIZE]-(r:Role) " +
                "OPTIONAL MATCH (r)<-[:AUTHORIZED]-(g:Group)-[:DEPENDS*1..2]->(s:Structure {id: {structureId}}) " +
                "OPTIONAL MATCH (s)<-[:HAS_ATTACHMENT]-(subStruct:Structure)<-[:DEPENDS]-()-[:AUTHORIZED]-(r) " +
            "WITH r,e,subStruct, COLLECT(DISTINCT{id: g.id, name: g.name}) as groups " +
            "RETURN r.id as id, r.name as name, e.id as connectorId, " +
                    "COLLECT(DISTINCT subStruct.name) as subStructures, e.structureId as owner, " +
                    "CASE WHEN any(x in groups where x <> {name: null, id: null}) THEN groups ELSE [] END as groups";

		JsonObject params = new JsonObject()
			.put("connectorId", connectorId)
			.put("structureId", structureId);
		neo.execute(query, params, validResultHandler(handler));
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
		JsonObject params = new JsonObject().put("id", applicationId);
		neo.execute(query, params, validEmptyHandler(handler));
	}

	@Override
	public void createExternalApplication(String structureId, final JsonObject application, final Handler<Either<String, JsonObject>> handler) {
		if (defaultValidationParamsNull(
				handler,
				application,
				application.getString("name"),
				structureId,
				application.getString("address")))
			return;

		final String applicationName = application.getString("name");
		final String id = UUID.randomUUID().toString();
		application.put("scope", new fr.wseduc.webutils.collections.JsonArray("[\"" + application.getString("scope", "").replaceAll("\\s", "\",\"") + "\"]"));
		application.put("id", id);
		application.put("structureId", structureId);

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
						new JsonObject().put("applicationName", applicationName).put("props", application));

		/* Underlying action & role creation query */
		String createActionsAndRolesQuery =
				"MATCH (n:Application) " +
				"WHERE n.id = {id} " +
				"CREATE UNIQUE n-[r:PROVIDE]->(a:Action:WorkflowAction {type: {type}, " +
				"name:{name}, displayName:{displayName}}) " +
				"WITH a " +
				"CREATE UNIQUE (r:Role {id: {roleId}, name: {roleName}, structureId: {structureId}})-[:AUTHORIZE]->(a) " +
				"RETURN r.id as roleId";
		b.add(createActionsAndRolesQuery, new JsonObject()
				.put("id", id)
				.put("roleId", UUID.randomUUID().toString())
				.put("type", "SECURED_ACTION_WORKFLOW")
				.put("name", applicationName + "|address")
				.put("roleName", applicationName + "- ACCESS ")
				.put("structureId", structureId)
				.put("displayName", applicationName + ".address"));

		neo.executeTransaction(b.build(), null, true, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				JsonArray results = m.body().getJsonArray("results");
				if ("ok".equals(m.body().getString("status")) && results != null) {
					JsonArray appRes = results.getJsonArray(0);
					JsonArray roleRes = results.getJsonArray(1);
					JsonObject j = new JsonObject()
							.mergeIn(appRes.size() > 0 ? appRes.getJsonObject(0) : new JsonObject())
							.mergeIn(roleRes.size() > 0 ? roleRes.getJsonObject(0) : new JsonObject());
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
		neo.execute(query, new JsonObject().put("structureId", structureId), validUniqueResultHandler(handler));
	}

	@Override
	public void massAuthorize(String appId, List<String> profiles, final Handler<Either<String, JsonObject>> handler){
		String query = "";
		if (profiles.contains("AdminLocal")) {
			profiles = profiles.stream()
					.filter(profile -> !"AdminLocal".equals(profile))
					.collect(Collectors.toList());
			if (profiles.size() == 0) {
				// Only ADML
				query = "MATCH (app:Application:External {id: {appId}})-[:PROVIDE]->(act:Action)<-[:AUTHORIZE]-(r:Role), " +
						"(appStruct:Structure)<-[:HAS_ATTACHMENT*0..]-(s:Structure)<-[:DEPENDS]-(fg:FunctionGroup) " +
						"WHERE coalesce(app.locked, false) = false AND appStruct.id = app.structureId AND r.structureId = app.structureId " +
						"AND fg.name ENDS WITH 'AdminLocal' AND NOT((fg)-[:AUTHORIZED]->(r)) " +
						"CREATE UNIQUE (fg)-[:AUTHORIZED]->(r) ";
			} else {
				// Profiles + ADML
				query = "MATCH (app:Application:External {id: {appId}})-[:PROVIDE]->(act:Action)<-[:AUTHORIZE]-(r:Role), " +
						"(appStruct:Structure)<-[:HAS_ATTACHMENT*0..]-(s:Structure) " +
						"WHERE coalesce(app.locked, false) = false AND appStruct.id = app.structureId AND r.structureId = app.structureId " +
						"WITH r, s " +
						"OPTIONAL MATCH (s)<-[:DEPENDS]-(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
						"WHERE p.name IN {profiles} AND NOT((pg)-[:AUTHORIZED]->(r)) " +
						"MERGE (pg)-[:AUTHORIZED]->(r) " +
						"WITH r, s " +
						"OPTIONAL MATCH (s:Structure)<-[:DEPENDS]-(fg:FunctionGroup) " +
						"WHERE fg.name ENDS WITH 'AdminLocal' AND NOT((fg)-[:AUTHORIZED]->(r)) " +
						"MERGE (fg)-[:AUTHORIZED]->(r)";

			}
		} else {
			// only Profiles (no ADML)
			query = "MATCH (app:Application:External {id: {appId}})-[:PROVIDE]->(act:Action)<-[:AUTHORIZE]-(r:Role), " +
					"(appStruct:Structure)<-[:HAS_ATTACHMENT*0..]-(s:Structure)<-[:DEPENDS]-(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
					"WHERE coalesce(app.locked, false) = false AND appStruct.id = app.structureId AND r.structureId = app.structureId " +
					"AND p.name IN {profiles} AND NOT((pg)-[:AUTHORIZED]->(r)) " +
					"CREATE UNIQUE (pg)-[:AUTHORIZED]->(r) ";
		}

		JsonObject params = new JsonObject();
		params.put("appId", appId);
		if (profiles != null && profiles.size() > 0) {
			params.put("profiles", new fr.wseduc.webutils.collections.JsonArray(profiles));
		}
		neo.execute(query, params, validEmptyHandler(handler));
	}

	@Override
	public void massUnauthorize(String appId, List<String> profiles, final Handler<Either<String, JsonObject>> handler){
		String query = "";
		if (profiles.contains("AdminLocal")) {
			profiles = profiles.stream()
					.filter(profile -> !"AdminLocal".equals(profile))
					.collect(Collectors.toList());
			if (profiles.size() == 0) {
				// Only ADML
				query = "MATCH (app:Application:External {id: {appId}})-[:PROVIDE]->(act:Action)<-[:AUTHORIZE]-(r:Role), " +
						"(appStruct:Structure)<-[:HAS_ATTACHMENT*0..]-(s:Structure)<-[:DEPENDS]-(fg:FunctionGroup) " +
						"WHERE coalesce(app.locked, false) = false AND appStruct.id = app.structureId AND r.structureId = app.structureId " +
						"AND fg.name ENDS WITH 'AdminLocal' " +
						"MATCH (r)<-[auth:AUTHORIZED]-(fg) " +
						"DELETE auth ";;
			} else {
				// Profiles + ADML
				query = "MATCH (app:Application:External {id: {appId}})-[:PROVIDE]->(act:Action)<-[:AUTHORIZE]-(r:Role), " +
						"(appStruct:Structure)<-[:HAS_ATTACHMENT*0..]-(s:Structure) " +
						"WHERE coalesce(app.locked, false) = false AND appStruct.id = app.structureId AND r.structureId = app.structureId " +
						"WITH r, s " +
						"OPTIONAL MATCH (s)<-[:DEPENDS]-(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), (pg)-[auth:AUTHORIZED]->(r) " +
						"WHERE p.name IN {profiles} " +
						"DELETE auth " +
						"WITH r, s " +
						"OPTIONAL MATCH (s:Structure)<-[:DEPENDS]-(fg:FunctionGroup), (fg)-[auth:AUTHORIZED]->(r) " +
						"WHERE fg.name ENDS WITH 'AdminLocal' " +
						"DELETE auth";

			}
		} else {
			// only Profiles (no ADML)
			query = "MATCH (app:Application:External {id: {appId}})-[:PROVIDE]->(act:Action)<-[:AUTHORIZE]-(r:Role), " +
					"(appStruct:Structure)<-[:HAS_ATTACHMENT*0..]-(s:Structure)<-[:DEPENDS]-(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
					"WHERE coalesce(app.locked, false) = false AND appStruct.id = app.structureId AND r.structureId = app.structureId " +
					"AND p.name IN {profiles} " +
					"MATCH (r)<-[auth:AUTHORIZED]-(pg) " +
					"DELETE auth ";
		}


		JsonObject params = new JsonObject();
		params.put("appId", appId);
		if (profiles != null && profiles.size() > 0) {
			params.put("profiles", new fr.wseduc.webutils.collections.JsonArray(profiles));
		}
		neo.execute(query, params, validEmptyHandler(handler));
	}

}
