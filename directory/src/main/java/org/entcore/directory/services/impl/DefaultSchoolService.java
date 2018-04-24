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

import io.vertx.core.eventbus.Message;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.user.UserInfos;
import org.entcore.directory.Directory;
import org.entcore.directory.services.SchoolService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.neo4j.Neo4jResult.*;
import static org.entcore.common.user.DefaultFunctions.ADMIN_LOCAL;
import static org.entcore.common.user.DefaultFunctions.CLASS_ADMIN;
import static org.entcore.common.user.DefaultFunctions.SUPER_ADMIN;

public class DefaultSchoolService implements SchoolService {

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
		String query = "match (s:`Structure`) where s.id = {id} return s.id as id, s.UAI as UAI, s.name as name";
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
				"CASE WHEN any(p in parents where p <> {id: null, name: null}) THEN parents END as parents";

		neo.execute(query, params, validResultHandler(results));
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
	public void massmailUsers(String structureId, JsonObject filterObj, UserInfos userInfos, Handler<Either<String, JsonArray>> results) {
		this.massmailUsers(structureId, filterObj, true, true, null, userInfos, results);
	}
	@Override
	public void massmailUsers(String structureId, JsonObject filterObj, boolean groupClasses,
			boolean groupChildren, Boolean hasMail, UserInfos userInfos, Handler<Either<String, JsonArray>> results) {

		String filter =
				"MATCH (s:Structure {id: {structureId}})<-[:DEPENDS]-(g:ProfileGroup)<-[:IN]-(u:User), "+
				"(g)-[:HAS_PROFILE]-(p: Profile) ";
		String condition = "";
		String optional =
				"OPTIONAL MATCH (s)<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u) " +
				"OPTIONAL MATCH (u)<-[:RELATED]-(child: User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c) ";

		JsonObject params = new JsonObject().put("structureId", structureId);

		//Activation
		if(filterObj.containsKey("activated")){
			String activated = filterObj.getString("activated", "false");
			if("false".equals(activated.toLowerCase())){
				condition = "WHERE NOT(u.activationCode IS NULL) ";
			} else if("true".equals(activated.toLowerCase())){
				condition = "WHERE (u.activationCode IS NULL) ";
			} else {
				condition = "WHERE 1 = 1 ";
			}
		} else {
			condition = "WHERE NOT(u.activationCode IS NULL) ";
		}

		//Profiles
		if(filterObj.getJsonArray("profiles").size() > 0){
			condition += "AND p.name IN {profilesArray} ";
			params.put("profilesArray", filterObj.getJsonArray("profiles"));
		}

		//Levels
		if(filterObj.getJsonArray("levels").size() > 0){
			condition += " AND u.level IN {levelsArray} ";
			params.put("levelsArray", filterObj.getJsonArray("levels"));
		}

		//Classes
		if(filterObj.getJsonArray("classes").size() > 0){
			filter += ", (c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u) ";
			optional = "OPTIONAL MATCH (u)<-[:RELATED]-(child: User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c) ";
			condition += " AND c.id IN {classesArray} ";
			params.put("classesArray", filterObj.getJsonArray("classes"));
		}

		//Email
		if(hasMail != null) {
			if(hasMail){
				condition += " AND COALESCE(u.email, \"\") <> \"\" ";
			} else {
				condition += " AND COALESCE(u.email, \"\") = \"\" ";
			}

		}

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
			if(filterObj.getJsonArray("classes").size() < 1){
				results.handle(new Either.Left<String, JsonArray>("forbidden"));
				return;
			}

			UserInfos.Function f = userInfos.getFunctions().get(CLASS_ADMIN);
			List<String> scope = f.getScope();
			if (scope != null && !scope.isEmpty()) {
				condition = "AND c.id IN {scope} ";
				params.put("scope", new fr.wseduc.webutils.collections.JsonArray(scope));
			}
		}

		//With clause
		String withStr =
				"WITH u, p ";

		//Return clause
		String returnStr =
				"RETURN distinct collect(p.name)[0] as profile, " +
				"u.id as id, u.firstName as firstName, u.lastName as lastName, " +
				"u.email as email, u.login as login, u.activationCode as activationCode ";

		if(groupClasses){
			withStr += ", collect(distinct c.name) as classes, min(c.name) as classname, CASE count(c) WHEN 0 THEN false ELSE true END as isInClass ";
			returnStr += ", classes, classname, isInClass ";
		} else {
			withStr += ", c.name as classname, CASE count(c) WHEN 0 THEN false ELSE true END as isInClass ";
			returnStr += ", classname, isInClass ";
		}

		if(groupChildren){
			withStr += ", CASE count(child) WHEN 0 THEN null ELSE collect(distinct {firstName: child.firstName, lastName: child.lastName, classname: c.name}) END as children ";
			returnStr += ", filter(c IN children WHERE not(c.firstName is null)) as children ";
		} else {
			if(groupClasses) {
				withStr =
					"WITH u, p, c, " +
					"CASE count(child) WHEN 0 THEN null " +
					"ELSE {firstName: child.firstName, lastName: child.lastName, classname: c.name} " +
					"END as child " + withStr + ", child ";
			} else {
				withStr += ", CASE count(child) WHEN 0 THEN null ELSE {firstName: child.firstName, lastName: child.lastName } END as child ";
			}
			returnStr += ", child ";
		}

		//Order by
		String sort = "ORDER BY ";
		for(Object sortObj: filterObj.getJsonArray("sort")){
			String sortstr = (String) sortObj;
			sort += sortstr + ",";
		}
		sort += "lastName";

		String query = filter + condition + optional + withStr + returnStr + sort;

		neo.execute(query.toString(), params, validResultHandler(results));
	}

	public void massMailUser(String userId, UserInfos userInfos, Handler<Either<String, JsonArray>> results){
		String filter =
				"MATCH (s:Structure)<-[:DEPENDS]-(g:ProfileGroup)<-[:IN]-(u:User {id: {userId}}), "+
						"(g)-[:HAS_PROFILE]-(p: Profile) ";
		String condition = "";
		String optional =
				"OPTIONAL MATCH (s)<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u) " +
						"OPTIONAL MATCH (u)<-[:RELATED]-(child: User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c) ";

		JsonObject params = new JsonObject().put("userId", userId);

		//With clause
		String withStr =
				"WITH u, p ";

		//Return clause
		String returnStr =
				"RETURN distinct collect(p.name)[0] as profile, " +
						"u.id as id, u.firstName as firstName, u.lastName as lastName, " +
						"u.email as email, u.login as login, u.activationCode as activationCode ";

		withStr += ", collect(distinct c.name) as classes, min(c.name) as classname, CASE count(c) WHEN 0 THEN false ELSE true END as isInClass ";
		returnStr += ", classes, classname, isInClass ";

		withStr += ", CASE count(child) WHEN 0 THEN null ELSE {firstName: child.firstName, lastName: child.lastName } END as child ";

		String query = filter + condition + optional + withStr + returnStr ;

		neo.execute(query.toString(), params, validResultHandler(results));
	}

	@Override
	public void massMailAllUsersByStructure(String structureId, UserInfos userInfos, Handler<Either<String, JsonArray>> results){
		String filter =
				"MATCH (s:Structure {id: {structureId}})<-[:DEPENDS]-(g:ProfileGroup)<-[:IN]-(u:User), "+
						"(g)-[:HAS_PROFILE]-(p: Profile) ";
		String condition = "";
		String optional =
				"OPTIONAL MATCH (s)<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u) " +
						"OPTIONAL MATCH (u)<-[:RELATED]-(child: User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c) ";

		JsonObject params = new JsonObject().put("structureId", structureId);

		//Admin check
		if (!userInfos.getFunctions().containsKey(SUPER_ADMIN) &&
				!userInfos.getFunctions().containsKey(ADMIN_LOCAL)) {
			results.handle(new Either.Left<String, JsonArray>("forbidden"));
			return;
		} else if (userInfos.getFunctions().containsKey(ADMIN_LOCAL)) {
			UserInfos.Function f = userInfos.getFunctions().get(ADMIN_LOCAL);
			List<String> scope = f.getScope();
			if (scope != null && !scope.isEmpty()) {
				condition += "WHERE s.id IN {scope} ";
				params.put("scope", new fr.wseduc.webutils.collections.JsonArray(scope));
			}
		}

		//With clause
		String withStr =
				"WITH u, p ";

		//Return clause
		String returnStr =
				"RETURN distinct collect(p.name)[0] as type, " +
						"u.id as id, u.firstName as firstName, u.lastName as lastName, " +
						"u.email as email, u.login as login, u.activationCode as code ";

		withStr += ", collect(distinct {id: c.id, name: c.name}) as classes, min(c.name) as classname, CASE count(c) WHEN 0 THEN false ELSE true END as isInClass ";
		returnStr += ", classes, classname, isInClass ";

		withStr += ", CASE count(child) WHEN 0 THEN null ELSE collect(distinct {firstName: child.firstName, lastName: child.lastName, classname: c.name}) END as children ";
		returnStr += ", filter(c IN children WHERE not(c.firstName is null)) as children ";

		String sort = "ORDER BY lastName";

		String query = filter + condition + optional + withStr + returnStr + sort;

		neo.execute(query.toString(), params, validResultHandler(results));
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
			"RETURN collect(DISTINCT EXTRACT(function IN u.functions | last(split(function, \"$\")))) as aafFunctions";

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
			"u.source as source, u.deleteDate as deleteDate, u.disappearanceDate as disappearanceDate, u.blocked as blocked," +
			"EXTRACT(function IN u.functions | last(split(function, \"$\"))) as aafFunctions," +
			"CASE WHEN class IS NULL THEN [] ELSE COLLECT(distinct {id: class.id, name: class.name}) END as classes," +
			"CASE WHEN fgroup IS NULL THEN [] ELSE COLLECT(distinct fgroup.name) END as functionalGroups, " +
			"CASE WHEN mgroup IS NULL THEN [] ELSE COLLECT(distinct mgroup.name) END as manualGroups, " +
			"CASE WHEN f IS NULL THEN [] ELSE COLLECT(distinct [f.externalId, rf.scope]) END as functions, " +
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
	public void searchCriteria(List<String> structures, Handler<Either<String, JsonObject>> handler) {
		final String query =
				"MATCH (s:Structure) " +
				"WHERE s.id IN {structures} " +
				"OPTIONAL MATCH (s)<-[:BELONGS]-(c:Class) " +
				"OPTIONAL MATCH (s)<-[:DEPENDS]-(fg:FunctionGroup) " +
				"RETURN COLLECT(DISTINCT { id: s.id, name: s.name}) as structures, " +
				"COLLECT(DISTINCT { id: c.id, name: c.name}) as classes, " +
				"COLLECT(DISTINCT fg.filter) as functions, " +
				"['Teacher', 'Personnel', 'Student', 'Relative', 'Guest'] as profiles ";
		neo.execute(query, new JsonObject().put("structures", new JsonArray(structures)), validUniqueResultHandler(handler));
	}

}
