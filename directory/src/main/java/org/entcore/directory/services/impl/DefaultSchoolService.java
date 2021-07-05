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

import io.vertx.core.CompositeFuture;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.StringUtils;
import org.entcore.common.validation.StringValidation;
import org.entcore.common.neo4j.TransactionHelper;
import org.entcore.directory.Directory;
import org.entcore.directory.services.SchoolService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

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

	static final String EXCLUDE_ADMC_QUERY_FILTER = " NOT((u)-[:HAS_FUNCTION]->(:Function {externalId:'SUPER_ADMIN'})) ";

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
				"RETURN s.id as id, s.UAI as UAI, s.name as name, s.externalId as externalId, s.timetable as timetable, s.punctualTimetable AS punctualTimetable, " +
				"s.hasApp as hasApp, s.levelsOfEducation as levelsOfEducation, s.distributions as distributions, s.manualName AS manualName, " +
				"s.feederName AS feederName, " +
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
		final String search = StringValidation.sanitize(input);
		String query =
			"MATCH (u:User)-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
			"WHERE s.id = {id} " +
			"AND u.displayNameSearchField CONTAINS {search} " +
			"RETURN distinct u.id as id, u.firstName as firstName, u.lastName as lastName " +
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
			"MATCH (pg)-[:HAS_PROFILE]->(p: Profile) " +
			"WITH distinct u, p " +
			"OPTIONAL MATCH (u)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(class: Class) " +
			"  WITH distinct u, p, CASE WHEN class IS NULL THEN [] ELSE COLLECT(distinct {id: class.id, name: class.name, externalId : class.externalId}) END as classes  " +
			"OPTIONAL MATCH (u)-[d: DUPLICATE]-(duplicate: User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(sd: Structure) " +
			"  WITH distinct u, p, classes, collect(DISTINCT {id: sd.id, name: sd.name}) as structuresDup, duplicate, d " +
			"OPTIONAL MATCH (u)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(struct: Structure) " +
			"  WITH distinct u, p, classes, structuresDup, " +
			"  CASE WHEN duplicate IS NULL THEN [] ELSE COLLECT(distinct { id: duplicate.id, firstName: duplicate.firstName, lastName: duplicate.lastName, score: d.score, code: duplicate.activationCode, structures: structuresDup }) END as duplicates, " +
			"  COLLECT (distinct {id: struct.id, name: struct.name, externalId: struct.externalId}) as structures " +
			"OPTIONAL MATCH (u)-[:IN]->(fgroup: FunctionalGroup) " +
    		"  WITH distinct u, p, classes, structuresDup, duplicates, structures, CASE WHEN fgroup IS NULL THEN [] ELSE COLLECT(distinct fgroup.name) END as functionalGroups " +
			"OPTIONAL MATCH (u)-[:IN]->(mgroup: ManualGroup) " +
			"  WITH distinct u, p, classes, structuresDup, duplicates, structures, functionalGroups, CASE WHEN mgroup IS NULL THEN [] ELSE COLLECT(distinct mgroup.name) END as manualGroups " +
			"OPTIONAL MATCH (u)-[rf:HAS_FUNCTION]->()-[:CONTAINS_FUNCTION*0..1]->(f:Function) " +
			"  WITH distinct u, p, classes, structuresDup, duplicates, structures, functionalGroups, manualGroups, CASE WHEN f IS NULL THEN [] ELSE COLLECT(distinct [f.externalId, rf.scope]) END as functions " +
			"RETURN DISTINCT " +
			"u.id as id, p.name as type, u.activationCode as code, u.login as login," +
			"u.firstName as firstName, u.lastName as lastName, u.displayName as displayName," +
			"u.source as source, u.deleteDate as deleteDate, u.disappearanceDate as disappearanceDate, u.blocked as blocked, u.created as creationDate, u.removedFromStructures AS removedFromStructures, " +
			"EXTRACT(function IN u.functions | split(function, \"$\")) as aafFunctions," +
			" classes, functionalGroups, manualGroups, functions, duplicates, structures " +
			"ORDER BY lastName, firstName " +
			"UNION " +
			"MATCH (u: User)-[:HAS_RELATIONSHIPS]->(b: Backup) " +
			"WHERE {structureId} IN b.structureIds AND EXISTS(u.deleteDate)" +
			"MATCH (s: Structure) " +
			"WHERE s.id IN b.structureIds " +
			"WITH u, b, s " +
			"RETURN DISTINCT u.id as id, u.profiles[0] as type, u.activationCode as code, u.login as login, u.firstName as firstName, u.lastName as lastName, u.displayName as displayName, " +
			"u.source as source, u.deleteDate as deleteDate, u.disappearanceDate as disappearanceDate, u.blocked as blocked, u.created as creationDate, u.removedFromStructures as removedFromStructures, " +
			"[] as aafFunctions, [] as classes, [] as functionalGroups, [] as manualGroups, [] as functions, [] as duplicates, " +
			"COLLECT(distinct {id: s.id, name: s.name, externalId: s.externalId}) as structures " +
			"ORDER BY lastName, firstName ";

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
		query.append("OPTIONAL MATCH (s)<-[:DEPENDS]-(dirg:DirectionGroup) ");
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

		neo.execute(query.toString(), new JsonObject().put("structures", new JsonArray(structures)), validUniqueResultHandler(handler));
	}

	@Override
	public void getClasses(String structureId, Handler<Either<String, JsonObject>> handler) {
		String query = "MATCH (s: Structure {id: {structureId}})<-[:BELONGS]-(c:Class) " +
				"RETURN FILTER(c IN COLLECT(DISTINCT { id: c.id, label: c.name }) WHERE NOT(c.id IS NULL)) as classes";
		neo.execute(query, new JsonObject().put("structureId", structureId), validUniqueResultHandler(handler));
	}

	@Override
	public void massDistributionEducationMobileApp(JsonArray data, Boolean setDistribution, Boolean setEducation, Boolean setHasApp,
												   Integer transactionId, Boolean commit, Handler<Either<String, JsonObject>> handler) {

		if (!(setDistribution.booleanValue() || setEducation.booleanValue() || setHasApp.booleanValue())) {
			// nothing to do
			handler.handle(new Either.Right<>(new JsonObject().put("status", "ok")));
			return;
		}

		StatementsBuilder s = new StatementsBuilder();

		data.forEach(entry -> {

			String query = "MATCH (s:Structure {id: {structureId}}) ";
			JsonObject jo = (JsonObject) entry;
			String structureId = jo.getString("ent_structure_id");
			if (structureId == null) {
				return;
			}
			JsonObject params = new JsonObject().put("structureId", structureId);
			if (setDistribution.booleanValue()) {
				String distribution = jo.getString("distribution");
				List<String> distributions = StringUtils.isEmpty(distribution) ? Collections.EMPTY_LIST :
						Arrays.stream(distribution.split(",")).collect(Collectors.toList());
				query += "SET s.distributions = {distributions} ";
				params.put("distributions", distributions);
			}
			if (setEducation.booleanValue()) {
				String education = jo.getString("education");
				List<Long> education_levels = StringUtils.isEmpty(education) ? Collections.EMPTY_LIST :
						Arrays.stream(education.split(",")).mapToLong(Long::parseLong).boxed().collect(Collectors.toList());
				query += "SET s.levelsOfEducation = {levelsOfEducation} ";
				params.put("levelsOfEducation", education_levels);
			}
			if (setHasApp.booleanValue()) {
				Boolean hasApp = jo.getBoolean("hasApp");
				if (hasApp != null) {
					query += "SET s.hasApp = {hasApp}";
					params.put("hasApp", hasApp);
				}
			}
			s.add(query, params);
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
		query.append("WITH s, COLLECT(DISTINCT u) as users WITH s, ");
		query.append("FILTER(u IN users WHERE u.activationCode IS NULL) as active, ");
		query.append("FILTER(u IN users WHERE NOT(u.activationCode IS NULL)) as inactive ");
		query.append("RETURN s.id AS id, LENGTH(active) AS activated, LENGTH(inactive) AS notactivated");

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
		query.append("RETURN s.id AS id, s.name AS name, s.UAI as UAI");
		JsonArray uaiUppercase = new JsonArray(uais.stream().map(uai -> uai.toString().toUpperCase()).collect(Collectors.toList()));
		JsonObject params = new JsonObject().put("uais", uaiUppercase);
		neo.execute(query.toString(), params, validResultHandler(handler));
	}

	@Override
	public void duplicateStructureSettings(String structureId, JsonArray targetStructureIds, JsonObject options,
										   Handler<Either<String, JsonObject>> handler) {
		TransactionHelper helper = new TransactionHelper(neo, eventBus, true);

		StatementsBuilder builder = new StatementsBuilder();
		JsonObject params = new JsonObject().put("structureId", structureId);
		String query1 = "MATCH (s:Structure {id:{structureId}}) RETURN " +
				"REDUCE(acc = '', level IN s.levelsOfEducation | acc + CASE WHEN acc = '' THEN '' ELSE ',' END + level) AS education," +
				"REDUCE(acc = '', distrib IN s.distributions | acc + CASE WHEN acc = '' THEN '' ELSE ',' END + distrib) AS distribution," +
				"s.hasApp AS hasApp";
		builder.add(query1, params);
		String query2 = "MATCH (r:Role), (s:Structure {id:{structureId}}) " +
				"WHERE NOT (:External)-[:PROVIDE]->(:Action)<-[:AUTHORIZE]-(r) " +
				"AND ((r)<-[:AUTHORIZED]-(:ProfileGroup)-[:DEPENDS]->(s) OR" +
				"(r)<-[:AUTHORIZED]-(:FunctionGroup {filter: \"AdminLocal\"})-[:DEPENDS]->(s))" +
				"RETURN r.id AS ent_role_id, " +
				"EXISTS((r)<-[:AUTHORIZED]-(:ProfileGroup {filter: \"Teacher\"})-[:DEPENDS]->(s)) AS Teacher, " +
				"EXISTS((r)<-[:AUTHORIZED]-(:ProfileGroup {filter: \"Student\"})-[:DEPENDS]->(s)) AS Student, " +
				"EXISTS((r)<-[:AUTHORIZED]-(:ProfileGroup {filter: \"Relative\"})-[:DEPENDS]->(s)) AS Relative, " +
				"EXISTS((r)<-[:AUTHORIZED]-(:ProfileGroup {filter: \"Personnel\"})-[:DEPENDS]->(s)) AS Personnel, " +
				"EXISTS((r)<-[:AUTHORIZED]-(:ProfileGroup {filter: \"Guest\"})-[:DEPENDS]->(s)) AS Guest, " +
				"EXISTS((r)<-[:AUTHORIZED]-(:FunctionGroup {filter: \"AdminLocal\"})-[:DEPENDS]->(s)) AS AdminLocal";
		builder.add(query2, params);
		String query3 = "MATCH (w:Widget), (s:Structure {id:{structureId}}) " +
				"WHERE NOT (:External)-[:PROVIDE]->(:Action)<-[:AUTHORIZE]-(w) " +
				"AND ((w)<-[:AUTHORIZED]-(:ProfileGroup)-[:DEPENDS]->(s) OR" +
				"(w)<-[:AUTHORIZED]-(:FunctionGroup {filter: \"AdminLocal\"})-[:DEPENDS]->(s))" +
				"RETURN w.id AS ent_widget_id, " +
				"EXISTS((w)<-[:AUTHORIZED]-(:ProfileGroup {filter: \"Teacher\"})-[:DEPENDS]->(s)) AS Teacher, " +
				"EXISTS((w)<-[:AUTHORIZED]-(:ProfileGroup {filter: \"Student\"})-[:DEPENDS]->(s)) AS Student, " +
				"EXISTS((w)<-[:AUTHORIZED]-(:ProfileGroup {filter: \"Relative\"})-[:DEPENDS]->(s)) AS Relative, " +
				"EXISTS((w)<-[:AUTHORIZED]-(:ProfileGroup {filter: \"Personnel\"})-[:DEPENDS]->(s)) AS Personnel, " +
				"EXISTS((w)<-[:AUTHORIZED]-(:ProfileGroup {filter: \"Guest\"})-[:DEPENDS]->(s)) AS Guest, " +
				"EXISTS((w)<-[:AUTHORIZED]-(:FunctionGroup {filter: \"AdminLocal\"})-[:DEPENDS]->(s)) AS AdminLocal";
		builder.add(query3, params);
		helper.addStatements(builder.build(), message -> {
			if ("ok".equals(message.body().getString("status"))) {
				JsonObject resultSettings = message.body().getJsonArray("results").getJsonArray(0).getJsonObject(0);
				JsonArray resultApplications = message.body().getJsonArray("results").getJsonArray(1);
				JsonArray resultWidgets = message.body().getJsonArray("results").getJsonArray(2);
				JsonArray dataSettings = new JsonArray(), dataApplicationsWidgets = new JsonArray();
				for (int i = 0; i < targetStructureIds.size(); i++) {
					String targetStructureId = targetStructureIds.getString(i);
					JsonObject copySettings = resultSettings.copy();
					copySettings.put("ent_structure_id", targetStructureId);
					dataSettings.add(copySettings);
					for (int j = 0; j < resultApplications.size(); j++) {
						JsonObject copyRole = resultApplications.getJsonObject(j).copy();
						copyRole.put("ent_structure_id", targetStructureId);
						dataApplicationsWidgets.add(copyRole);
					}
					for (int k = 0; k < resultWidgets.size(); k++) {
						JsonObject copyWidget = resultWidgets.getJsonObject(k).copy();
						copyWidget.put("ent_structure_id", targetStructureId);
						dataApplicationsWidgets.add(copyWidget);
					}
				}
				Promise<Void> promise1 = Promise.promise();
				JsonObject action1 = new JsonObject()
						.put("action", "set-roles-and-profiles-by-structureId")
						.put("data", dataApplicationsWidgets)
						.put("setApplications", options.getBoolean("setApplications", true))
						.put("setWidgets", options.getBoolean("setWidgets", true));
				helper.addEventBusRequest("wse.app.registry.bus", action1, res -> {
					if ("ok".equals(res.body().getString("status"))) {
						promise1.complete();
					} else {
						promise1.fail(res.body().getString("message"));
					}
				});
				Promise<Void> promise2 = Promise.promise();
				JsonObject action2 = new JsonObject()
						.put("action", "set-distrib-and-education-by-structureId")
						.put("data", dataSettings)
						.put("setDistribution", options.getBoolean("setDistribution", true))
						.put("setEducation", options.getBoolean("setEducation", true))
						.put("setHasApp", options.getBoolean("setHasApp", true));
				helper.addEventBusRequest("directory", action2, res -> {
					if ("ok".equals(res.body().getString("status"))) {
						promise2.complete();
					} else {
						promise2.fail(res.body().getString("message"));
					}
				});
				CompositeFuture.join(promise1.future(), promise2.future()).onComplete(result -> {
					if (result.succeeded()) {
						helper.commit(bool -> {
							if (bool.booleanValue()) {
								handler.handle(new Either.Right<>(new JsonObject().put("status", "ok")));
							} else {
								handler.handle(new Either.Left<>("Error duplicating settings"));
							}
						});
					} else {
						handler.handle(new Either.Left<>(result.cause().toString()));
					}
				});
			} else {
				handler.handle(new Either.Left<>(message.body().getString("message")));
			}
		});
	}

}
