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

package org.entcore.directory.services.impl;

import fr.wseduc.webutils.Either;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;
import org.entcore.directory.Directory;
import org.entcore.directory.services.SchoolService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;
import static org.entcore.common.user.DefaultFunctions.ADMIN_LOCAL;
import static org.entcore.common.user.DefaultFunctions.SUPER_ADMIN;

public class DefaultSchoolService implements SchoolService {

	private final Neo4j neo = Neo4j.getInstance();
	private final EventBus eventBus;

	public DefaultSchoolService(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	@Override
	public void create(JsonObject school, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject().putString("action", "manual-create-structure")
				.putObject("data", school);
		eventBus.send(Directory.FEEDER, action, validUniqueResultHandler(result));
	}

	@Override
	public void get(String id, Handler<Either<String, JsonObject>> result) {
		String query = "match (s:`Structure`) where s.id = {id} return s.id as id, s.UAI as UAI, s.name as name";
		neo.execute(query, new JsonObject().putString("id", id), validUniqueResultHandler(result));
	}

	@Override
	public void getByClassId(String classId, Handler<Either<String, JsonObject>> result) {
		String query =
				"match (c:`Class` {id : {id}})-[:BELONGS]->(s:`Structure`) " +
				"return s.id as id, s.UAI as UAI, s.name as name";
		neo.execute(query, new JsonObject().putString("id", classId), validUniqueResultHandler(result));
	}

	@Override
	public void listByUserId(String userId, Handler<Either<String, JsonArray>> results) {
		String query =
				"MATCH (u:User { id: {id}})-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
				"RETURN DISTINCT s.id as id, s.UAI as UAI, s.name as name ";
		neo.execute(query, new JsonObject().putString("id", userId), validResultHandler(results));
	}

	@Override
	public void listAdmin(UserInfos userInfos, Handler<Either<String, JsonArray>> results) {
		if (userInfos == null) {
			results.handle(new Either.Left<String, JsonArray>("invalid.user"));
			return;
		}
		String condition = "";
		JsonObject params = new JsonObject();
		if (!userInfos.getFunctions().containsKey(SUPER_ADMIN) && !userInfos.getFunctions().containsKey(ADMIN_LOCAL)) {
			results.handle(new Either.Left<String, JsonArray>("forbidden"));
			return;
		} else if (userInfos.getFunctions().containsKey(ADMIN_LOCAL)) {
			UserInfos.Function f = userInfos.getFunctions().get(ADMIN_LOCAL);
			List<String> structuresIds = f.getStructures();
			if (structuresIds != null && !structuresIds.isEmpty()) {
				condition = "WHERE s.id IN {structures} ";
				params.putArray("structures", new JsonArray(structuresIds.toArray()));
			}
		}
		String query =
				"MATCH (s:Structure) " + condition +
				"RETURN s.id as id, s.UAI as UAI, s.name as name ";
		neo.execute(query, params, validResultHandler(results));
	}

	@Override
	public void link(String structureId, String userId, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.putString("action", "manual-add-user")
				.putString("structureId", structureId)
				.putString("userId", userId);
		eventBus.send(Directory.FEEDER, action, validUniqueResultHandler(result));
	}

	@Override
	public void unlink(String structureId, String userId, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.putString("action", "manual-remove-user")
				.putString("structureId", structureId)
				.putString("userId", userId);
		eventBus.send(Directory.FEEDER, action, validUniqueResultHandler(result));
	}

	@Override
	public void defineParent(String structureId, String parentStructureId, Handler<Either<String, JsonObject>> handler) {
		JsonObject action = new JsonObject()
				.putString("action", "manual-structure-attachment")
				.putString("structureId", structureId)
				.putString("parentStructureId", parentStructureId);
		eventBus.send(Directory.FEEDER, action, validUniqueResultHandler(0, handler));
	}

}
