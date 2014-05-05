/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.directory.services.impl;

import fr.wseduc.webutils.Either;
import org.entcore.common.neo4j.Neo;
import org.entcore.directory.Directory;
import org.entcore.directory.services.SchoolService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;

public class DefaultSchoolService implements SchoolService {

	private final Neo neo;
	private final EventBus eventBus;

	public DefaultSchoolService(Neo neo, EventBus eventBus) {
		this.neo = neo;
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

}
