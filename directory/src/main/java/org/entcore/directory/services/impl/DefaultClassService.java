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
import static org.entcore.common.neo4j.Neo4jResult.*;
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
	public void findUsers(String classId, JsonArray expectedTypes, boolean collectRelative,
						  Handler<Either<String, JsonArray>> results) {
		JsonObject params = new JsonObject().put("classId", classId);
		//=== Filter by type
		String filterPart = "";
		if (expectedTypes != null && expectedTypes.size()  >= 1) {
			filterPart = " WHERE p.name IN {expected} ";
			params.put("expected", expectedTypes);
		}
		//=== Collect relative
		String collectPart = " WITH m, p, [] as relativeList ";
		if(collectRelative){
			collectPart = " WITH m, p OPTIONAL MATCH (m)-[:RELATED]->(relative) WITH m, p, "+
					"CASE WHEN relative IS NOT NULL THEN COLLECT(distinct {relatedName: relative.displayName, relatedId: relative.id, relatedType: relative.profiles}) ELSE [] END as relativeList ";
		}
		//=== Make query
		String query =
				"MATCH (c:`Class` { id : {classId}})<-[:DEPENDS]-(cpg:ProfileGroup)" +
				"-[:DEPENDS]->(spg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), cpg<-[:IN]-(m:User)-[:IN]->spg " +
						filterPart + collectPart +
				"RETURN distinct m.lastName as lastName, m.firstName as firstName, m.id as id, " +
				"(LENGTH(m.email)>0 AND EXISTS(m.email)) as hasEmail, " +
				"CASE WHEN m.loginAlias IS NOT NULL THEN m.loginAlias ELSE m.login END as login, m.login as originalLogin, m.activationCode as activationCode, m.birthDate as birthDate, " +
				"p.name as type, m.blocked as blocked, m.source as source, relativeList " +
				"ORDER BY type, lastName ";
		neo.execute(query, params, validResultHandler(results));
	}

	@Override
	public void findVisibles(UserInfos user, String classId, boolean collectRelative, Handler<JsonArray> handler) {
		JsonObject params = new JsonObject().put("classId", classId);
		//=== Collect relative
		String collectPart = " WITH visibles, p, [] as relativeList ";
		if(collectRelative){
			collectPart = " WITH visibles, p OPTIONAL MATCH (visibles)-[:RELATED]->(relative) WITH visibles, p, "+
					"CASE WHEN relative IS NOT NULL THEN COLLECT(distinct {relatedName: relative.displayName, relatedId: relative.id, relatedType: relative.profiles}) ELSE [] END as relativeList ";
		}
		//=== Make query
		String customReturn =
				"MATCH (c:`Class` { id : {classId}})<-[:DEPENDS]-(cpg:ProfileGroup)-[:DEPENDS]->(spg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), (cpg)<-[:IN]-(visibles:User) " +
					collectPart +
					"RETURN distinct visibles.displayName as displayName, visibles.lastName as lastName, visibles.firstName as firstName, " +
					"visibles.id as id, p.name as type, relativeList " +
					"ORDER BY type, lastName ";
		UserUtils.findVisibleUsers(eb, user.getUserId(), false, customReturn, params, handler);
	}

	@Override
	public void get(String classId, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(result, classId)) return;
		String query = "MATCH (c:`Class` { id : {classId}}) RETURN c.id as id, c.externalId as externalId,  c.name as name, c.level as level";
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
	public void link(String classId, JsonArray userIds, Handler<Either<String, JsonArray>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-add-users")
				.put("classId", classId)
				.put("userIds", userIds);
		eb.send(Directory.FEEDER, action, handlerToAsyncHandler(validResultsHandler(result)));
	}

	@Override
	public void unlink( String classId, String userId, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-remove-user")
				.put("classId", classId)
				.put("userId", userId);
		eb.send(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(result)));
	}

	@Override
	public void unlink(JsonArray classIds, JsonArray userIds, Handler<Either<String, JsonArray>> handler) {
		JsonObject action = new JsonObject()
				.put("action", "manual-remove-users")
				.put("classIds", classIds)
				.put("userIds", userIds);
		eb.send(Directory.FEEDER, action, handlerToAsyncHandler(validResultsHandler(handler)));
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
				params.put("scope", new fr.wseduc.webutils.collections.JsonArray(scope));
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
				"RETURN c.id as id, c.name as name , c.externalId as externalId";
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

	public void listDetachedUsers(JsonArray structureIds, UserInfos user, Handler<JsonArray> handler){
		JsonObject params = new JsonObject();
		params.put("structureIds",structureIds);
		//=== Make query
		String customReturn =
				"MATCH (p:Profile)<-[:HAS_PROFILE]-(g:ProfileGroup)<-[:IN]-(visibles:User)-[:IN]->()-[:DEPENDS]->(s:Structure) " +
						"WHERE s.id IN {structureIds} AND NOT (visibles)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(:Class) "+
						" WITH visibles, p, s " +
						"OPTIONAL MATCH (visibles)-[:RELATED]->(relative) WITH visibles, p, s, relative "+
						"RETURN distinct visibles.displayName as displayName, visibles.lastName as lastName, visibles.firstName as firstName, visibles.login as login, " +
						"CASE WHEN relative IS NOT NULL THEN COLLECT(distinct {relatedName: relative.displayName, relatedId: relative.id, relatedType: relative.profiles}) ELSE [] END as relativeList, "+
						"visibles.id as id, p.name as type, s.id as structureId, s.name as structureName ";
		UserUtils.findVisibleUsers(eb, user.getUserId(), false, customReturn, params, handler);
	}
}
