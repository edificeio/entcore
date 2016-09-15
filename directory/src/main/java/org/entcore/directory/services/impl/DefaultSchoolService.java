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
import static org.entcore.common.user.DefaultFunctions.*;

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
				"MATCH (u:User { id: {id}})-[:IN]->(g: Group)-[:DEPENDS]->(s:Structure) " +
				"OPTIONAL MATCH (s)-[r:HAS_ATTACHMENT]->(ps:Structure) " +
				"WITH s, COLLECT(DISTINCT {id: ps.id, name: ps.name}) as parents " +
				"RETURN DISTINCT s.id as id, s.UAI as UAI, s.name as name, " +
				"CASE WHEN any(p in parents where p <> {id: null, name: null}) THEN parents END as parents";
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
		} else if (!userInfos.getFunctions().containsKey(SUPER_ADMIN) && userInfos.getFunctions().containsKey(ADMIN_LOCAL)) {
			UserInfos.Function f = userInfos.getFunctions().get(ADMIN_LOCAL);
			List<String> scope = f.getScope();
			if (scope != null && !scope.isEmpty()) {
				condition = "WHERE s.id IN {structures} ";
				params.putArray("structures", new JsonArray(scope.toArray()));
			}
		}

        String query =
                " MATCH (s:Structure) " + condition +
                    " OPTIONAL MATCH (s)-[r:HAS_ATTACHMENT]->(ps:Structure) " +
                    " OPTIONAL MATCH (s:Structure)<-[DEPENDS]-(pf:ProfileGroup) " +
                    " OPTIONAL MATCH (pf:ProfileGroup)-[HAS_PROFILE]->(pfile:Profile) " +
                    " WITH " +
                    " s, " +
                    " COLLECT( DISTINCT {id: ps.id, name: ps.name}) as parents, " +
                    " COLLECT( DISTINCT {id: pf.id, name:pf.name, profile:pfile.name, storage:pf.storage, quota:pf.quota, maxquota:pf.maxquota}) as pgroup " +
                    " RETURN s.id as id, s.UAI as UAI, s.name as name, s.externalId as externalId,  s.storage as storage, s.quota as quota, s.maxquota as maxquota, " +
                    " CASE WHEN any(p in parents where p <> {id: null, name: null}) THEN parents END as parents, " +
                    " CASE WHEN any(pg in pgroup where pg <> {id: null, name: null}) THEN pgroup END as pgroup ";

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

	@Override
	public void removeParent(String structureId, String parentStructureId, Handler<Either<String, JsonObject>> handler) {
		JsonObject action = new JsonObject()
			.putString("action", "manual-structure-detachment")
			.putString("structureId", structureId)
			.putString("parentStructureId", parentStructureId);
		eventBus.send(Directory.FEEDER, action, validUniqueResultHandler(handler));
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
	public void update(String structureId, JsonObject body, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.putString("action", "manual-update-structure")
				.putString("structureId", structureId)
				.putObject("data", body);
		eventBus.send(Directory.FEEDER, action, validUniqueResultHandler(result));
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

		JsonObject params = new JsonObject().putString("structureId", structureId);

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
				params.putArray("scope", new JsonArray(scope.toArray()));
			}
		} else if(userInfos.getFunctions().containsKey(CLASS_ADMIN)){
			UserInfos.Function f = userInfos.getFunctions().get(CLASS_ADMIN);
			List<String> scope = f.getScope();
			if (scope != null && !scope.isEmpty()) {
				condition = "AND class.id IN {scope} ";
				params.putArray("scope", new JsonArray(scope.toArray()));
			}
		}

		String query = filter + condition + filter2;

		neo.execute(query.toString(), params, validResultHandler(results));
	}

	@Override
	public void massmailUsers(String structureId, JsonObject filterObj, UserInfos userInfos, Handler<Either<String, JsonArray>> results) {
		this.massmailUsers(structureId, filterObj, true, true, userInfos, results);
	}
	@Override
	public void massmailUsers(String structureId, JsonObject filterObj,
			boolean groupClasses, boolean groupChildren, UserInfos userInfos, Handler<Either<String, JsonArray>> results) {

		String filter =
				"MATCH (s:Structure {id: {structureId}})<-[:DEPENDS]-(g:ProfileGroup)<-[:IN]-(u:User), "+
				"(g)-[:HAS_PROFILE]-(p: Profile) ";
		String condition = "";
		String optional =
				"OPTIONAL MATCH (s)<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u) " +
				"OPTIONAL MATCH (u)<-[:RELATED]-(child: User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c) ";

		JsonObject params = new JsonObject().putString("structureId", structureId);

		//Activation
		if(filterObj.containsField("activated")){
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
		if(filterObj.getArray("profiles").size() > 0){
			condition += "AND p.name IN {profilesArray} ";
			params.putArray("profilesArray", filterObj.getArray("profiles"));
		}

		//Levels
		if(filterObj.getArray("levels").size() > 0){
			condition += " AND u.level IN {levelsArray} ";
			params.putArray("levelsArray", filterObj.getArray("levels"));
		}

		//Classes
		if(filterObj.getArray("classes").size() > 0){
			filter += ", (c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u) ";
			optional = "OPTIONAL MATCH (u)<-[:RELATED]-(child: User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c) ";
			condition += " AND c.id IN {classesArray} ";
			params.putArray("classesArray", filterObj.getArray("classes"));
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
				params.putArray("scope", new JsonArray(scope.toArray()));
			}
		} else if(userInfos.getFunctions().containsKey(CLASS_ADMIN)){
			if(filterObj.getArray("classes").size() < 1){
				results.handle(new Either.Left<String, JsonArray>("forbidden"));
				return;
			}

			UserInfos.Function f = userInfos.getFunctions().get(CLASS_ADMIN);
			List<String> scope = f.getScope();
			if (scope != null && !scope.isEmpty()) {
				condition = "AND c.id IN {scope} ";
				params.putArray("scope", new JsonArray(scope.toArray()));
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
			withStr += ", CASE count(child) WHEN 0 THEN null ELSE {firstName: child.firstName, lastName: child.lastName";
			if(groupClasses)
				withStr += ", classname: c.name";
			withStr += "} END as child ";
			returnStr += ", child ";
		}

		//Order by
		String sort = "ORDER BY ";
		for(Object sortObj: filterObj.getArray("sort")){
			String sortstr = (String) sortObj;
			sort += sortstr + ",";
		}
		sort += "lastName";

		String query = filter + condition + optional + withStr + returnStr + sort;

		neo.execute(query.toString(), params, validResultHandler(results));
	}

}
