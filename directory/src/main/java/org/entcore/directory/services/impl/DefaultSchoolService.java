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

package org.entcore.directory.services.impl;

import fr.wseduc.webutils.Either;

import io.vertx.core.eventbus.Message;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.common.user.UserInfos;
import org.entcore.directory.Directory;
import org.entcore.directory.services.SchoolService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collector;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.neo4j.Neo4jResult.*;
import static org.entcore.common.user.DefaultFunctions.ADMIN_LOCAL;
import static org.entcore.common.user.DefaultFunctions.CLASS_ADMIN;
import static org.entcore.common.user.DefaultFunctions.SUPER_ADMIN;

public class DefaultSchoolService implements SchoolService {
	private final int firstLevel = 1;
	private final int secondLevel = 2;
	private final JsonArray defaultStructureLevelsOfEducation = new JsonArray()
			.add(firstLevel)
			.add(secondLevel);
	private final JsonArray defaultStructureDistributions = new JsonArray();

	private final Neo4j neo = Neo4j.getInstance();
	private final EventBus eventBus;

	public DefaultSchoolService(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	@Override
	public void create(JsonObject school, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject().put("action", "manual-create-structure")
				.put("data", school);
		eventBus.send(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(result)));
	}

	@Override
	public void get(String id, Handler<Either<String, JsonObject>> result) {
		String query =
				"match (s:`Structure`) where s.id = {id} " +
				"return s.id as id, s.externalId as externalId, s.UAI as UAI, s.name as name";
		neo.execute(query, new JsonObject().put("id", id), validUniqueResultHandler(result));
	}

	@Override
	public void getByClassId(String classId, Handler<Either<String, JsonObject>> result) {
		String query =
				"match (c:`Class` {id : {id}})-[:BELONGS]->(s:`Structure`) " +
				"return s.id as id, s.UAI as UAI, s.name as name, s.externalId as externalId ";
		neo.execute(query, new JsonObject().put("id", classId), validUniqueResultHandler(result));
	}

	@Override
	public void listByUserId(String userId, Handler<Either<String, JsonArray>> results) {
		String query =
				"MATCH (u:User { id: {id}})-[:IN]->(g: Group)-[:DEPENDS]->(s:Structure) " +
				"OPTIONAL MATCH (s)-[r:HAS_ATTACHMENT]->(ps:Structure) " +
				"WITH s, COLLECT(DISTINCT {id: ps.id, name: ps.name}) as parents " +
				"RETURN DISTINCT s.id as id, s.UAI as UAI, s.name as name, " +
				"CASE WHEN any(p in parents where p <> {id: null, name: null}) THEN parents END as parents";
		neo.execute(query, new JsonObject().put("id", userId), validResultHandler(results));
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
		} else if (!userInfos.getFunctions().containsKey(SUPER_ADMIN) && userInfos.getFunctions().containsKey(ADMIN_LOCAL)) {
			UserInfos.Function f = userInfos.getFunctions().get(ADMIN_LOCAL);
			List<String> scope = f.getScope();
			if (scope != null && !scope.isEmpty()) {
				condition = "WHERE s.id IN {structures} ";
				params.put("structures", new fr.wseduc.webutils.collections.JsonArray(scope));
			}
		}
		String query =
				"MATCH (s:Structure) " + condition +
				"OPTIONAL MATCH (s)-[r:HAS_ATTACHMENT]->(ps:Structure) " +
				"WITH s, COLLECT({id: ps.id, name: ps.name}) as parents " +
				"RETURN s.id as id, s.UAI as UAI, s.name as name, s.externalId as externalId, s.timetable as timetable, " +
				"s.hasApp as hasApp, s.levelsOfEducation as levelsOfEducation, s.distributions as distributions, " +
				"CASE WHEN any(p in parents where p <> {id: null, name: null}) THEN parents END as parents";

		neo.execute(query, params, result -> {
			Either<String, JsonArray> resultAsArray = validResult(result);
			if(resultAsArray.isRight()) {
				JsonArray structures = resultAsArray
						.right()
						.getValue()
						.stream()
						.map(JsonObject.class::cast)
						.map(structure -> structure
								.put("levelsOfEducation", structure.getJsonArray("levelsOfEducation", defaultStructureLevelsOfEducation))
								.put("distributions", structure.getJsonArray("distributions", defaultStructureDistributions)))
						.collect(Collector.of(JsonArray::new, JsonArray::add, JsonArray::add));
				resultAsArray = new Either.Right<>(structures);
			}
			results.handle(resultAsArray);
		});
	}

	@Override
	public void link(String structureId, String userId, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-add-user")
				.put("structureId", structureId)
				.put("userId", userId);
		eventBus.send(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(result)));
	}

	@Override
	public void unlink(String structureId, String userId, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-remove-user")
				.put("structureId", structureId)
				.put("userId", userId);
		eventBus.send(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(result)));
	}

	@Override
	public void defineParent(String structureId, String parentStructureId, Handler<Either<String, JsonObject>> handler) {
		JsonObject action = new JsonObject()
				.put("action", "manual-structure-attachment")
				.put("structureId", structureId)
				.put("parentStructureId", parentStructureId);
		eventBus.send(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(0, handler)));
	}

	@Override
	public void removeParent(String structureId, String parentStructureId, Handler<Either<String, JsonObject>> handler) {
		JsonObject action = new JsonObject()
			.put("action", "manual-structure-detachment")
			.put("structureId", structureId)
			.put("parentStructureId", parentStructureId);
		eventBus.send(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(handler)));
	}

	@Override
	public void list(JsonArray fields, Handler<Either<String, JsonArray>> results) {
		if (fields == null || fields.size() == 0) {
			fields = new fr.wseduc.webutils.collections.JsonArray().add("id").add("externalId").add("name").add("UAI");
		}
		StringBuilder query = new StringBuilder();
		query.append("MATCH (s:Structure) RETURN ");
		for (Object field : fields) {
			query.append(" s.").append(field).append(" as ").append(field).append(",");
		}
		query.deleteCharAt(query.length() - 1);
		neo.execute(query.toString(), (JsonObject) null, validResultHandler(results));
	}

	@Override
	public void update(String structureId, JsonObject body, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-update-structure")
				.put("structureId", structureId)
				.put("data", body);
		eventBus.send(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(result)));
	}

	@Override
	public void getLevels(String structureId, UserInfos userInfos, Handler<Either<String, JsonArray>> results){
		String filter =
				"MATCH (s:Structure {id: {structureId}})<-[:DEPENDS]-(g:ProfileGroup)<-[:IN]-(u:User) ";
		String condition =
				"WHERE has(u.level) ";
		String filter2 =
				"MATCH u-[:IN]->(:ProfileGroup)-[:DEPENDS]->(class:Class)-[:BELONGS]->(n) "+
				"WITH distinct u.level as name, collect(distinct {id: class.id, name: class.name}) as classes "+
				"RETURN distinct name, classes";

		JsonObject params = new JsonObject().put("structureId", structureId);

		//Admin check
		if (!userInfos.getFunctions().containsKey(SUPER_ADMIN) &&
				!userInfos.getFunctions().containsKey(ADMIN_LOCAL) &&
				!userInfos.getFunctions().containsKey(CLASS_ADMIN)) {
			results.handle(new Either.Left<String, JsonArray>("forbidden"));
			return;
		} else if (userInfos.getFunctions().containsKey(ADMIN_LOCAL)) {
			UserInfos.Function f = userInfos.getFunctions().get(ADMIN_LOCAL);
			List<String> scope = f.getScope();
			if (scope != null && !scope.isEmpty()) {
				condition += "AND s.id IN {scope} ";
				params.put("scope", new fr.wseduc.webutils.collections.JsonArray(scope));
			}
		} else if(userInfos.getFunctions().containsKey(CLASS_ADMIN)){
			UserInfos.Function f = userInfos.getFunctions().get(CLASS_ADMIN);
			List<String> scope = f.getScope();
			if (scope != null && !scope.isEmpty()) {
				condition = "AND class.id IN {scope} ";
				params.put("scope", new fr.wseduc.webutils.collections.JsonArray(scope));
			}
		}

		String query = filter + condition + filter2;

		neo.execute(query.toString(), params, validResultHandler(results));
	}

	@Override
	public void setLevelsOfEducation(String structureId, List<Integer> levelsOfEducations, Handler<Either<String, JsonObject>> handler) {
		String query =
				"MATCH (s:Structure {id: {structureId}}) " +
				"SET s.levelsOfEducation = {levelsOfEducation} " +
				"RETURN s.id as id, s.levelsOfEducation as levelsOfEducation";

		JsonObject params = new JsonObject()
				.put("structureId", structureId)
				.put("levelsOfEducation", levelsOfEducations);

		neo.execute(query, params, validUniqueResultHandler(handler));
	}

    @Override
    public void setDistributions(String structureId, List<String> distributions, Handler<Either<String, JsonObject>> handler) {
        String query =
                "MATCH (s:Structure {id: {structureId}}) " +
                        "SET s.distributions = {distributions} " +
                        "RETURN s.id as id, s.distributions as distributions";

        JsonObject params = new JsonObject()
                .put("structureId", structureId)
                .put("distributions", distributions);

        neo.execute(query, params, validUniqueResultHandler(handler));
    }


	@Override
	public void listSources(String structureId, Handler<Either<String, JsonArray>> result) {
		String query = 
			"MATCH (u:User)-[:IN]->(pg: ProfileGroup)-[:DEPENDS]->(s:Structure) " +
			"WHERE s.id = {structureId} " + 
			"RETURN collect(distinct u.source) as sources";

		JsonObject params = new JsonObject().put("structureId", structureId);
		neo.execute(query, params, Neo4jResult.validResultHandler(result));
	}
	
	@Override
	public void listAafFunctions(String structureId, Handler<Either<String, JsonArray>> result) {
		String query = 
			"MATCH (u:User)-[:IN]->(pg: ProfileGroup)-[:DEPENDS]->(s:Structure) " +
			"WHERE s.id = {structureId} " + 
			"RETURN collect(DISTINCT EXTRACT(function IN u.functions | split(function, \"$\"))) as aafFunctions";

		JsonObject params = new JsonObject().put("structureId", structureId);
		neo.execute(query, params, Neo4jResult.validResultHandler(result));
	}

	@Override
	public void getMetrics(String structureId, Handler<Either<String, JsonObject>> results){
		String query = "MATCH (s:Structure) " +
				"WHERE s.id = {structureId} " +
				"MATCH (u:User)-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(s)," +
				"(pg)-[:HAS_PROFILE]->(p:Profile) " +
				"WITH p, collect(distinct u) as allUsers " +
				"WITH p, FILTER(u IN allUsers WHERE u.activationCode IS NULL) as active, " +
				"FILTER(u IN allUsers WHERE NOT(u.activationCode IS NULL)) as inactive " +
				"WITH p, length (active) as active, length(inactive) as inactive " +
				"RETURN collect({profile: p.name, active: active, inactive: inactive}) as metrics";

		JsonObject params = new JsonObject().put("structureId", structureId);

		neo.execute(query.toString(), params, validUniqueResultHandler(results));
	}
	
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
			"OPTIONAL MATCH (u)-[rf:HAS_FUNCTION]->()-[:CONTAINS_FUNCTION*0..1]->(f:Function) " +
			"WITH u, p, class, fgroup, mgroup, f, rf, struct, duplicate, d, collect(DISTINCT {id: sd.id, name: sd.name}) as structuresDup " +
			"RETURN DISTINCT " +
			"u.id as id, p.name as type, u.activationCode as code, u.login as login," +
			"u.firstName as firstName, u.lastName as lastName, u.displayName as displayName," +
			"u.source as source, u.deleteDate as deleteDate, u.disappearanceDate as disappearanceDate, u.blocked as blocked, u.created as creationDate, " +
			"EXTRACT(function IN u.functions | split(function, \"$\")) as aafFunctions," +
			"CASE WHEN class IS NULL THEN [] ELSE COLLECT(distinct {id: class.id, name: class.name, externalId : class.externalId}) END as classes," +
			"CASE WHEN fgroup IS NULL THEN [] ELSE COLLECT(distinct fgroup.name) END as functionalGroups, " +
			"CASE WHEN mgroup IS NULL THEN [] ELSE COLLECT(distinct mgroup.name) END as manualGroups, " +
			"CASE WHEN f IS NULL THEN [] ELSE COLLECT(distinct [f.externalId, rf.scope]) END as functions, " +
			"CASE WHEN duplicate IS NULL THEN [] " +
			"ELSE COLLECT(distinct { id: duplicate.id, firstName: duplicate.firstName, lastName: duplicate.lastName, score: d.score, code: duplicate.activationCode, structures: structuresDup }) END as duplicates, " +
			"COLLECT (distinct {id: struct.id, name: struct.name}) as structures " +
			"ORDER BY lastName, firstName " +
			"UNION " +
			"MATCH (u: User)-[:HAS_RELATIONSHIPS]->(b: Backup) " +
			"WHERE {structureId} IN b.structureIds AND EXISTS(u.deleteDate)" +
			"MATCH (s: Structure) " +
			"WHERE s.id IN b.structureIds " +
			"WITH u, b, s " +
			"RETURN DISTINCT u.id as id, u.profiles[0] as type, u.activationCode as code, u.login as login, u.firstName as firstName, " +
			"u.lastName as lastName, u.displayName as displayName,u.source as source, u.deleteDate as deleteDate, u.disappearanceDate as disappearanceDate, u.blocked as blocked, u.created as creationDate, " +
			"[] as aafFunctions, [] as classes, [] as functionalGroups, [] as manualGroups, [] as functions, [] as duplicates, " +
			"COLLECT(distinct {id: s.id, name: s.name}) as structures " +
			"ORDER BY lastName, firstName ";

		JsonObject params = new JsonObject().put("structureId", structureId);
		neo.execute(query, params, Neo4jResult.validResultHandler(handler));
	}

	@Override
	public void blockUsers(String structureId, String profile, boolean block, Handler<JsonObject> handler) {
		String query = "MATCH (s:Structure {id:{structureId}})<-[:DEPENDS]-(g:ProfileGroup)<-[:IN]-(u:User) WHERE g.name ENDS WITH {profile} SET u.blocked = {blocked} RETURN COLLECT(DISTINCT u.id) as usersId";
		JsonObject params = new JsonObject()
				.put("structureId", structureId)
				.put("profile", profile)
				.put("blocked", block);
		neo.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) { ;
				handler.handle(r.body());
			}
		});
	}

	@Override
	public void searchCriteria(List<String> structures, boolean getClassesForMonoEtabOnly, Handler<Either<String, JsonObject>> handler) {
		final StringBuilder query = new StringBuilder();
		query.append("MATCH (s:Structure) WHERE s.id IN {structures} ");

		if (getClassesForMonoEtabOnly) {
			if(structures != null && structures.size() == 1) {
				query.append("OPTIONAL MATCH (s)<-[:BELONGS]-(c:Class) ");
			}
		} else {
			query.append("OPTIONAL MATCH (s)<-[:BELONGS]-(c:Class) ");
		}

		query.append("OPTIONAL MATCH (s)<-[:DEPENDS]-(fg:FunctionGroup) ");
		query.append("OPTIONAL MATCH (s)<-[:DEPENDS]-(htg:HTGroup) ");
		query.append("RETURN COLLECT(DISTINCT { id: s.id, name: s.name}) as structures, ");

		if (getClassesForMonoEtabOnly) {
			if (structures != null && structures.size() == 1) {
				query.append("FILTER(c IN COLLECT(DISTINCT { id: c.id, name: c.name}) WHERE NOT(c.id IS NULL)) as classes, ");
			}
		} else {
			query.append("FILTER(c IN COLLECT(DISTINCT { id: c.id, name: c.name}) WHERE NOT(c.id IS NULL)) as classes, ");
		}

		query.append("CASE WHEN LENGTH(COLLECT(distinct htg)) = 0 THEN COLLECT(DISTINCT fg.filter) ELSE COLLECT(DISTINCT fg.filter) + 'HeadTeacher' END as functions, ");
		query.append("['Teacher', 'Personnel', 'Student', 'Relative', 'Guest'] as profiles, ");
		query.append("['ManualGroup','FunctionalGroup','CommunityGroup'] as groupTypes");

		neo.execute(query.toString(), new JsonObject().put("structures", new JsonArray(structures)), validUniqueResultHandler(handler));
	}

	@Override
	public void getClasses(String structureId, Handler<Either<String, JsonObject>> handler) {
		String query = "MATCH (s: Structure {id: {structureId}})<-[:BELONGS]-(c:Class) " +
				"RETURN FILTER(c IN COLLECT(DISTINCT { id: c.id, label: c.name }) WHERE NOT(c.id IS NULL)) as classes";
		neo.execute(query, new JsonObject().put("structureId", structureId), validUniqueResultHandler(handler));
	}

	@Override
	public void massDistributionAndLevelOfEducation(JsonArray data, Handler<Either<String, JsonObject>> handler) {

		StatementsBuilder s = new StatementsBuilder();

		data.forEach(entry -> {

			JsonObject jo = (JsonObject) entry;
			String structureId = jo.getString("ent_structure_id");
			String distribution = jo.getString("distribution");
			Integer education = jo.getInteger("education");

			if (structureId != null) {
				String query = "MATCH (s:Structure {id: {structureId}}) " +
						"SET s.levelsOfEducation = {levelsOfEducation} " +
						"SET s.distributions = {distributions} ";

				JsonObject params = new JsonObject().put("structureId", structureId)
						.put("levelsOfEducation", education != null ? Arrays.asList(education) : Collections.EMPTY_LIST)
						.put("distributions", distribution != null ? Arrays.asList(distribution) : Collections.EMPTY_LIST);

				s.add(query, params);
			}

		});

		neo.executeTransaction(s.build(), null, true, validEmptyHandler(handler));
	}
}
