/*
 * Copyright © "Open Digital Education", 2014
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

package org.entcore.directory.services.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.collections.Joiner;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;
import org.entcore.directory.Directory;
import org.entcore.directory.services.GroupService;

import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.neo4j.Neo4jResult.*;
import static org.entcore.common.user.DefaultFunctions.ADMIN_LOCAL;
import static org.entcore.common.user.DefaultFunctions.SUPER_ADMIN;

public class DefaultGroupService implements GroupService {

	private final Neo4j neo = Neo4j.getInstance();
	private final EventBus eventBus;

	public DefaultGroupService(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	@Override
	public void listAdmin(String structureId, Boolean onlyAutomaticGroups, Boolean recursive, UserInfos userInfos, JsonArray expectedTypes,
			Handler<Either<String, JsonArray>> results) {
		if (userInfos == null) {
			results.handle(new Either.Left<String, JsonArray>("invalid.user"));
			return;
		}
		String recursion = "";
		if(recursive.booleanValue() == true)
		{
			recursion = "<-[:HAS_ATTACHMENT*0..]-(:Structure)";
		}

		String condition;
		if (expectedTypes != null && expectedTypes.size() > 0) {
			for (Object groupType: expectedTypes) {
				if (!GROUP_TYPES.contains(groupType)) {
					results.handle(new Either.Left<>("invalid.group.type"));
					return;
				}
			}
			condition = "WHERE (g:" + Joiner.on(" OR g:").join(expectedTypes) + ") ";
		} else {
			condition = "WHERE g:Group ";
		}
		if(onlyAutomaticGroups.booleanValue() == true)
			condition += " AND EXISTS(g.filter) ";

		JsonObject params = new JsonObject();
		if (!userInfos.getFunctions().containsKey(SUPER_ADMIN) &&
				!userInfos.getFunctions().containsKey(ADMIN_LOCAL)) {
			results.handle(new Either.Left<String, JsonArray>("forbidden"));
			return;
		} else if (userInfos.getFunctions().containsKey(ADMIN_LOCAL) && !userInfos.getFunctions().containsKey(SUPER_ADMIN)) {
			UserInfos.Function f = userInfos.getFunctions().get(ADMIN_LOCAL);
			List<String> scope = f.getScope();
			if (scope != null && !scope.isEmpty()) {
				condition += "AND s.id IN {structures} ";
				params.put("structures", new JsonArray(scope));
			}
		}

		if (structureId != null && !structureId.trim().isEmpty()) {
			condition += " AND s.id = {structure} ";
			params.put("structure", structureId);
		}
		String query =
				"MATCH (s:Structure)" + recursion + "<-[:BELONGS*0..1]-()<-[:DEPENDS]-(g) " + condition +
				"OPTIONAL MATCH (g)-[:DEPENDS]->(c:Class) " +
				"WITH g, collect({name: c.name, id: c.id}) as classes, collect( distinct {name: s.name, id: s.id}) as structures, " +
				"HEAD(filter(x IN labels(g) WHERE x <> 'Visible' AND x <> 'Group')) as type " +
				"RETURN DISTINCT g.id as id, g.name as name, g.displayName as displayName, g.filter as filter, labels(g) as labels, " +
				"g.createdAt as createdAt, g.createdByName as createdByName, g.modifiedAt as modifiedAt, g.modifiedByName as modifiedByName, " +
				"g.autolinkTargetAllStructs as autolinkTargetAllStructs, g.autolinkTargetStructs as autolinkTargetStructs," +
				"g.autolinkUsersFromGroups as autolinkUsersFromGroups, type, g.users as internalCommunicationRule, "+
				"g.lockDelete AS lockDelete, coalesce(g.nbUsers,0) as nbUsers, " +
				"CASE WHEN any(x in classes where x <> {name: null, id: null}) THEN classes END as classes," +
				"CASE WHEN any(x in structures where x <> {name: null, id: null}) THEN structures END as structures, " +
				"CASE WHEN (g: ProfileGroup)-[:DEPENDS]-(:Structure) THEN 'StructureGroup' " +
					" WHEN (g: ProfileGroup)-[:DEPENDS]->(:Class) THEN 'ClassGroup' " +
					" WHEN HAS(g.subType) THEN g.subType " +
					" WHEN (g: ManualGroup) AND (" +
						" g.autolinkTargetAllStructs = true " +
						" OR size(coalesce(g.autolinkUsersFromGroups, [])) > 0 " +
						" OR size(coalesce(g.autolinkTargetStructs, [])) > 0 " +
				 	") THEN 'BroadcastGroup' " +
				"END as subType";
		neo.execute(query, params, validResultHandler(results));
	}

	@Override
	public void createOrUpdateManual(JsonObject group, String structureId, String classId,
			Handler<Either<String, JsonObject>> result) {
		group.put("groupDisplayName", group.getString("name"));
		JsonObject action = new JsonObject()
				.put("action", "manual-create-group")
				.put("structureId", structureId)
				.put("classId", classId)
				.put("group", group);
		eventBus.request(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(0, result)));
	}

	@Override
	public void deleteManual(String groupId, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-delete-group")
				.put("groupId", groupId);
		eventBus.request(Directory.FEEDER, action, handlerToAsyncHandler(validEmptyHandler(result)));
	}

	@Override
	public void list(String structureId, String type, boolean subGroups, Handler<Either<String, JsonArray>> results) {
		String condition = "";
		JsonObject params = new JsonObject();
		if (structureId != null && !structureId.trim().isEmpty()) {
			condition = "WHERE s.id = {structureId} ";
			params.put("structureId", structureId);
		}
		if (type == null || type.trim().isEmpty()) {
			type = "Group";
		} else if (!GROUP_TYPES.contains(type)) {
			results.handle(new Either.Left<>("invalid.group.type"));
			return;
		}
		String sub = "";
		if (subGroups) {
			sub = "*1..2";
		}
		String query =
				"MATCH (s:Structure)<-[:DEPENDS" + sub + "]-(g:" + type + ") " + condition +
				"RETURN g.id as id, g.name as name, g.displayName as displayName ";
		neo.execute(query, params, validResultHandler(results));
	}
	
	@Override
	public void addUsers(String groupId, JsonArray userIds, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-add-group-users")
				.put("groupId", groupId)
				.put("userIds", userIds);
		eventBus.request(Directory.FEEDER, action, handlerToAsyncHandler(validEmptyHandler(result)));
	}
	
	@Override
	public void removeUsers(String groupId, JsonArray userIds, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-remove-group-users")
				.put("groupId", groupId)
				.put("userIds", userIds);
		eventBus.request(Directory.FEEDER, action, handlerToAsyncHandler(validEmptyHandler(result)));
	}

	@Override
	public void getInfos(String groupId, Handler<Either<String, JsonObject>> handler) {
		final String query =
				"MATCH (g:Group {id:{id}}) " +
				"RETURN g.id as id, g.name as name, g.nbUsers as nbUsers ";
		final JsonObject params = new JsonObject().put("id", groupId);
		neo.execute(query, params, validUniqueResultHandler(handler));
	}

	@Override
	public void getFuncAndDisciplinesGroups(String structureId, Boolean recursive, UserInfos userInfos, Handler<Either<String, JsonArray>> results) {
		if (userInfos == null) {
			results.handle(new Either.Left<String, JsonArray>("invalid.user"));
			return;
		}

		String recursion = "";
		if (recursive.booleanValue() == true) {
			recursion = "<-[:HAS_ATTACHMENT*0..]-(:Structure)";
		}

		String structureCondition = "";
		JsonObject params = new JsonObject();
		if (!userInfos.getFunctions().containsKey(SUPER_ADMIN) &&
				!userInfos.getFunctions().containsKey(ADMIN_LOCAL)) {
			results.handle(new Either.Left<String, JsonArray>("forbidden"));
			return;
		} else if (userInfos.getFunctions().containsKey(ADMIN_LOCAL) && !userInfos.getFunctions().containsKey(SUPER_ADMIN)) {
			UserInfos.Function f = userInfos.getFunctions().get(ADMIN_LOCAL);
			List<String> scope = f.getScope();
			if (scope != null && !scope.isEmpty()) {
				structureCondition += " AND s.id IN {structures} ";
				params.put("structures", new JsonArray(scope));
			}
		}

		if (structureId != null && !structureId.trim().isEmpty()) {
			structureCondition += " AND s.id = {structure} ";
			params.put("structure", structureId);
		}

		String query =
				"MATCH (s:Structure)" + recursion + "<-[:BELONGS*0..1]-()<-[:DEPENDS]-(g:Group) " +
				"WHERE EXISTS(g.filter) " + structureCondition + " AND ('FuncGroup' in labels(g) OR 'DisciplineGroup' in labels(g)) " +
				"RETURN g.id as id, g.name as name, g.displayName as displayName, g.filter as filter, labels(g) as labels";
		neo.execute(query, params, validResultHandler(results));
	}

	/**
	 * This method is used to get the community group
	 * if the structureId is not null, it will get the community group of the structure
	 * else it will get all the community group
	 * The community group is not a group dependant of a structure, it is a group that can be used to communicate with all the users of the platform
	 * for this reason, it is not dependant of a structure
	 *
	 * @param structureId : the structure id
	 * @param results     : the result handler
	 */
	@Override
	public void getCommunityGroup(String structureId, Handler<Either<String, JsonArray>> results) {
		String query = "";
		String returnStructure = "";
		JsonObject params = new JsonObject();

		if (structureId != null && !structureId.trim().isEmpty()) {
			query = "MATCH (s:Structure)<-[:DEPENDS]-(pg:ProfileGroup)<-[:IN]-(u:User), u-[:IN]->(g:CommunityGroup) WHERE s.id = {structureId} WITH g, collect( distinct {name: s.name, id: s.id}) as structures, ";
			returnStructure = ", CASE WHEN any(x in structures where x <> {name: null, id: null}) THEN structures END as structures";
			params.put("structureId", structureId);
		} else {
			query = "MATCH (g:CommunityGroup) WITH g, ";
		}

		query = query +
				"HEAD(filter(x IN labels(g) WHERE x <> 'Visible' AND x <> 'Group')) as type " +
				"RETURN DISTINCT g.id as id, g.name as name, g.displayName as displayName, g.filter as filter, labels(g) as labels, type, " +
				"g.lockDelete AS lockDelete, coalesce(g.nbUsers,0) as nbUsers" +
				returnStructure;

		neo.execute(query, params, validResultHandler(results));
	}
}
