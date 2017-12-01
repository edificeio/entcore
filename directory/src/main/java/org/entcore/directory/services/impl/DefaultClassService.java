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
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.Directory;
import org.entcore.directory.services.ClassService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
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
				.put("action", "manual-create-class")
				.put("structureId", schoolId)
				.put("data", classe);
		eb.send(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(result)));
	}

	@Override
	public void update(String classId, JsonObject classe, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-update-class")
				.put("classId", classId)
				.put("data", classe);
		eb.send(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(result)));
	}

	@Override
	public void findUsers(String classId, JsonArray expectedTypes,
						  Handler<Either<String, JsonArray>> results) {
		String filter;
		JsonObject params = new JsonObject().put("classId", classId);
		if (expectedTypes == null || expectedTypes.size() < 1) {
			filter = "";
		} else {
			filter = "WHERE p.name IN {expected} ";
			params.put("expected", expectedTypes);
		}
		String query =
				"MATCH (c:`Class` { id : {classId}})<-[:DEPENDS]-(cpg:ProfileGroup)" +
				"-[:DEPENDS]->(spg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), cpg<-[:IN]-(m:User) " +
				filter +
				"RETURN distinct m.lastName as lastName, m.firstName as firstName, m.id as id, " +
				"m.login as login, m.activationCode as activationCode, m.birthDate as birthDate, " +
				"p.name as type, m.blocked as blocked, m.source as source " +
				"ORDER BY type, lastName ";
		neo.execute(query, params, validResultHandler(results));
	}

	@Override
	public void get(String classId, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(result, classId)) return;
		String query = "MATCH (c:`Class` { id : {classId}}) RETURN c.id as id,  c.name as name, c.level as level";
		neo.execute(query, new JsonObject().put("classId", classId), validUniqueResultHandler(result));
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
				"RETURN distinct p.name as type", new JsonObject().put("id", userId),
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> r) {
						JsonArray res = r.body().getJsonArray("result");
						if ("ok".equals(r.body().getString("status")) && res != null && res.size() == 1) {
							final String t = (res.getJsonObject(0)).getString("type");
							String customReturn =
									"MATCH (c:`Class` { id : {classId}})<-[:DEPENDS]-(cpg:ProfileGroup)" +
									"-[:DEPENDS]->(spg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile {name : {profile}}), " +
									"c-[:BELONGS]->(s:Structure) " +
									"WHERE visibles.id = {uId} " +
									"CREATE UNIQUE visibles-[:IN]->cpg " +
									"RETURN DISTINCT visibles.id as id, s.id as schoolId";
							JsonObject params = new JsonObject()
									.put("classId", classId)
									.put("uId", userId)
									.put("profile", t);
							UserUtils.findVisibleUsers(eb, user.getUserId(), false, customReturn, params,
									new Handler<JsonArray>() {

								@Override
								public void handle(final JsonArray users) {
									if (users != null && users.size() == 1) {
										if ("Student".equals(t)) {
											String query =
													"MATCH (c:`Class` { id : {classId}})<-[:DEPENDS]-(cpg:ProfileGroup)-[:DEPENDS]->(spg:ProfileGroup)" +
													"-[:HAS_PROFILE]->(p:Profile {name : 'Relative'}), " +
													"(u:User {id: {uId}})-[:RELATED]->(relative: User) " +
													"CREATE UNIQUE relative-[:IN]->cpg " +
													"RETURN count(relative) as relativeNb";

											JsonObject params = new JsonObject()
												.put("classId", classId)
												.put("uId", userId);

											neo.execute(query, params, Neo4jResult.validEmptyHandler(new Handler<Either<String,JsonObject>>() {
												public void handle(Either<String, JsonObject> event) {
													if(event.isLeft()){
														result.handle(new Either.Left<String, JsonObject>("error.while.attaching.relatives"));
													} else {
														result.handle(new Either.Right<String, JsonObject>(users.getJsonObject(0)));
													}
												}
											}));
										} else {
											result.handle(new Either.Right<String, JsonObject>(users.getJsonObject(0)));
										}
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
				.put("action", "manual-add-user")
				.put("classId", classId)
				.put("userId", userId);
		eb.send(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(result)));
	}

	@Override
	public void unlink(String classId, String userId, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-remove-user")
				.put("classId", classId)
				.put("userId", userId);
		eb.send(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(result)));
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
				params.put("scope", new JsonArray(scope));
			}
		}

		if (structureId != null && !structureId.trim().isEmpty()) {
			if (condition.isEmpty()) {
				condition = "WHERE s.id = {structure} ";
			} else {
				condition += ") AND s.id = {structure} ";
			}
			params.put("structure", structureId);
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
