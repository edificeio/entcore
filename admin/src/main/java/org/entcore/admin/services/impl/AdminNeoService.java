package org.entcore.admin.services.impl;
import org.entcore.admin.services.AdminService;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.webutils.Either;

public class AdminNeoService implements AdminService {

	private final Neo4j neo = Neo4j.getInstance();

	@Override
	public void quickSearchUsers(String structureId, String input, Handler<Either<String, JsonArray>> handler) {
		String query =
			"MATCH (u:User)-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
			"WHERE s.id = {id} " +
			"AND u.displayName =~ {inputRegExp} " +
			"RETURN distinct u.id as id, u.firstName as firstName, u.lastName as lastName " +
			"ORDER BY u.lastName";
		String inputRegExp = "(?i).*" + input.trim() + ".*";
		JsonObject params = new JsonObject()
				.put("id", structureId)
				.put("inputRegExp", inputRegExp);
		neo.execute(query, params, Neo4jResult.validResultHandler(handler));
	}

	@Override
	public void userList(String structureId, Handler<Either<String, JsonArray>> handler) {
		String query =
			"MATCH (u: User)-[:IN]->(pg: ProfileGroup)-[:DEPENDS]->(s: Structure) " +
			"WHERE s.id = {structureId} " +
			"MATCH (pg)-[:HAS_PROFILE]->(p: Profile) " +
			"OPTIONAL MATCH (u)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(class: Class) " +
			"OPTIONAL MATCH (u)-[d: DUPLICATE]-(duplicate: User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(sd: Structure) " +
			"OPTIONAL MATCH (u)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(struct: Structure) " +
			"OPTIONAL MATCH (u)-[:IN]->(fgroup: FunctionalGroup) " +
			"OPTIONAL MATCH (u)-[:IN]->(mgroup: ManualGroup) " +
			"WITH u, p, class, fgroup, mgroup, struct, duplicate, d, collect(DISTINCT {id: sd.id, name: sd.name}) as structuresDup " +
			"RETURN DISTINCT " +
			"u.id as id, p.name as type, u.activationCode as code, u.login as login," +
			"u.firstName as firstName, u.lastName as lastName, u.displayName as displayName," +
			"u.source as source, u.deleteDate as deleteDate, u.disappearanceDate as disappearanceDate, u.blocked as blocked," +
			"EXTRACT(function IN u.functions | last(split(function, \"$\"))) as aafFunctions," +
			"CASE WHEN class IS NULL THEN [] " +
			"ELSE COLLECT(distinct {id: class.id, name: class.name}) END as classes," +
			"CASE WHEN fgroup IS NULL THEN [] " +
			"ELSE COLLECT(distinct fgroup.name) END as functionalGroups, " +
			"CASE WHEN mgroup IS NULL THEN [] " +
			"ELSE COLLECT(distinct mgroup.name) END as manualGroups, " +
			"CASE WHEN duplicate IS NULL THEN [] " +
			"ELSE COLLECT(distinct { id: duplicate.id, firstName: duplicate.firstName, lastName: duplicate.lastName, score: d.score, code: duplicate.activationCode, structures: structuresDup }) END as duplicates, " +
			"COLLECT (distinct {id: struct.id, name: struct.name}) as structures " +
			"ORDER BY lastName, firstName " +
			"UNION " +
			"MATCH (u: User)-[:HAS_RELATIONSHIPS]->(b: Backup) " +
			"WHERE {structureId} IN b.structureIds " +
			"MATCH (s: Structure) " +
			"WHERE s.id IN b.structureIds " +
			"WITH u, b, s " +
			"RETURN DISTINCT u.id as id, u.profiles[0] as type, u.activationCode as code, u.login as login, u.firstName as firstName, " +
			"u.lastName as lastName, u.displayName as displayName,u.source as source, u.deleteDate as deleteDate, u.disappearanceDate as disappearanceDate, u.blocked as blocked, " +
			"[] as aafFunctions, [] as classes, [] as functionalGroups, [] as manualGroups, [] as duplicates, " +
			"COLLECT(distinct {id: s.id, name: s.name}) as structures " +
			"ORDER BY lastName, firstName ";

		JsonObject params = new JsonObject().put("structureId", structureId);
		neo.execute(query, params, Neo4jResult.validResultHandler(handler));
	}
}