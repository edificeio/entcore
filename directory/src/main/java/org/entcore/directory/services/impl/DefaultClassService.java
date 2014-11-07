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
import org.entcore.common.user.UserUtils;
import org.entcore.directory.Directory;
import org.entcore.directory.services.ClassService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;
import static org.entcore.common.user.DefaultFunctions.ADMIN_LOCAL;
import static org.entcore.common.user.DefaultFunctions.CLASS_ADMIN;
import static org.entcore.common.user.DefaultFunctions.SUPER_ADMIN;

public class DefaultClassService implements ClassService {

	private final Neo4j neo = Neo4j.getInstance();
	private final EventBus eb;

	public DefaultClassService(EventBus eb) {
		this.eb = eb;
	}

	@Override
	public void create(String schoolId, JsonObject classe, final Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.putString("action", "manual-create-class")
				.putString("structureId", schoolId)
				.putObject("data", classe);
		eb.send(Directory.FEEDER, action, validUniqueResultHandler(result));
	}

	@Override
	public void update(String classId, JsonObject classe, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.putString("action", "manual-update-class")
				.putString("classId", classId)
				.putObject("data", classe);
		eb.send(Directory.FEEDER, action, validUniqueResultHandler(result));
	}

	@Override
	public void findUsers(String classId, JsonArray expectedTypes,
						  Handler<Either<String, JsonArray>> results) {
		String filter;
		JsonObject params = new JsonObject().putString("classId", classId);
		if (expectedTypes == null || expectedTypes.size() < 1) {
			filter = "";
		} else {
			filter = "WHERE p.name IN {expected} ";
			params.putArray("expected", expectedTypes);
		}
		String query =
				"MATCH (c:`Class` { id : {classId}})<-[:DEPENDS]-(cpg:ProfileGroup)" +
				"-[:DEPENDS]->(spg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), cpg<-[:IN]-(m:User) " +
				filter +
				"RETURN distinct m.lastName as lastName, m.firstName as firstName, m.id as id, " +
				"m.login as login, m.activationCode as activationCode, m.birthDate as birthDate, " +
				"p.name as type, m.blocked as blocked " +
				"ORDER BY type, lastName ";
		neo.execute(query, params, validResultHandler(results));
	}

	@Override
	public void get(String classId, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(result, classId)) return;
		String query = "MATCH (c:`Class` { id : {classId}}) RETURN c.id as id,  c.name as name, c.level as level";
		neo.execute(query, new JsonObject().putString("classId", classId), validUniqueResultHandler(result));
	}

	@Override
	public void addUser(final String classId, final String userId, final UserInfos user,
			final Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(result, classId, userId)) return;
		if (user == null) {
			result.handle(new Either.Left<String, JsonObject>("invalid.userinfos"));
			return;
		}
		neo.execute("MATCH (u:`User` {id : {id}})-[:IN]->(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
				"RETURN p.name as type", new JsonObject().putString("id", userId),
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> r) {
						JsonArray res = r.body().getArray("result");
						if ("ok".equals(r.body().getString("status")) && res != null && res.size() == 1) {
							String t = ((JsonObject)res.get(0)).getString("type");
							String addRelativeToClassGroup = " ";
							if ("Student".equals(t)) {
								addRelativeToClassGroup =
										"WITH c, visibles, s " +
										"MATCH c<-[:DEPENDS]-(cpg:ProfileGroup)-[:DEPENDS]->(spg:ProfileGroup)" +
										"-[:HAS_PROFILE]->(p:Profile {name : 'Relative'}), " +
										"visibles-[:RELATED]->(relative:User)" +
										"CREATE UNIQUE relative-[:IN]->cpg ";
							}
							String customReturn =
									"MATCH (c:`Class` { id : {classId}})<-[:DEPENDS]-(cpg:ProfileGroup)" +
									"-[:DEPENDS]->(spg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile {name : {profile}}), " +
									"c-[:BELONGS]->(s:School) " +
									"WHERE visibles.id = {uId} " +
									"CREATE UNIQUE visibles-[:IN]->cpg " +
									addRelativeToClassGroup +
									"RETURN DISTINCT visibles.id as id, s.id as schoolId";
							JsonObject params = new JsonObject()
									.putString("classId", classId)
									.putString("uId", userId)
									.putString("profile", t);
							UserUtils.findVisibleUsers(eb, user.getUserId(), false, customReturn, params,
									new Handler<JsonArray>() {

								@Override
								public void handle(JsonArray users) {
									if (users != null && users.size() == 1) {
										result.handle(new Either.Right<String, JsonObject>(
												(JsonObject) users.get(0)));
									} else {
										result.handle(new Either.Left<String, JsonObject>("user.not.visible"));
									}
								}
							});
						} else {
							result.handle(new Either.Left<String, JsonObject>("invalid.user"));
						}
			}
		});
	}

	@Override
	public void link(String classId, String userId, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.putString("action", "manual-add-user")
				.putString("classId", classId)
				.putString("userId", userId);
		eb.send(Directory.FEEDER, action, validUniqueResultHandler(result));
	}

	@Override
	public void unlink(String classId, String userId, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.putString("action", "manual-remove-user")
				.putString("classId", classId)
				.putString("userId", userId);
		eb.send(Directory.FEEDER, action, validUniqueResultHandler(result));
	}

	@Override
	public void listAdmin(String structureId, UserInfos userInfos, Handler<Either<String, JsonArray>> results) {
		if (userInfos == null) {
			results.handle(new Either.Left<String, JsonArray>("invalid.user"));
			return;
		}
		String condition = "";
		JsonObject params = new JsonObject();
		if (!userInfos.getFunctions().containsKey(SUPER_ADMIN) &&
				!userInfos.getFunctions().containsKey(ADMIN_LOCAL) &&
				!userInfos.getFunctions().containsKey(CLASS_ADMIN)) {
			results.handle(new Either.Left<String, JsonArray>("forbidden"));
			return;
		} else if (userInfos.getFunctions().containsKey(ADMIN_LOCAL) ||
				userInfos.getFunctions().containsKey(CLASS_ADMIN)) {
			UserInfos.Function f = userInfos.getFunctions().get(ADMIN_LOCAL);
			List<String> scope = f.getScope();
			if (scope != null && !scope.isEmpty()) {
				condition = "WHERE (s.id IN {scope} OR c.id IN {scope}";
				params.putArray("scope", new JsonArray(scope.toArray()));
			}
		}

		if (structureId != null && !structureId.trim().isEmpty()) {
			if (condition.isEmpty()) {
				condition = "WHERE s.id = {structure} ";
			} else {
				condition += ") AND s.id = {structure} ";
			}
			params.putString("structure", structureId);
		} else if (!condition.isEmpty()) {
			condition += ") ";
		}
		String query =
				"MATCH (c:Class)-[:BELONGS]->(s:Structure) " + condition +
				"RETURN c.id as id, c.name as name ";
		neo.execute(query, params, validResultHandler(results));
	}

	private boolean validationParamsError(Handler<Either<String, JsonObject>> result, String ... params) {
		if (params.length > 0) {
			for (String s : params) {
				if (s == null) {
					result.handle(new Either.Left<String, JsonObject>("school.invalid.parameter"));
					return true;
				}
			}
		}
		return false;
	}

}
