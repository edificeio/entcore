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
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;
import org.entcore.directory.Directory;
import org.entcore.directory.services.GroupService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.neo4j.Neo4jResult.validEmptyHandler;
import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;
import static org.entcore.common.user.DefaultFunctions.ADMIN_LOCAL;
import static org.entcore.common.user.DefaultFunctions.SUPER_ADMIN;

public class DefaultGroupService implements GroupService {

	private final Neo4j neo = Neo4j.getInstance();
	private final EventBus eventBus;

	public DefaultGroupService(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	@Override
	public void listAdmin(String structureId, UserInfos userInfos, JsonArray expectedTypes,
			Handler<Either<String, JsonArray>> results) {
		if (userInfos == null) {
			results.handle(new Either.Left<String, JsonArray>("invalid.user"));
			return;
		}
		String condition;
		if (expectedTypes != null && expectedTypes.size() > 0) {
			condition = "WHERE (g:" + Joiner.on(" OR g:").join(expectedTypes) + ") ";
		} else {
			condition = "WHERE g:Group ";
		}

		JsonObject params = new JsonObject();
		if (!userInfos.getFunctions().containsKey(SUPER_ADMIN) &&
				!userInfos.getFunctions().containsKey(ADMIN_LOCAL)) {
			results.handle(new Either.Left<String, JsonArray>("forbidden"));
			return;
		} else if (userInfos.getFunctions().containsKey(ADMIN_LOCAL)) {
			UserInfos.Function f = userInfos.getFunctions().get(ADMIN_LOCAL);
			List<String> scope = f.getScope();
			if (scope != null && !scope.isEmpty()) {
				condition += "AND s.id IN {structures} ";
				params.put("structures", new fr.wseduc.webutils.collections.JsonArray(scope));
			}
		}

		if (structureId != null && !structureId.trim().isEmpty()) {
			condition += " AND s.id = {structure} ";
			params.put("structure", structureId);
		}
		String query =
				"MATCH (s:Structure)<-[:BELONGS*0..1]-()<-[:DEPENDS]-(g) " + condition +
				"OPTIONAL MATCH (s)<-[:BELONGS]-(c: Class)<-[:DEPENDS]-(g) " +
				"WITH g, collect({name: c.name, id: c.id}) as classes, " +
				"HEAD(filter(x IN labels(g) WHERE x <> 'Visible' AND x <> 'Group')) as type " +
				"RETURN DISTINCT g.id as id, g.name as name, g.displayName as displayName, type, g.users as internalCommunicationRule, "+
				"CASE WHEN any(x in classes where x <> {name: null, id: null}) THEN classes END as classes," +
				"CASE WHEN (g: ProfileGroup)-[:DEPENDS]-(:Structure) THEN 'StructureGroup' END as subType";
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
		eventBus.send(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(0, result)));
	}

	@Override
	public void deleteManual(String groupId, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-delete-group")
				.put("groupId", groupId);
		eventBus.send(Directory.FEEDER, action, handlerToAsyncHandler(validEmptyHandler(result)));
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
		eventBus.send(Directory.FEEDER, action, handlerToAsyncHandler(validEmptyHandler(result)));
	}
	
	@Override
	public void removeUsers(String groupId, JsonArray userIds, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-remove-group-users")
				.put("groupId", groupId)
				.put("userIds", userIds);
		eventBus.send(Directory.FEEDER, action, handlerToAsyncHandler(validEmptyHandler(result)));
	}

	@Override
	public void getInfos(String groupId, Handler<Either<String, JsonObject>> handler) {
		final String query =
				"MATCH (g:Group {id:{id}}) " +
				"RETURN g.id as id, g.name as name, g.nbUsers as nbUsers ";
		final JsonObject params = new JsonObject().put("id", groupId);
		neo.execute(query, params, validUniqueResultHandler(handler));
	}

}
