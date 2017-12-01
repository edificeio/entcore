/*
 * Copyright © WebServices pour l'Éducation, 2014
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
				params.put("structures", new JsonArray(scope));
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
				"RETURN DISTINCT g.id as id, g.name as name, g.displayName as displayName, type, "+
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

}
