/* Copyright © "Open Digital Education", 2014
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

 *
 */

package org.entcore.registry.services.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.collections.Joiner;

import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.common.utils.StringUtils;
import org.entcore.registry.services.AppRegistryService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;

import static fr.wseduc.webutils.Utils.defaultValidationParamsNull;
import static org.entcore.common.neo4j.Neo4jResult.*;
import static org.entcore.common.neo4j.Neo4jUtils.nodeSetPropertiesFromJson;

public class DefaultAppRegistryService implements AppRegistryService {

	private final int firstLevel = 1;
	private final int secondLevel = 2;
	private final JsonArray defaultLevelsOfEducation = new JsonArray()
			.add(firstLevel)
			.add(secondLevel);
	private final JsonArray defaultDistributions = new JsonArray();

	private final Neo4j neo = Neo4j.getInstance();
	private static final Logger log = LoggerFactory.getLogger(DefaultAppRegistryService.class);
	private static Pattern URL_PATTERN = Pattern.compile("^https?://[^\\s/$.?#].[^\\s]*$");
	private final Function<Either<String, JsonArray>, Either<String, JsonArray>> addsDefaultRoleDistributions = result -> {
		if(result.isRight()) {
			JsonArray applications = result
					.right()
					.getValue()
					.stream()
					.map(JsonObject.class::cast)
					.map(role -> role.put("distributions", role.getJsonArray("distributions", defaultDistributions)))
					.collect(Collector.of(JsonArray::new, JsonArray::add, JsonArray::add));
			result = new Either.Right<>(applications);
		}
		return result;
	};

	@Override
	public void listApplications(String structureId, Handler<Either<String, JsonArray>> handler) {
		String filter = "";
		JsonObject params = null;
		if (structureId != null && !structureId.trim().isEmpty()) {
			filter = "WHERE NOT(HAS(n.structureId)) OR n.structureId = {structure} ";
			params = new JsonObject().put("structure", structureId);
		}
		String query =
				"MATCH (n:Application) " + filter +
				"RETURN n.id as id, n.displayName as displayName, n.name as name, n.icon as icon, 'External' IN labels(n) as isExternal, n.levelsOfEducation as levelsOfEducation, n.appType as appType";
		neo.execute(query, params, result -> {
			Either<String, JsonArray> resultAsArray = validResult(result);
			if(resultAsArray.isRight()) {
				JsonArray applications = resultAsArray
                        .right()
                        .getValue()
                        .stream()
                        .map(JsonObject.class::cast)
                        .map(app -> app.put("levelsOfEducation", app.getJsonArray("levelsOfEducation", defaultLevelsOfEducation)))
                        .collect(Collector.of(JsonArray::new, JsonArray::add, JsonArray::add));
				resultAsArray = new Either.Right<>(applications);
			}
			handler.handle(resultAsArray);
		});
	}

	@Override
	public void listRoles(String structureId, Handler<Either<String, JsonArray>> handler) {
		String filter = "";
		JsonObject params = null;
		if (structureId != null && !structureId.trim().isEmpty()) {
			filter = "WHERE NOT(HAS(n.structureId)) OR n.structureId = {structure} ";
			params = new JsonObject().put("structure", structureId);
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
			params = new JsonObject().put("structure", structureId);
		}
		String query =
				"MATCH (n:Role) " + filter +
				"OPTIONAL MATCH n-[r:AUTHORIZE]->a " +
				"RETURN n.id as id, n.name as name, n.distributions as distributions, COLLECT([a.name, a.displayName, a.type]) as actions";
		neo.execute(query, params, result -> handler.handle(addsDefaultRoleDistributions.apply(validResult(result))));
	}

	@Override
	public void listActions(String application, Handler<Either<String, JsonArray>> handler) {
		String query =
				"MATCH (n:Application)-[:PROVIDE]->a " +
				"WHERE n.name = {name} " +
				"RETURN a.name as name, a.displayName as displayName, a.type as type";
		neo.execute(query, new JsonObject().put("name", application), validResultHandler(handler));
	}

	@Override
	public void listGroupsWithRoles(String structureId, boolean classGroups, Handler<Either<String, JsonArray>> handler) {
		String query;
		JsonObject params = new JsonObject();
		if (structureId != null && !structureId.trim().isEmpty()) {
			String filter = classGroups ? "<-[:BELONGS*0..1]-()" : "";
			params.put("structureId", structureId);
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
			params.put("structure", structureId);
		}
		String query =
				"MATCH (n:Application) " + filter +
				"OPTIONAL MATCH n-[r:PROVIDE]->(a:Action) ";
		if (actionType != null &&
				("WORKFLOW".equals(actionType) || "RESOURCE".equals(actionType))) {
			query += "WHERE r IS NULL OR a.type = {actionType} ";
			params.put("actionType", "SECURED_ACTION_" + actionType);
		}
		query += "RETURN n.id as id, n.name as name, COLLECT([a.name, a.displayName, a.type]) as actions";
		neo.execute(query, params, validResultHandler(handler));
	}

	@Override
	public void listApplicationRolesWithGroups(String structureId, String appId, Handler<Either<String, JsonArray>> handler) {
		String query =
				"MATCH (a:Application {id: {appId}})-[:PROVIDE]->(:Action)<-[:AUTHORIZE]-(r:Role) " +
				"WITH r,a " +
				"MATCH (r)-[:AUTHORIZE]->(:Action)<-[:PROVIDE]-(apps:Application) " +
				"    OPTIONAL MATCH (s:Structure {id: {structureId}})<-[:DEPENDS*1..2]-(g:Group)-[:AUTHORIZED]->(r) " +
				"    WITH r, a, apps, CASE WHEN g IS NOT NULL THEN COLLECT(DISTINCT{ id: g.id, name: g.name }) ELSE [] END as groups " +
				"RETURN r.id as id, r.name as name, r.distributions as distributions, a.id as appId, groups, COUNT(DISTINCT apps) > 1 as transverse";
		JsonObject params = new JsonObject()
			.put("appId", appId)
			.put("structureId", structureId);
		neo.execute(query, params, result -> handler.handle(addsDefaultRoleDistributions.apply(validResult(result))));
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
			role.put("structureId", structureId);
		}
		JsonObject params = new JsonObject()
				.put("actions", actions)
				.put("role", role.put("id", UUID.randomUUID().toString()))
				.put("roleName", role.getString("name"));
		neo.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void updateRole(String roleId, JsonObject role, JsonArray actions,
			Handler<Either<String, JsonObject>> handler) {
		if (defaultValidationParamsNull(handler, roleId, role, actions)) return;
		role.remove("id");
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
		role.put("actions", actions).put("roleId", roleId);
		neo.execute(query, role, validUniqueResultHandler(handler));
	}

	@Override
	public void deleteRole(String roleId, Handler<Either<String, JsonObject>> handler) {
		String query =
				"MATCH (role:Role {id : {id}}) " +
				"OPTIONAL MATCH role-[r]-() " +
				"DELETE role, r ";
		neo.execute(query, new JsonObject().put("id", roleId), validUniqueResultHandler(handler));
	}

	@Override
	public void linkRolesToGroup(String groupId, JsonArray roleIds, Handler<Either<String, JsonObject>> handler) {
		JsonObject params = new JsonObject();
		params.put("groupId", groupId);
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
				s.add(createQuery, params.copy().put("roles", roleIds));
				neo.executeTransaction(s.build(), null, true, validEmptyHandler(handler));
			}
		} else {
			handler.handle(new Either.Left<String, JsonObject>("invalid.arguments"));
		}
	}

	@Override
	public void addGroupLink(String groupId, String roleId, Handler<Either<String, JsonObject>> handler) {
		JsonObject params = new JsonObject();
		params.put("groupId", groupId);
		params.put("roleId", roleId);
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
		params.put("groupId", groupId);
		params.put("roleId", roleId);
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
		application.put("scope", new fr.wseduc.webutils.collections.JsonArray("[\"" +
				application.getString("scope", "").replaceAll("\\s", "\",\"") + "\"]"));
		application.put("id", id);
		final String createApplicationQuery =
				"MATCH (n:Application) " +
				"WHERE n.name = {applicationName} " +
				"WITH count(*) AS exists " +
				"WHERE exists=0 " +
				"CREATE (m:Application {props}) " +
				"RETURN m.id as id";
		final JsonObject params = new JsonObject().put("applicationName", applicationName);
		if (structureId != null && !structureId.trim().isEmpty()) {
			application.put("structureId", structureId);
		}
		if (application.getJsonArray("levelsOfEducation") == null) {
			application.put("levelsOfEducation", defaultLevelsOfEducation);
		}
		final StatementsBuilder b = new StatementsBuilder()
				.add(createApplicationQuery, params.copy().put("props", application));
		if (actions != null && actions.size() > 0) {
			for (Object o: actions) {
				JsonObject json = (JsonObject) o;
				String type;
				List<String> removeLabels = new ArrayList<>();
				removeLabels.add("ResourceAction");
				removeLabels.add("AuthenticatedAction");
				removeLabels.add("WorkflowAction");
				switch (json.getString("type", "WORKFLOW")) {
					case "RESOURCE" : type = "Resource"; break;
					case "AUTHENTICATED" : type = "Authenticated"; break;
					default: type = "Workflow"; break;
				}
				removeLabels.remove(type + "Action");
				String createAction =
						"MERGE (a:Action {name:{name}}) " +
						"REMOVE a:" + Joiner.on(":").join(removeLabels) + " " +
						"SET a.displayName = {displayName}, a.type = {type}, a:" + type + "Action " +
						"WITH a " +
						"MATCH (n:Application) " +
						"WHERE n.name = {applicationName} " +
						"CREATE UNIQUE n-[r:PROVIDE]->a " +
						"RETURN a.name as name";
				b.add(createAction, json
						.put("applicationName", applicationName)
						.put("type", "SECURED_ACTION_" + json.getString("type", "WORKFLOW")));
			}
			final String removeNotWorkflowInRole =
					"MATCH (:Role)-[r:AUTHORIZE]->(a:Action) " +
					"WHERE a:ResourceAction OR a:AuthenticatedAction " +
					"DELETE r";
			b.add(removeNotWorkflowInRole);
		} else if (address != null && !address.trim().isEmpty()) {
			String query2 =
					"MATCH (n:Application) " +
					"WHERE n.id = {id} " +
					"CREATE UNIQUE n-[r:PROVIDE]->(a:Action:WorkflowAction {type: {type}, " +
					"name:{name}, displayName:{displayName}}) " +
					"RETURN a.name as name";
			b.add(query2, new JsonObject()
					.put("id", id)
					.put("type", "SECURED_ACTION_WORKFLOW")
					.put("name", applicationName + "|address")
					.put("displayName", applicationName + ".address"));
		}
		neo.executeTransaction(b.build(), null, true, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				JsonArray results = m.body().getJsonArray("results");
				if ("ok".equals(m.body().getString("status")) && results != null) {
					JsonArray r = results.getJsonArray(0);
					JsonObject j;
					if (r.size() > 0) {
						j = r.getJsonObject(0);
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
				"n.scope as scope, n.pattern as pattern, n.casType as casType, n.levelsOfEducation as levelsOfEducation";
		JsonObject params = new JsonObject()
				.put("id", applicationId);
		neo.execute(query,params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				JsonArray r = res.body().getJsonArray("result");
				if (r != null && r.size() == 1) {
					JsonObject j = r.getJsonObject(0);
					JsonArray scope = j.getJsonArray("scope");
					if (scope != null && scope.size() > 0) {
						j.put("scope", Joiner.on(" ").join(scope));
					} else {
						j.put("scope", "");
					}
				}
				Either<String, JsonObject> result = validUniqueResult(res);
				if (result.isRight()) {
					JsonObject app = result
							.right()
							.getValue();
					app.put("levelsOfEducation", app.getJsonArray("levelsOfEducation", defaultLevelsOfEducation));
					result = new Either.Right<>(app);
				}
				handler.handle(result);
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
		application.put("applicationId", applicationId);
		application.put("scope", new fr.wseduc.webutils.collections.JsonArray("[\"" +
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
		JsonObject params = new JsonObject().put("id", applicationId);
		neo.execute(query, params, validEmptyHandler(handler));
	}

	@Override
	public void setLevelsOfEducation(String applicationId, List<Integer> levelsOfEducations, Handler<Either<String, JsonObject>> handler) {
		String query = "MATCH (a:Application { id : {applicationId}}) " +
				"SET a.levelsOfEducation = {levelsOfEducation} " +
				"RETURN a.id as id, a.levelsOfEducation as levelsOfEducation";

		JsonObject params = new JsonObject()
				.put("applicationId", applicationId)
				.put("levelsOfEducation", levelsOfEducations);

		neo.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void setRoleDistributions(String roleId, List<String> distributions, Handler<Either<String, JsonObject>> handler) {
		String query = "MATCH (r:Role { id : {roleId}}) " +
				"SET r.distributions = {distributions} " +
				"RETURN r.id as id, r.distributions as distributions";

		JsonObject params = new JsonObject()
				.put("roleId", roleId)
				.put("distributions", distributions);

		neo.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void applicationAllowedUsers(String application, JsonArray users, JsonArray groups,
			Handler<Either<String, JsonArray>> handler) {
			JsonObject params = new JsonObject().put("application", application);
			String filter = "";
			if (users != null) {
				filter += "AND u.id IN {users} ";
				params.put("users", users);
			}
			if (groups != null) {
				filter += "AND pg.id IN {groups} ";
				params.put("groups", groups);
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
		neo.execute(query, new JsonObject().put("application", application), validResultHandler(handler));
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
		final JsonObject params = new JsonObject().put("id", classId);
		final String widgetQuery =
				"MATCH (c:Class { id : {id}})<-[:DEPENDS]-(csg:ProfileGroup)-[:DEPENDS]->(ssg:ProfileGroup), (w:Widget) " +
				"WHERE w.default = true " +
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
					JsonArray patterns = app.getJsonArray("patterns", new fr.wseduc.webutils.collections.JsonArray());
					if(patterns.size() == 0 || patterns.size() > 0 && patterns.getString(0).isEmpty()){
						final URL addressURL = checkCasUrl(address);

						if (addressURL != null) {
							String pattern = "^\\Q" + addressURL.getProtocol() + "://" + addressURL.getHost() + (addressURL.getPort() > 0 ? ":" + addressURL.getPort() : "") + "\\E.*";
							patterns.add(pattern);
						} else {
							log.error("Url for registered service : " + app.getString("service", "") + " is malformed : " + address);
						}
					}
				}
				handler.handle(new Either.Right<String, JsonArray>(results));
			}
		}));
	}

	public static URL checkCasUrl(final String address) {
		URL addressURL = null;

		if (!StringUtils.isEmpty(address)) {
			try {
				String finalAddress = "";
				if (address.startsWith("/adapter#")) {
					finalAddress = address.substring(address.indexOf("#") + 1);
				} else if (address.startsWith("/cas/login?service=")) {
					final String urlEncoded = address.substring(address.indexOf("=") + 1);
					final String urlDecoded = URLDecoder.decode(urlEncoded, "UTF-8");
					//check url is encoded
					if (!StringUtils.isEmpty(urlDecoded) && !urlDecoded.equals(urlEncoded)) {
						finalAddress = urlDecoded;
					}
				} else {
					finalAddress = address;
				}

				if (!StringUtils.isEmpty(finalAddress)) {
					final Matcher matcher = URL_PATTERN.matcher(finalAddress);

					if (matcher.matches()) {
						addressURL = new URL(finalAddress);
					}
				}
			} catch (MalformedURLException | UnsupportedEncodingException | IllegalArgumentException e) {
				if (log.isDebugEnabled()) log.debug("address of external CAS app is malformed", e);
			}
		}

		return (addressURL != null && !StringUtils.isEmpty(addressURL.getHost())) ? addressURL : null;
	}

	@Override
	public void massAuthorize(String structureId, List<String> profiles, List<String> rolesId, Handler<Either<String, JsonObject>> handler) {
		String query =
				"MATCH (r:Role), " +
						"(parentStructure:Structure {id: {structureId}})<-[:HAS_ATTACHMENT*0..]-(s:Structure)<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
						"WHERE r.id IN {roleIds} " +
						"AND p.name IN {profiles} AND NOT(g-[:AUTHORIZED]->r) " +
						"CREATE UNIQUE g-[:AUTHORIZED]->r";
		executeQueryForStructureProfilesAndRoles(query, structureId, profiles, rolesId, handler);
	}

	@Override
	public void massUnauthorize(String structureId, List<String> profiles, List<String> rolesId, Handler<Either<String, JsonObject>> handler) {
		String query =
				"MATCH (r:Role), " +
						"(parentStructure:Structure {id: {structureId}})<-[:HAS_ATTACHMENT*0..]-(s:Structure)<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
						"WHERE r.id IN {roleIds} " +
						"AND p.name IN {profiles} " +
						"MATCH r<-[auth:AUTHORIZED]-g " +
						"DELETE auth";
		executeQueryForStructureProfilesAndRoles(query, structureId, profiles, rolesId, handler);
	}

	private void executeQueryForStructureProfilesAndRoles(String query, String structureId, List<String> profiles, List<String> rolesId, Handler<Either<String, JsonObject>> handler) {
		JsonObject params = new JsonObject()
				.put("structureId", structureId)
				.put("profiles", new fr.wseduc.webutils.collections.JsonArray(profiles))
				.put("roleIds", new fr.wseduc.webutils.collections.JsonArray(rolesId));
		neo.execute(query, params, validEmptyHandler(handler));
	}
}
