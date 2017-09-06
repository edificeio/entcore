/* Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package org.entcore.registry.services.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.collections.Joiner;

import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.registry.services.AppRegistryService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import static fr.wseduc.webutils.Utils.defaultValidationParamsNull;
import static org.entcore.common.neo4j.Neo4jResult.*;
import static org.entcore.common.neo4j.Neo4jUtils.nodeSetPropertiesFromJson;

public class DefaultAppRegistryService implements AppRegistryService {

	private final Neo4j neo = Neo4j.getInstance();
	private static final Logger log = LoggerFactory.getLogger(DefaultAppRegistryService.class);

	@Override
	public void listApplications(String structureId, Handler<Either<String, JsonArray>> handler) {
		String filter = "";
		JsonObject params = null;
		if (structureId != null && !structureId.trim().isEmpty()) {
			filter = "WHERE NOT(HAS(n.structureId)) OR n.structureId = {structure} ";
			params = new JsonObject().putString("structure", structureId);
		}
		String query =
				"MATCH (n:Application) " + filter +
				"RETURN n.id as id, n.name as name";
		neo.execute(query, params, validResultHandler(handler));
	}

	@Override
	public void listRoles(String structureId, Handler<Either<String, JsonArray>> handler) {
		String filter = "";
		JsonObject params = null;
		if (structureId != null && !structureId.trim().isEmpty()) {
			filter = "WHERE NOT(HAS(n.structureId)) OR n.structureId = {structure} ";
			params = new JsonObject().putString("structure", structureId);
		}
		String query =
				"MATCH (n:Role) " + filter +
				"RETURN n.id as id, n.name as name ORDER BY name ASC ";
		neo.execute(query, params, validResultHandler(handler));
	}

	@Override
	public void listRolesWithActions(String structureId, Handler<Either<String, JsonArray>> handler) {
		String filter = "";
		JsonObject params = null;
		if (structureId != null && !structureId.trim().isEmpty()) {
			filter = "WHERE NOT(HAS(n.structureId)) OR n.structureId = {structure} ";
			params = new JsonObject().putString("structure", structureId);
		}
		String query =
				"MATCH (n:Role) " + filter +
				"OPTIONAL MATCH n-[r:AUTHORIZE]->a " +
				"RETURN n.id as id, n.name as name, COLLECT([a.name, a.displayName, a.type]) as actions";
		neo.execute(query, params, validResultHandler(handler));
	}

	@Override
	public void listActions(String application, Handler<Either<String, JsonArray>> handler) {
		String query =
				"MATCH (n:Application)-[:PROVIDE]->a " +
				"WHERE n.name = {name} " +
				"RETURN a.name as name, a.displayName as displayName, a.type as type";
		neo.execute(query, new JsonObject().putString("name", application), validResultHandler(handler));
	}

	@Override
	public void listGroupsWithRoles(String structureId, boolean classGroups, Handler<Either<String, JsonArray>> handler) {
		String query;
		JsonObject params = new JsonObject();
		if (structureId != null && !structureId.trim().isEmpty()) {
			String filter = classGroups ? "<-[:BELONGS*0..1]-()" : "";
			params.putString("structureId", structureId);
			query = "MATCH (m:Structure)" + filter + "<-[:DEPENDS]-(n:Group) " +
					"WHERE m.id = {structureId} " +
					"OPTIONAL MATCH n-[r:AUTHORIZED]->a ";
		} else {
			query = "MATCH (n:Group) " +
					"OPTIONAL MATCH n-[r:AUTHORIZED]->a ";
		}
		query += "RETURN distinct n.id as id, n.name as name, n.groupDisplayName as groupDisplayName, " +
				"COLLECT(a.id) as roles " +
				"ORDER BY name ASC ";
		neo.execute(query, params, validResultHandler(handler));
	}

	@Override
	public void listApplicationsWithActions(String structureId, String actionType,
			Handler<Either<String, JsonArray>> handler) {
		String filter = "";
		JsonObject params = new JsonObject();
		if (structureId != null && !structureId.trim().isEmpty()) {
			filter = "WHERE NOT(HAS(n.structureId)) OR n.structureId = {structure} ";
			params.putString("structure", structureId);
		}
		String query =
				"MATCH (n:Application) " + filter +
				"OPTIONAL MATCH n-[r:PROVIDE]->(a:Action) ";
		if (actionType != null &&
				("WORKFLOW".equals(actionType) || "RESOURCE".equals(actionType))) {
			query += "WHERE r IS NULL OR a.type = {actionType} ";
			params.putString("actionType", "SECURED_ACTION_" + actionType);
		}
		query += "RETURN n.id as id, n.name as name, COLLECT([a.name, a.displayName, a.type]) as actions";
		neo.execute(query, params, validResultHandler(handler));
	}

	@Override
	public void createRole(String structureId, JsonObject role, JsonArray actions,
			Handler<Either<String, JsonObject>> handler) {
		String query =
				"MATCH (n:Role) " +
				"WHERE n.name = {roleName} " +
				"WITH count(*) AS exists " +
				"WHERE exists=0 " +
				"CREATE (m:Role {role}) " +
				"WITH m " +
				"MATCH (n:Action) " +
				"WHERE n.name IN {actions} " +
				"CREATE UNIQUE m-[:AUTHORIZE]->n " +
				"RETURN DISTINCT m.id as id";
		if (structureId != null && !structureId.trim().isEmpty()) {
			role.putString("structureId", structureId);
		}
		JsonObject params = new JsonObject()
				.putArray("actions", actions)
				.putObject("role", role.putString("id", UUID.randomUUID().toString()))
				.putString("roleName", role.getString("name"));
		neo.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void updateRole(String roleId, JsonObject role, JsonArray actions,
			Handler<Either<String, JsonObject>> handler) {
		if (defaultValidationParamsNull(handler, roleId, role, actions)) return;
		role.removeField("id");
		String updateValues = "";
		if (role.size() > 0) {
			updateValues = "SET " + nodeSetPropertiesFromJson("role", role);
		}
		String updateActions = "RETURN DISTINCT role.id as id";
		if (actions.size() > 0) {
			updateActions =
					"DELETE r " +
					"WITH role " +
					"MATCH (n:Action) " +
					"WHERE n.name IN {actions} " +
					"CREATE UNIQUE role-[:AUTHORIZE]->n " +
					"RETURN DISTINCT role.id as id";
		}
		String query =
				"MATCH (role:Role {id : {roleId}}) " +
				"OPTIONAL MATCH role-[r:AUTHORIZE]->(a:Action) " +
				updateValues +
				updateActions;
		role.putArray("actions", actions).putString("roleId", roleId);
		neo.execute(query, role, validUniqueResultHandler(handler));
	}

	@Override
	public void deleteRole(String roleId, Handler<Either<String, JsonObject>> handler) {
		String query =
				"MATCH (role:Role {id : {id}}) " +
				"OPTIONAL MATCH role-[r]-() " +
				"DELETE role, r ";
		neo.execute(query, new JsonObject().putString("id", roleId), validUniqueResultHandler(handler));
	}

	@Override
	public void linkRolesToGroup(String groupId, JsonArray roleIds, Handler<Either<String, JsonObject>> handler) {
		JsonObject params = new JsonObject();
		params.putString("groupId", groupId);
		if (groupId != null && !groupId.trim().isEmpty()) {
			String deleteQuery =
					"MATCH (m:Group)-[r:AUTHORIZED]-(:Role) " +
					"WHERE m.id = {groupId} " +
					"DELETE r";
			if (roleIds == null || roleIds.size() == 0) {
				neo.execute(deleteQuery, params, validEmptyHandler(handler));
			} else {
				StatementsBuilder s = new StatementsBuilder().add(deleteQuery, params);
				String createQuery =
						"MATCH (n:Role), (m:Group) " +
						"WHERE m.id = {groupId} AND n.id IN {roles} " +
						"CREATE UNIQUE m-[:AUTHORIZED]->n";
				s.add(createQuery, params.copy().putArray("roles", roleIds));
				neo.executeTransaction(s.build(), null, true, validEmptyHandler(handler));
			}
		} else {
			handler.handle(new Either.Left<String, JsonObject>("invalid.arguments"));
		}
	}

	@Override
	public void addGroupLink(String groupId, String roleId, Handler<Either<String, JsonObject>> handler) {
		JsonObject params = new JsonObject();
		params.putString("groupId", groupId);
		params.putString("roleId", roleId);
		if (groupId != null && !groupId.trim().isEmpty() && roleId != null && !roleId.trim().isEmpty()) {
			String query = "MATCH (r:Role), (g:Group) " +
					"WHERE r.id = {roleId} and g.id = {groupId} " +
					"CREATE UNIQUE (g)-[:AUTHORIZED]->(r)";
			neo.execute(query, params, Neo4jResult.validEmptyHandler(handler));
		}  else {
			handler.handle(new Either.Left<String, JsonObject>("invalid.arguments"));
		}
	}

	@Override
	public void deleteGroupLink(String groupId, String roleId, Handler<Either<String, JsonObject>> handler) {
		JsonObject params = new JsonObject();
		params.putString("groupId", groupId);
		params.putString("roleId", roleId);
		if (groupId != null && !groupId.trim().isEmpty() && roleId != null && !roleId.trim().isEmpty()) {
			String query = "MATCH (g:Group)-[auth:AUTHORIZED]->(r:Role) " +
					"WHERE r.id = {roleId} and g.id = {groupId} " +
					"DELETE auth";
			neo.execute(query, params, Neo4jResult.validEmptyHandler(handler));
		}  else {
			handler.handle(new Either.Left<String, JsonObject>("invalid.arguments"));
		}
	}

	@Override
	public void createApplication(String structureId, JsonObject application, JsonArray actions,
			final Handler<Either<String, JsonObject>> handler) {
		if (defaultValidationParamsNull(handler, application, application.getString("name"))) return;
		final String applicationName = application.getString("name");
		final String id = UUID.randomUUID().toString();
		final String address = application.getString("address");
		application.putArray("scope", new JsonArray("[\"" +
				application.getString("scope", "").replaceAll("\\s", "\",\"") + "\"]"));
		application.putString("id", id);
		final String createApplicationQuery =
				"MATCH (n:Application) " +
				"WHERE n.name = {applicationName} " +
				"WITH count(*) AS exists " +
				"WHERE exists=0 " +
				"CREATE (m:Application {props}) " +
				"RETURN m.id as id";
		final JsonObject params = new JsonObject().putString("applicationName", applicationName);
		if (structureId != null && !structureId.trim().isEmpty()) {
			application.putString("structureId", structureId);
		}
		final StatementsBuilder b = new StatementsBuilder()
				.add(createApplicationQuery, params.copy().putObject("props", application));
		if (actions != null && actions.size() > 0) {
			for (Object o: actions) {
				JsonObject json = (JsonObject) o;
				String type;
				switch (json.getString("type", "WORKFLOW")) {
					case "RESOURCE" : type = "Resource"; break;
					case "AUTHENTICATED" : type = "Authenticated"; break;
					default: type = "Workflow"; break;
				}
				String createAction =
						"MERGE (a:Action:" + type + "Action {name:{name}}) " +
						"SET a.displayName = {displayName}, a.type = {type} " +
						"WITH a " +
						"MATCH (n:Application) " +
						"WHERE n.name = {applicationName} " +
						"CREATE UNIQUE n-[r:PROVIDE]->a " +
						"RETURN a.name as name";
				b.add(createAction, json
						.putString("applicationName", applicationName)
						.putString("type", "SECURED_ACTION_" + json.getString("type", "WORKFLOW")));
			}
		} else if (address != null && !address.trim().isEmpty()) {
			String query2 =
					"MATCH (n:Application) " +
					"WHERE n.id = {id} " +
					"CREATE UNIQUE n-[r:PROVIDE]->(a:Action:WorkflowAction {type: {type}, " +
					"name:{name}, displayName:{displayName}}) " +
					"RETURN a.name as name";
			b.add(query2, new JsonObject()
					.putString("id", id)
					.putString("type", "SECURED_ACTION_WORKFLOW")
					.putString("name", applicationName + "|address")
					.putString("displayName", applicationName + ".address"));
		}
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
	public void getApplication(String applicationId, final Handler<Either<String, JsonObject>> handler) {
		String query =
				"MATCH (n:Application) " +
				"WHERE n.id = {id} " +
				"RETURN n.id as id, n.name as name, " +
				"n.grantType as grantType, n.secret as secret, n.address as address, " +
				"n.icon as icon, n.target as target, n.displayName as displayName, " +
				"n.scope as scope, n.pattern as pattern, n.casType as casType";
		JsonObject params = new JsonObject()
				.putString("id", applicationId);
		neo.execute(query,params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				JsonArray r = res.body().getArray("result");
				if (r != null && r.size() == 1) {
					JsonObject j = r.get(0);
					JsonArray scope = j.getArray("scope");
					if (scope != null && scope.size() > 0) {
						j.putString("scope", Joiner.on(" ").join(scope.toArray()));
					} else {
						j.putString("scope", "");
					}
				}
				handler.handle(validUniqueResult(res));
			}
		});
	}

	@Override
	public void updateApplication(String applicationId, JsonObject application,
			Handler<Either<String, JsonObject>> handler) {
		String query =
				"MATCH (n:Application) " +
				"WHERE n.id = {applicationId} AND coalesce(n.locked, false) = false " +
				"SET " + nodeSetPropertiesFromJson("n", application) +
				"RETURN n.id as id";
		application.putString("applicationId", applicationId);
		application.putArray("scope", new JsonArray("[\"" +
				application.getString("scope", "").replaceAll("\\s", "\",\"") + "\"]"));
		neo.execute(query, application, validUniqueResultHandler(handler));
	}

	@Override
	public void deleteApplication(String applicationId, Handler<Either<String, JsonObject>> handler) {
		String query =
				"MATCH (n:Application { id : {id}}) " +
				"WHERE coalesce(n.locked, false) = false " +
				"OPTIONAL MATCH n-[r1:PROVIDE]->(a:Action) " +
				"OPTIONAL MATCH a<-[r2:AUTHORIZE]-(r:Role) " +
				"DELETE n, r1 " +
				"WITH a, r2 " +
				"WHERE NOT(a<-[:PROVIDE]-()) "+
				"DELETE a, r2";
		JsonObject params = new JsonObject().putString("id", applicationId);
		neo.execute(query, params, validEmptyHandler(handler));
	}

	@Override
	public void applicationAllowedUsers(String application, JsonArray users, JsonArray groups,
			Handler<Either<String, JsonArray>> handler) {
			JsonObject params = new JsonObject().putString("application", application);
			String filter = "";
			if (users != null) {
				filter += "AND u.id IN {users} ";
				params.putArray("users", users);
			}
			if (groups != null) {
				filter += "AND pg.id IN {groups} ";
				params.putArray("groups", groups);
			}
			String query =
					"MATCH (app:Application)-[:PROVIDE]->(a:Action)<-[:AUTHORIZE]-(r:Role)" +
					"<-[:AUTHORIZED]-(pg:Group)<-[:IN]-(u:User) " +
					"WHERE app.name = {application} " + filter +
					"RETURN DISTINCT u.id as id";
			neo.execute(query, params, validResultHandler(handler));
	}

	@Override
	public void applicationAllowedProfileGroups(String application, Handler<Either<String, JsonArray>> handler) {
		String query =
				"MATCH (app:Application)-[:PROVIDE]->(a:Action)<-[:AUTHORIZE]-(r:Role)" +
				"<-[:AUTHORIZED]-(g:ProfileGroup)<-[:DEPENDS*0..1]-(pg:ProfileGroup) " +
				"WHERE app.name = {application} " +
				"RETURN pg.id as id";
		neo.execute(query, new JsonObject().putString("application", application), validResultHandler(handler));
	}

	@Override
	public void setDefaultClassRoles(String classId, Handler<Either<String, JsonObject>> handler) {
		if (defaultValidationParamsNull(handler, classId)) return;
		String query =
				"MATCH (c:Class { id : {id}})<-[:DEPENDS]-(csg:ProfileGroup)-[:DEPENDS]->(ssg:ProfileGroup)-[:HAS_PROFILE]->(sp:Profile {name : 'Student'}), " +
				"c<-[:DEPENDS]-(ctg:ProfileGroup)-[:DEPENDS]->(stg:ProfileGroup)-[:HAS_PROFILE]->(tp:Profile {name : 'Teacher'}), " +
				"c<-[:DEPENDS]-(crg:ProfileGroup)-[:DEPENDS]->(srg:ProfileGroup)-[:HAS_PROFILE]->(rp:Profile {name : 'Relative'}), " +
				"c<-[:DEPENDS]-(cpg:ProfileGroup)-[:DEPENDS]->(spg:ProfileGroup)-[:HAS_PROFILE]->(pp:Profile {name : 'Personnel'}), " +
				"(rs:Role), (rt:Role), (rr:Role), (pr:Role) " +
				"WHERE rs.name =~ '^[A-Za-z0-9]+-(student|all)-default$' " +
				"AND rt.name =~ '^[A-Za-z0-9]+-(teacher|all)-default$' " +
				"AND rr.name =~ '^[A-Za-z0-9]+-(relative|all)-default$' " +
				"AND pr.name =~ '^[A-Za-z0-9]+-(personnel|all)-default$' " +
				"CREATE UNIQUE csg-[:AUTHORIZED]->rs, ctg-[:AUTHORIZED]->rt, crg-[:AUTHORIZED]->rr, cpg-[:AUTHORIZED]->pr";
		final JsonObject params = new JsonObject().putString("id", classId);
		final String widgetQuery =
				"MATCH (c:Class { id : {id}})<-[:DEPENDS]-(csg:ProfileGroup)-[:DEPENDS]->(ssg:ProfileGroup), (w:Widget) " +
				"MERGE w<-[r:AUTHORIZED]-ssg";
		StatementsBuilder sb = new StatementsBuilder();
		sb.add(query, params).add(widgetQuery, params);
		neo.executeTransaction(sb.build(), null, true, validEmptyHandler(handler));
	}

	@Override
	public void listCasConnectors(final Handler<Either<String, JsonArray>> handler) {
		String query =
				"MATCH (app:Application) " +
				"WHERE has(app.casType) and app.casType <> '' " +
				"RETURN app.casType as service, app.address as address, COLLECT(app.pattern) as patterns";
		neo.execute(query, (JsonObject) null, validResultHandler(new Handler<Either<String, JsonArray>>(){
			public void handle(Either<String, JsonArray> event) {
				if(event.isLeft()){
					handler.handle(event);
					return;
				}
				JsonArray results = event.right().getValue();
				for(Object o : results){
					JsonObject app = (JsonObject) o;
					String address = app.getString("address", "");
					JsonArray patterns = app.getArray("patterns", new JsonArray());
					if(patterns.size() == 0 || patterns.size() > 0 && patterns.get(0).toString().isEmpty()){
							URL addressURL;
						try {
							if(address.startsWith("/adapter#")){
								addressURL = new URL(address.substring(address.indexOf("#") + 1));
							} else {
								addressURL = new URL(address);
							}
						} catch (MalformedURLException e) {
							log.error("Malformed address : " + address, e);
							continue;
						}
						String pattern = "^\\Q" + addressURL.getProtocol() + "://" + addressURL.getHost() + (addressURL.getPort() > 0 ? ":" + addressURL.getPort() : "") + "\\E.*";
						patterns.add(pattern);
					}
				}
				handler.handle(new Either.Right<String, JsonArray>(results));
			}
		}));
	}

}
