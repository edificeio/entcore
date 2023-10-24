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
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.StringUtils;
import org.entcore.common.validation.StringValidation;
import org.entcore.directory.Directory;
import org.entcore.directory.services.SchoolService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.neo4j.Neo4jResult.*;
import static org.entcore.common.user.DefaultFunctions.*;

public class DefaultSchoolService implements SchoolService {
	private static final Logger log = LoggerFactory.getLogger(DefaultSchoolService.class);
	private final int firstLevel = 1;
	private final int secondLevel = 2;
	private final JsonArray defaultStructureLevelsOfEducation = new JsonArray()
			.add(firstLevel)
			.add(secondLevel);
	private final JsonArray defaultStructureDistributions = new JsonArray();

	private final Neo4j neo = Neo4j.getInstance();
	private final EventBus eventBus;
	private String listUserMode = "multi";

	static final String EXCLUDE_ADMC_QUERY_FILTER = " NOT((u)-[:HAS_FUNCTION]->(:Function {externalId:'SUPER_ADMIN'})) ";

	public DefaultSchoolService(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	public DefaultSchoolService setListUserMode(String listUserMode) {
		this.listUserMode = listUserMode;
		return this;
	}

	@Override
	public void create(JsonObject school, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject().put("action", "manual-create-structure")
				.put("data", school);
		eventBus.request(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(result)));
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
				params.put("structures", new JsonArray(scope));
			}
		}
		String query =
				"MATCH (s:Structure) " + condition +
				"OPTIONAL MATCH (s)-[r:HAS_ATTACHMENT]->(ps:Structure) " +
				"WITH s, COLLECT({id: ps.id, name: ps.name}) as parents " +
				"RETURN s.id as id, s.UAI as UAI, s.name as name, s.externalId as externalId, s.timetable as timetable, s.punctualTimetable AS punctualTimetable, " +
				"s.hasApp as hasApp, s.ignoreMFA as ignoreMFA, s.levelsOfEducation as levelsOfEducation, s.distributions as distributions, s.manualName AS manualName, " +
				"s.feederName AS feederName, s.source AS source, s.exports as exports, " +
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
		eventBus.request(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(result)));
	}

	@Override
	public void unlink(String structureId, String userId, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-remove-user")
				.put("structureId", structureId)
				.put("userId", userId);
		eventBus.request(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(result)));
	}

	@Override
	public void isParent(String structureId, String parentStructureId, Handler<Either<String, Boolean>> handler) {
		final String query = "MATCH (s:Structure {id: {structureId}}) RETURN EXISTS(s-[:HAS_ATTACHMENT*1..]->(:Structure {id: {parentStructureId}})) AS exists";
		JsonObject params = new JsonObject().put("structureId", structureId).put("parentStructureId", parentStructureId);
		neo.execute(query.toString(), params, validUniqueResultHandler(exists -> {
			if (exists.isRight()) {
				Boolean res = exists.right().getValue().getBoolean("exists");
				handler.handle(new Either.Right<>(res != null ? res.booleanValue() : true));
			} else {
				handler.handle(new Either.Left<>(exists.left().getValue()));
			}
		}));
	}

	@Override
	public void defineParent(String structureId, String parentStructureId, Handler<Either<String, JsonObject>> handler) {
		JsonObject action = new JsonObject()
				.put("action", "manual-structure-attachment")
				.put("structureId", structureId)
				.put("parentStructureId", parentStructureId);
		eventBus.request(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(0, handler)));
	}

	@Override
	public void removeParent(String structureId, String parentStructureId, Handler<Either<String, JsonObject>> handler) {
		JsonObject action = new JsonObject()
			.put("action", "manual-structure-detachment")
			.put("structureId", structureId)
			.put("parentStructureId", parentStructureId);
		eventBus.request(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(handler)));
	}

	@Override
	public void listChildren(String structureId, Handler<Either<String, JsonArray>> results) {
		final String query = "MATCH (Structure {id: {structureId}})<-[r:HAS_ATTACHMENT*1..]-(s:Structure) RETURN COLLECT(s.id) AS children";
		JsonObject params = new JsonObject().put("structureId", structureId);
		neo.execute(query.toString(), params, validUniqueResultHandler(handler -> {
			if (handler.isRight()) {
				JsonArray children = handler.right().getValue().getJsonArray("children");
				results.handle(new Either.Right<>(children != null ? children : new JsonArray()));
			} else {
				results.handle(new Either.Left<>(handler.left().getValue()));
			}
		}));
	}


	@Override
	public void list(JsonArray fields, Handler<Either<String, JsonArray>> results) {
		if (fields == null || fields.size() == 0) {
			fields = new JsonArray().add("id").add("externalId").add("name").add("UAI");
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
	public void updateAndLog(UserInfos user, String structureId, JsonObject body, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-update-structure")
				.put("structureId", structureId)
				.put("data", body)
				.put("userId", user.getUserId())
				.put("userLogin", user.getLogin());
		eventBus.request(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(result)));
	}

	@Override
	public void update(String structureId, JsonObject body, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-update-structure")
				.put("structureId", structureId)
				.put("data", body);
		eventBus.request(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(result)));
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
				params.put("scope", new JsonArray(scope));
			}
		} else if(userInfos.getFunctions().containsKey(CLASS_ADMIN)){
			UserInfos.Function f = userInfos.getFunctions().get(CLASS_ADMIN);
			List<String> scope = f.getScope();
			if (scope != null && !scope.isEmpty()) {
				condition = "AND class.id IN {scope} ";
				params.put("scope", new JsonArray(scope));
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
		final String search = StringValidation.sanitize(input);
		String query =
			"MATCH (u:User)-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure {id: {id}}) " +
			"WHERE u.displayNameSearchField CONTAINS {search} " +
			"OR u.lastNameSearchField CONTAINS {search} " +
			"OR u.firstNameSearchField CONTAINS {search} " +
			"RETURN distinct u.id as id, u.firstName as firstName, u.lastName as lastName, u.displayName as displayName " +
			"ORDER BY u.lastName";

		JsonObject params = new JsonObject()
				.put("id", structureId)
				.put("search", search);
		neo.execute(query, params, Neo4jResult.validResultHandler(handler));
	}

	@Override
	public void userList(String structureId, boolean listRemovedUsersInsteadOfNormalUsers, boolean isAdmc, Handler<Either<String, JsonArray>> handler) {
		String filter = "";
		if (!isAdmc) {
			filter = (listRemovedUsersInsteadOfNormalUsers ? "AND " : "WHERE ") + EXCLUDE_ADMC_QUERY_FILTER;
		}
		String userStructMatcher = listRemovedUsersInsteadOfNormalUsers == false
			? "MATCH (u:User)-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure {id:{structureId}}) "
			: "MATCH (s:Structure {id:{structureId}}), " +
				"(u:User)-[:IN]->(pg:ProfileGroup) WHERE EXISTS(u.removedFromStructures) AND s.externalId IN (coalesce(u.removedFromStructures, [])) ";
		String query =
			userStructMatcher + filter +
			"OPTIONAL MATCH (u)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(class: Class) " +
			"  WITH distinct u, CASE WHEN class IS NULL THEN [] ELSE COLLECT(distinct {id: class.id, name: class.name, externalId : class.externalId}) END as classes  " +
			"OPTIONAL MATCH (u)-[d: DUPLICATE]-(duplicate: User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(sd: Structure) " +
			"  WITH distinct u, classes, collect(DISTINCT {id: sd.id, name: sd.name}) as structuresDup, duplicate, d " +
			"OPTIONAL MATCH (u)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(struct: Structure) " +
			"  WITH distinct u, classes, structuresDup, " +
			"  CASE WHEN duplicate IS NULL THEN [] ELSE COLLECT(distinct { id: duplicate.id, firstName: duplicate.firstName, lastName: duplicate.lastName, score: d.score, code: duplicate.activationCode, structures: structuresDup }) END as duplicates, " +
			"  COLLECT (distinct {id: struct.id, name: struct.name, externalId: struct.externalId}) as structures " +
			"OPTIONAL MATCH (u)-[:IN]->(fgroup: FunctionalGroup) " +
			"  WITH distinct u, classes, structuresDup, duplicates, structures, CASE WHEN fgroup IS NULL THEN [] ELSE COLLECT(distinct fgroup.name) END as functionalGroups " +
			"OPTIONAL MATCH (u)-[:IN]->(mgroup: ManualGroup) " +
			"  WITH distinct u, classes, structuresDup, duplicates, structures, functionalGroups, CASE WHEN mgroup IS NULL THEN [] ELSE COLLECT(distinct mgroup.name) END as manualGroups " +
			"OPTIONAL MATCH (u)-[rf:HAS_FUNCTION]->(f:Function) " +
			"  WITH distinct u, classes, structuresDup, duplicates, structures, functionalGroups, manualGroups, CASE WHEN f IS NULL THEN [] ELSE COLLECT(distinct [f.externalId, rf.scope]) END as functions " +
			"OPTIONAL MATCH (u)-[:HAS_POSITION]->(p:UserPosition)-[:IN]->(:Structure {id:{structureId}}) " +
			"  WITH distinct u, classes, structuresDup, duplicates, structures, functionalGroups, manualGroups, functions, CASE WHEN p IS NULL THEN [] ELSE COLLECT(distinct {id: p.id}) END as userPositions " +
			"RETURN DISTINCT " +
			"u.id as id, u.profiles[0] as type, u.activationCode as code, u.login as login," +
			"u.firstName as firstName, u.lastName as lastName, u.displayName as displayName," +
			"u.source as source, u.deleteDate as deleteDate, u.disappearanceDate as disappearanceDate, u.blocked as blocked, u.created as creationDate, u.removedFromStructures AS removedFromStructures, " +
			"EXTRACT(function IN u.functions | split(function, \"$\")) as aafFunctions," +
			" classes, functionalGroups, manualGroups, functions, duplicates, structures, userPositions " +
			"ORDER BY lastName, firstName ";
		if("none".equals(listUserMode)){
			//do not include presuppressed user
		}else if("mono".equals(listUserMode)){
			//include presuppressed (mono etab)
			query += " UNION " +
					"MATCH (s: Structure) WHERE s.id = {structureId} WITH s " +
					"MATCH (u: User)-[:HAS_RELATIONSHIPS]->(b: Backup) WHERE {structureId} IN b.structureIds AND EXISTS(u.deleteDate)" +
					"WITH u, b, s " +
					"RETURN DISTINCT u.id as id, u.profiles[0] as type, u.activationCode as code, u.login as login, u.firstName as firstName, u.lastName as lastName, u.displayName as displayName, " +
					"u.source as source, u.deleteDate as deleteDate, u.disappearanceDate as disappearanceDate, u.blocked as blocked, u.created as creationDate, u.removedFromStructures as removedFromStructures, " +
					"[] as aafFunctions, [] as classes, [] as functionalGroups, [] as manualGroups, [] as functions, [] as duplicates, " +
					"COLLECT(distinct {id: s.id, name: s.name, externalId: s.externalId}) as structures, [] as userPositions " +
					"ORDER BY lastName, firstName ";
		}else{
			//include presuppressed (multi etab)
			query += " UNION " +
					"MATCH (u: User)-[:HAS_RELATIONSHIPS]->(b: Backup) " +
					"WHERE {structureId} IN b.structureIds AND EXISTS(u.deleteDate)" +
					"MATCH (s: Structure) " +
					"WHERE s.id IN b.structureIds " +
					"WITH u, b, s " +
					"RETURN DISTINCT u.id as id, u.profiles[0] as type, u.activationCode as code, u.login as login, u.firstName as firstName, u.lastName as lastName, u.displayName as displayName, " +
					"u.source as source, u.deleteDate as deleteDate, u.disappearanceDate as disappearanceDate, u.blocked as blocked, u.created as creationDate, u.removedFromStructures as removedFromStructures, " +
					"[] as aafFunctions, [] as classes, [] as functionalGroups, [] as manualGroups, [] as functions, [] as duplicates, " +
					"COLLECT(distinct {id: s.id, name: s.name, externalId: s.externalId}) as structures, [] as userPositions " +
					"ORDER BY lastName, firstName ";
		}
		JsonObject params = new JsonObject().put("structureId", structureId);
		neo.execute(query, params, Neo4jResult.validResultHandler(handler));
	}

	@Override
	public void blockUsers(String structureId, String profile, boolean block, boolean isAdmc, Handler<JsonObject> handler) {
		String filter = "";
		if (!isAdmc) {
			filter = "AND " + EXCLUDE_ADMC_QUERY_FILTER;
		}
		String query =
			"MATCH (s:Structure {id:{structureId}})<-[:DEPENDS]-(g:ProfileGroup)<-[:IN]-(u:User) " +
			"WHERE g.name ENDS WITH {profile} " + filter +
			"SET u.blocked = {blocked} " +
			"RETURN COLLECT(DISTINCT u.id) as usersId";
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
		query.append("OPTIONAL MATCH (s)<-[:DEPENDS]-(dirg:DirectionGroup) WHERE {1d} IN COALESCE(s.levelsOfEducation, {defLoe})");
		query.append("RETURN COLLECT(DISTINCT { id: s.id, name: s.name}) as structures, ");

		if (getClassesForMonoEtabOnly) {
			if (structures != null && structures.size() == 1) {
				query.append("FILTER(c IN COLLECT(DISTINCT { id: c.id, name: c.name}) WHERE NOT(c.id IS NULL)) as classes, ");
			}
		} else {
			query.append("FILTER(c IN COLLECT(DISTINCT { id: c.id, name: c.name}) WHERE NOT(c.id IS NULL)) as classes, ");
		}

		query.append("COLLECT(DISTINCT fg.filter) " +
									" + CASE WHEN LENGTH(COLLECT(distinct htg)) = 0 THEN [] ELSE 'HeadTeacher' END " +
									" + CASE WHEN LENGTH(COLLECT(distinct dirg)) = 0 THEN [] ELSE 'Direction' END " +
									" as functions, ");
		query.append("['Teacher', 'Personnel', 'Student', 'Relative', 'Guest'] as profiles, ");
		query.append("['ManualGroup','FunctionalGroup','CommunityGroup'] as groupTypes");

		JsonObject params = new JsonObject().put("structures", new JsonArray(structures)).put("1d", firstLevel).put("defLoe", defaultStructureLevelsOfEducation);
		neo.execute(query.toString(), params, validUniqueResultHandler(handler));
	}

	@Override
	public void getClasses(String structureId, Handler<Either<String, JsonObject>> handler) {
		String query = "MATCH (s: Structure {id: {structureId}})<-[:BELONGS]-(c:Class) " +
				"RETURN FILTER(c IN COLLECT(DISTINCT { id: c.id, label: c.name }) WHERE NOT(c.id IS NULL)) as classes";
		neo.execute(query, new JsonObject().put("structureId", structureId), validUniqueResultHandler(handler));
	}

	@Override
	public void massDistributionEducationMobileApp(JsonArray data, Integer transactionId, Boolean commit, Handler<Either<String, JsonObject>> handler) {

		StatementsBuilder s = new StatementsBuilder();

		data.forEach(entry -> {

			JsonObject jo = (JsonObject) entry;
			String structureId = jo.getString("ent_structure_id");
			String distribution = jo.getString("distribution");
			List<String> distributions = StringUtils.isEmpty(distribution) ? Collections.EMPTY_LIST :
					Arrays.stream(distribution.split(",")).collect(Collectors.toList());
			String education = jo.getString("education");
			List<Long> education_levels = StringUtils.isEmpty(education) ? Collections.EMPTY_LIST :
					Arrays.stream(education.split(",")).mapToLong(Long::parseLong).boxed().collect(Collectors.toList());
			Boolean hasApp = jo.getBoolean("hasApp");
			Boolean ignoreMFA = jo.getBoolean("ignoreMFA");

			if (structureId != null) {
				String query = "MATCH (s:Structure {id: {structureId}}) " +
						"SET s.levelsOfEducation = {levelsOfEducation} " +
						"SET s.distributions = {distributions} " +
						"SET s.ignoreMFA = {ignoreMFA} " +
						"SET s.hasApp = {hasApp}";

				JsonObject params = new JsonObject().put("structureId", structureId)
						.put("levelsOfEducation", education_levels)
						.put("distributions", distributions)
						.put("ignoreMFA", ignoreMFA)
						.put("hasApp", hasApp);

				s.add(query, params);
			}

		});

		neo.executeTransaction(s.build(), transactionId, commit.booleanValue(), validEmptyHandler(handler));
	}

	@Override
	public void resetName(String structureId, Handler<Either<String, JsonObject>> handler)
	{
		String query = "MATCH (s:Structure {id:{structureId}}) SET s.manualName = false, s.name = coalesce(s.feederName, s.name)";
		JsonObject params = new JsonObject().put("structureId", structureId);
		neo.execute(query, params, validEmptyHandler(handler));
	}

	@Override
	public void getActivationInfos(JsonArray structureIds, Handler<Either<String, JsonArray>> handler) {
		StringBuilder query = new StringBuilder();
		query.append("MATCH (s:Structure) WHERE s.id IN {structures} WITH s ");
		query.append("MATCH (u:User)-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(s)" );
		query.append("WITH s, COLLECT(DISTINCT u) AS uu ");
		query.append("UNWIND uu AS u ");
		query.append("WITH s, HEAD(u.profiles) AS p, HAS(u.activationCode) AS unactive ");
		query.append("RETURN s.id AS id, ");
		query.append("SUM(CASE WHEN NOT(unactive) THEN 1 ELSE 0 END) AS activated, ");
		query.append("SUM(CASE WHEN unactive THEN 1 ELSE 0 END) AS notactivated, ");
		query.append("SUM(CASE WHEN p = \"Teacher\" AND NOT(unactive) THEN 1 ELSE 0 END) AS teacherActivated, ");
		query.append("SUM(CASE WHEN p = \"Teacher\" AND unactive THEN 1 ELSE 0 END) AS teacherUnactivated, ");
		query.append("SUM(CASE WHEN p = \"Student\" AND NOT(unactive) THEN 1 ELSE 0 END) AS studentActivated, ");
		query.append("SUM(CASE WHEN p = \"Student\" AND unactive THEN 1 ELSE 0 END) AS studentUnactivated ");

		JsonObject params = new JsonObject().put("structures", structureIds);
		neo.execute(query.toString(), params, validResultHandler(handler));
	}

	@Override
	public void getUsersActivity(JsonArray userIds, Handler<Either<String, JsonArray>> handler) {
		StringBuilder query = new StringBuilder();

		query.append("MATCH (u:User) WHERE u.id IN {users} ");
		query.append("RETURN u.id as id, u.lastLogin AS lastlogin, ");
		query.append("(CASE WHEN u.lastLogin IS NULL THEN FALSE ELSE TRUE END) AS isactive");

		JsonObject params = new JsonObject().put("users", userIds);
		neo.execute(query.toString(), params, validResultHandler(handler));
	}

	@Override
	public void getStructureNameByUAI(JsonArray uais, Handler<Either<String, JsonArray>> handler) {
		StringBuilder query = new StringBuilder();
		query.append("MATCH (s:Structure) WHERE s.UAI IN {uais} ");
		query.append("RETURN s.name AS name, s.UAI as UAI");
		JsonArray uaiUppercase = new JsonArray(uais.stream().map(uai -> uai.toString().toUpperCase()).collect(Collectors.toList()));
		JsonObject params = new JsonObject().put("uais", uaiUppercase);
		neo.execute(query.toString(), params, validResultHandler(handler));
	}

	@Override
	public void duplicateStructureSettings(String structureId, JsonArray targetUAIs, JsonObject options,
					 Handler<Either<String, JsonObject>> handler) {
		List<String> list = targetUAIs.getList();
		StatementsBuilder builder = new StatementsBuilder();
		for (int i = 0; i < list.size(); i+=10) {
			List<String> sublist = list.subList(i, Math.min((i+10), list.size()));
			duplicate(structureId, new JsonArray(sublist), options, builder);
		}
		neo.executeTransaction(builder.build(), null, true, validEmptyHandler(handler));
	}

	private void duplicate(String structureId, JsonArray targetUAIs, JsonObject options, StatementsBuilder builder) {
		final boolean setApplications = options.getBoolean("setApplications", true),
				setWidget = options.getBoolean("setWidgets", true),
				setDistribution = options.getBoolean("setDistribution", true),
				setEducation = options.getBoolean("setEducation", true),
				setHasApp = options.getBoolean("setHasApp", true);
		if (setApplications) {
			buildDuplicateQuery(structureId, targetUAIs, builder, "Role", "ProfileGroup");
			buildDuplicateQuery(structureId, targetUAIs, builder, "Role", "FunctionGroup");
		}
		if (setWidget) {
			buildDuplicateQuery(structureId, targetUAIs, builder, "Widget", "ProfileGroup");
			buildDuplicateQuery(structureId, targetUAIs, builder, "Widget", "FunctionGroup");
		}
		if (setDistribution || setEducation || setHasApp) {
			String structureUpdateQuery = "MATCH (s:Structure {id:{structureId}}), (s2:Structure) " +
					"WHERE s2.UAI IN {uais} SET ";
			if (setDistribution) {
				structureUpdateQuery += "s2.distributions = s.distributions,";
			}
			if (setEducation) {
				structureUpdateQuery += "s2.levelsOfEducation = s.levelsOfEducation,";
			}
			if (setHasApp) {
				structureUpdateQuery += "s2.hasApp = s.hasApp,";
			}
			// removing lastComma
			structureUpdateQuery = structureUpdateQuery.substring(0, structureUpdateQuery.length()-1);

			final JsonObject params = new JsonObject().put("structureId", structureId).put("uais", targetUAIs);
			builder.add(structureUpdateQuery, params);
		}
	}

	private void buildDuplicateQuery(String structureId, JsonArray targetUAIs, StatementsBuilder builder, String nodeType, String groupType) {
		final JsonObject params = new JsonObject().put("structureId", structureId).put("uais", targetUAIs);
		final String deleteExistingQuery = "MATCH (r:"+nodeType+")<-[a:AUTHORIZED]-(:"+groupType+")-[:DEPENDS]->(s:Structure) " +
				"WHERE s.UAI in {uais} " +
				"AND NOT (:External)-[:PROVIDE]->(:Action)<-[:AUTHORIZE]-(r) " +
				"DELETE a";
		builder.add(deleteExistingQuery, params);
		final String duplicateQuery = "MATCH (s:Structure {id:{structureId}})<-[:DEPENDS]-(g1:"+groupType+")-[a:AUTHORIZED]->(r:"+nodeType+"), " +
				"(s2:Structure)<-[:DEPENDS]-(g2:"+groupType+") " +
				"WHERE NOT (:External)-[:PROVIDE]->(:Action)<-[:AUTHORIZE]-(r) " +
				"AND s2.UAI IN {uais} AND g2.name ENDS WITH LAST(SPLIT(g1.name, '-')) AND g1.id <> g2.id " +
				"MERGE (g2)-[a2:AUTHORIZED]->(r)" +
				"ON CREATE SET a2.mandatory = a.mandatory";
		builder.add(duplicateQuery, params);
	}

	@Override
	public void checkGAR(JsonArray uais, Handler<Either<String, JsonArray>> handler) {
		final String query = "MATCH (s:Structure) WHERE s.source IN ['AAF', 'AAF1D'] AND s.UAI IN {uais} " +
				"MATCH s<-[:DEPENDS]-(g:ProfileGroup)<-[:IN]-(u:User) " +
				"WITH s, count(u) as users where users > 0 RETURN DISTINCT s.name AS name, s.UAI as UAI";
		JsonArray uaiUppercase = new JsonArray(uais.stream().map(uai -> uai.toString().toUpperCase()).collect(Collectors.toList()));
		JsonObject params = new JsonObject().put("uais", uaiUppercase);
		neo.execute(query, params, validResultHandler(handler));
	}

	@Override
	public void activateGar(String garId, JsonArray targetUAIs, String groupName, String appName, Handler<Either<String, JsonObject>> handler) {
		final List<String> list = targetUAIs.getList();
		final AtomicInteger countDown = new AtomicInteger(list.size());
		final JsonArray errors = new JsonArray();

		for (String uai : list) {
			activateGar(garId, uai, groupName, appName, result -> {
				if (result.isLeft()) {
					final JsonObject error = result.left().getValue();
					errors.add(error.getString("uai"));
					log.error(error.getString("error") + error.getString("uai"));
				}

				if (countDown.decrementAndGet() == 0) {
					handler.handle(new Either.Right<>(new JsonObject().put("errors", errors)));
				}
            });
		}
	}

	private void activateGar(String garId, String uai, String groupName, String appName, Handler<Either<JsonObject, JsonObject>> handler) {
		final String query = "MATCH (s:Structure {UAI:{uai}}) OPTIONAL MATCH (s)<-[:DEPENDS]-(g:ManualGroup{name:{groupName}}) SET " +
				"s.exports = CASE WHEN ('GAR-' + {garId}) IN coalesce(s.exports, []) then s.exports " +
				"WHEN ANY(x IN coalesce(s.exports, []) WHERE x starts WITH 'GAR-') then [x IN s.exports | CASE WHEN x STARTS WITH 'GAR-' THEN 'GAR-' + {garId} ELSE x END] " +
				"ELSE coalesce(s.exports, []) + ('GAR-' + {garId}) END return s.id as structureId, g.id as groupId";

		final JsonObject params = new JsonObject().put("uai", uai).put("garId", garId).put("groupName", groupName);
		neo.execute(query, params, validUniqueResultHandler(either -> {
			if (either.isLeft()) {
				handler.handle(new Either.Left<>(new JsonObject().put("error", "Failed to deploy GAR structure : ").put("uai", uai)));
				return;
			}
			JsonObject result = either.right().getValue();
			if (result.getValue("structureId") == null) {
				handler.handle(new Either.Left<>(new JsonObject().put("error", "Failed to deploy GAR structure : ").put("uai", uai)));
				return;
			}
			final JsonObject group = new JsonObject().put("name", groupName).put("groupDisplayName", groupName);
			group.put("id", result.getValue("groupId"));
			group.put("lockDelete", true);

			JsonObject action = new JsonObject()
					.put("action", "manual-create-group")
					.put("structureId", result.getValue("structureId"))
					.put("group", group);
			eventBus.request("entcore.feeder", action, (Handler<AsyncResult<Message<JsonObject>>>) groupResult -> {
				if (groupResult.failed()) {
					handler.handle(new Either.Left<>(new JsonObject().put("error", "Failed to create GAR group : ").put("uai", uai)));
					return;
				}

				String groupId = groupResult.result().body()
						.getJsonArray("results")
						.getJsonArray(0)
						.getJsonObject(0).getString("id");

				StatementsBuilder builder = new StatementsBuilder();
				final String authorizedQuery = "MATCH (a:Application {name:{appName}})-[]->(ac:Action)<-[]-(r:Role), (g:Group {id:{groupId}}) " +
						" CREATE UNIQUE (g)-[:AUTHORIZED]->(r)";
				final JsonObject authorizedParams = new JsonObject().put("appName", appName).put("groupId", groupId);
				builder.add(authorizedQuery, authorizedParams);

				final String populateQuery = "MATCH (s:Structure {UAI: {uai}})<-[:DEPENDS]-(g:ManualGroup {id: {groupId} }), (s)<-[:DEPENDS]-(pg:ProfileGroup)<-[:IN]-(u:User) " +
						"WHERE pg.filter IN ['Teacher','Personnel'] AND ANY(function IN u.functions WHERE function CONTAINS '$DIR$' OR function CONTAINS '$DOC$' OR function CONTAINS '$DIRECTION') " +
						"CREATE UNIQUE u-[:IN {source:'MANUAL'}]->g";
				final JsonObject populateParams = new JsonObject().put("uai", uai).put("groupId", groupId);
				builder.add(populateQuery, populateParams);

				neo.executeTransaction(builder.build(), null, true, res -> {
                    if ("ok".equals(res.body().getString("status"))) {
                        handler.handle(new Either.Right<>(new JsonObject()));
                    } else {
                        handler.handle(new Either.Left<>(new JsonObject().put("error", "Failed to authorise and populate GAR group : ").put("uai", uai)));
                    }
                });
			});
		}));
	}
}
