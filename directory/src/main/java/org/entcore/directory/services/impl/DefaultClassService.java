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
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.Directory;
import org.entcore.directory.services.ClassService;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.neo4j.Neo4jResult.*;
import static org.entcore.common.user.DefaultFunctions.ADMIN_LOCAL;
import static org.entcore.common.user.DefaultFunctions.CLASS_ADMIN;
import static org.entcore.common.user.DefaultFunctions.SUPER_ADMIN;

public class DefaultClassService implements ClassService {

	private static final Logger log = LoggerFactory.getLogger(DefaultClassService.class);
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
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(result)));
	}

	@Override
	public void update(String classId, JsonObject classe, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-update-class")
				.put("classId", classId)
				.put("data", classe);
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(result)));
	}

	@Override
	public void remove(String classId, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-remove-class")
				.put("classId", classId);
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(result)));
	}

	@Override
	public void findUsers(String classId, JsonArray expectedTypes, boolean collectRelative, 
						  boolean ine, Handler<Either<String, JsonArray>> results) {
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
				" OPTIONAL MATCH m-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure) WITH COLLECT(distinct s) as structureNodes, m, p, relativeList " +
				" OPTIONAL MATCH (sAuth:Structure)-[:HAS_AUTH_DEFAULT]->(auths:AuthDefault { profile: HEAD(m.profiles), auth: 'FEDERATED' }) WHERE sAuth IN structureNodes " +
				" WITH COLLECT(auths) as auths, m, p, relativeList "	+
				"RETURN distinct m.lastName as lastName, m.firstName as firstName, m.id as id, " +
				"(LENGTH(m.email)>0 AND EXISTS(m.email)) as hasEmail, " +
				"CASE WHEN m.loginAlias IS NOT NULL THEN m.loginAlias ELSE m.login END as login, m.login as originalLogin, m.activationCode as activationCode, m.displayName as displayName, m.birthDate as birthDate, m.lastLogin as lastLogin, " +
				(ine 
					? "m.ine as ine, " 
					: "") +
				"p.name as type, m.blocked as blocked, m.source as source, relativeList, " +
				" (HAS(m.federatedIDP) AND NOT(m.federatedIDP IS NULL) AND HAS(m.federated) AND m.federated = true) OR " +
				"  (size(auths) > 0 AND (m.source in ['AAF', 'AAF1D']) AND m.activationCode IS NOT NULL) as hasFederatedIdentity " +
				"ORDER BY type, lastName ";
		neo.execute(query, params, validResultHandler(results));
	}

	@Override
	public Future<JsonArray> findVisibles(UserInfos user, String classId, boolean collectRelative) {
		return listClassUsers(classId, collectRelative).compose(users -> keepVisibles(user.getUserId(), users));
	}

	/**
	 * Users of a class, whatever their profile, without any visibility filter. The relatives are not filtered
	 * either : they are given as soon as the user they are related to is.
	 * @return [{displayName, lastName, firstName, id, type, relativeList: [{relatedName, relatedId, relatedType}]}],
	 * relativeList being empty unless collectRelative is true
	 */
	Future<JsonArray> listClassUsers(String classId, boolean collectRelative) {
		final Promise<JsonArray> promise = Promise.promise();
		final JsonObject params = new JsonObject().put("classId", classId);
		//=== Collect relative
		String collectPart = " WITH m, p, [] as relativeList ";
		if(collectRelative){
			collectPart = " WITH m, p OPTIONAL MATCH (m)-[:RELATED]->(relative) WITH m, p, "+
					"CASE WHEN relative IS NOT NULL THEN COLLECT(distinct {relatedName: relative.displayName, relatedId: relative.id, relatedType: relative.profiles}) ELSE [] END as relativeList ";
		}
		//=== Make query
		final String query =
				"MATCH (c:`Class` { id : {classId}})<-[:DEPENDS]-(cpg:ProfileGroup)-[:DEPENDS]->(spg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), (cpg)<-[:IN]-(m:User) " +
					collectPart +
					"RETURN distinct m.displayName as displayName, m.lastName as lastName, m.firstName as firstName, " +
					"m.id as id, p.name as type, relativeList " +
					"ORDER BY type, lastName ";
		neo.execute(query, params, validResultHandler(result -> {
			if (result.isRight()) {
				promise.complete(result.right().getValue());
			} else {
				promise.fail(result.left().getValue());
			}
		}));
		return promise.future();
	}

	@Override
	public void get(String classId, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(result, classId)) return;
		String query = "MATCH (c:`Class` { id : {classId}}) RETURN c.id as id, c.externalId as externalId,  c.name as name, c.level as level";
		neo.execute(query, new JsonObject().put("classId", classId), validUniqueResultHandler(result));
	}

	@Override
	public void addSelf(final String classId, final UserInfos user, final Handler<Either<String, JsonObject>> result) {
		addUser(classId, user.getUserId(), user, true, result);
	}

	@Override
	public void addUser(final String classId, final String userId, final UserInfos user,
			final Handler<Either<String, JsonObject>> result) {
		addUser(classId, userId, user, false, result);
	}
	
	private void addUser(final String classId, final String userId, final UserInfos user, boolean self,
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
							UserUtils.filterFewOrGetAllVisibles(eb, user.getUserId(), new JsonArray().add(userId), self)
									.onSuccess(visibles -> {
										final boolean visible = visibles.stream()
												.anyMatch(o -> userId.equals(((JsonObject) o).getString("id")));
										if (visible) {
											attachToClass(classId, userId, t, result);
										} else {
											result.handle(new Either.Left<String, JsonObject>("user.not.visible"));
										}
									})
									.onFailure(e -> {
										log.error("[DefaultClassService.addUser] failed to check the visibility of " + userId, e);
										result.handle(new Either.Left<String, JsonObject>("user.not.visible"));
									});
						} else {
							result.handle(new Either.Left<String, JsonObject>("invalid.user"));
						}
					}
				});
	}

	/**
	 * Attach the user to the profile group of the class matching his/her profile and, for a student, attach
	 * his/her relatives to the relative group of the class, in a single transaction. No visibility check : the
	 * caller is expected to have checked it.
	 * @return {id, schoolId} through the handler, schoolId being the structure of the class ; user.not.visible
	 * when the class has no group for this profile, as the query this replaces did
	 */
	void attachToClass(final String classId, final String userId, final String profile,
			final Handler<Either<String, JsonObject>> result) {
		final JsonObject params = new JsonObject()
				.put("classId", classId)
				.put("uId", userId)
				.put("profile", profile);
		final StatementsBuilder statements = new StatementsBuilder()
				.add("MATCH (c:`Class` { id : {classId}})<-[:DEPENDS]-(cpg:ProfileGroup)" +
						"-[:DEPENDS]->(spg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile {name : {profile}}), " +
						"c-[:BELONGS]->(s:Structure), (u:User {id: {uId}}) " +
						"CREATE UNIQUE u-[:IN {source:'MANUAL'}]->cpg " +
						"RETURN DISTINCT u.id as id, s.id as schoolId", params);
		if ("Student".equals(profile)) {
			statements.add("MATCH (c:`Class` { id : {classId}})<-[:DEPENDS]-(cpg:ProfileGroup)-[:DEPENDS]->(spg:ProfileGroup)" +
					"-[:HAS_PROFILE]->(p:Profile {name : 'Relative'}), " +
					"(u:User {id: {uId}})-[:RELATED]->(relative: User), " +
					"(u)-[:IN]->(:ProfileGroup)-[:DEPENDS]->c " +
					"CREATE UNIQUE relative-[:IN {source:'MANUAL'}]->cpg " +
					"RETURN count(relative) as relativeNb", params);
		}
		neo.executeTransaction(statements.build(), null, true, validResultsHandler(results -> {
			if (results.isLeft()) {
				log.error("[DefaultClassService.attachToClass] failed to attach " + userId + " to " + classId + " : " +
						results.left().getValue());
				result.handle(new Either.Left<String, JsonObject>("error.while.attaching.user"));
				return;
			}
			final JsonArray attached = results.right().getValue().getJsonArray(0);
			if (attached != null && attached.size() == 1) {
				result.handle(new Either.Right<String, JsonObject>(attached.getJsonObject(0)));
			} else {
				result.handle(new Either.Left<String, JsonObject>("user.not.visible"));
			}
		}));
	}

	@Override
	public void link(String classId, String userId, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-add-user")
				.put("classId", classId)
				.put("userId", userId);
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(result)));
	}

	@Override
	public void link(String classId, JsonArray userIds, Handler<Either<String, JsonArray>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-add-users")
				.put("classId", classId)
				.put("userIds", userIds);
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validResultsHandler(result)));
	}

	@Override
	public void unlink( String classId, String userId, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.put("action", "manual-remove-user")
				.put("classId", classId)
				.put("userId", userId);
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(result)));
	}

	@Override
	public void unlink(JsonArray classIds, JsonArray userIds, Handler<Either<String, JsonArray>> handler) {
		JsonObject action = new JsonObject()
				.put("action", "manual-remove-users")
				.put("classIds", classIds)
				.put("userIds", userIds);
		eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validResultsHandler(handler)));
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
				"RETURN c.id as id, c.name as name , c.externalId as externalId, c.level";
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

	@Override
	public Future<JsonArray> listDetachedUsers(JsonArray structureIds, UserInfos user) {
		return listDetachedCandidates(structureIds).compose(users -> keepVisibles(user.getUserId(), users));
	}

	/**
	 * Users of the structures attached to none of their classes, without any visibility filter. The relatives
	 * are not filtered either. A user of several of the structures gives one row per structure.
	 * @return [{displayName, lastName, firstName, relativeList, id, type, structureId, structureName}]
	 */
	Future<JsonArray> listDetachedCandidates(JsonArray structureIds) {
		final Promise<JsonArray> promise = Promise.promise();
		final JsonObject params = new JsonObject().put("structureIds", structureIds);
		final String query =
				"MATCH (m:User)-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure), (pg)-[:HAS_PROFILE]->(p:Profile) " +
						"WHERE s.id IN {structureIds} AND NOT (m)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(:Class) " +
						" WITH m, p, s " +
						"OPTIONAL MATCH (m)-[:RELATED]->(relative) WITH m, p, s, relative " +
						"RETURN distinct m.displayName as displayName, m.lastName as lastName, m.firstName as firstName, " +
						"CASE WHEN relative IS NOT NULL THEN COLLECT(distinct {relatedName: relative.displayName, relatedId: relative.id, relatedType: relative.profiles}) ELSE [] END as relativeList, " +
						"m.id as id, p.name as type, s.id as structureId, s.name as structureName ";
		neo.execute(query, params, validResultHandler(result -> {
			if (result.isRight()) {
				promise.complete(result.right().getValue());
			} else {
				promise.fail(result.left().getValue());
			}
		}));
		return promise.future();
	}

	/**
	 * Keep the rows whose user is visible to userId, the user himself/herself excluded.
	 */
	private Future<JsonArray> keepVisibles(String userId, JsonArray users) {
		final JsonArray userIds = new JsonArray(users.stream()
				.map(o -> ((JsonObject) o).getString("id"))
				.distinct()
				.collect(Collectors.toList()));
		return UserUtils.filterFewOrGetAllVisibles(eb, userId, userIds, false).map(visibles -> {
			final Set<String> visibleIds = visibles.stream()
					.map(o -> ((JsonObject) o).getString("id"))
					.collect(Collectors.toSet());
			return users.stream()
					.filter(o -> visibleIds.contains(((JsonObject) o).getString("id")))
					.collect(Collector.of(JsonArray::new, JsonArray::add, JsonArray::addAll));
		});
	}

	@Override
	public Future<JsonArray> listUserbookClassMembers(String userId, String classId) {
		return listClassMembers(userId, classId).compose(members -> {
			final JsonArray memberIds = new JsonArray(members.stream()
					.map(o -> ((JsonObject) o).getString("id"))
					.distinct()
					.collect(Collectors.toList()));
			return UserUtils.filterFewOrGetAllVisibles(eb, userId, memberIds, true)
					.map(visibles -> markVisibles(members, visibles.stream()
							.map(o -> ((JsonObject) o).getString("id"))
							.collect(Collectors.toSet())));
		});
	}

	/**
	 * Students and teachers of a class, or of the classes of the user when classId is null or empty,
	 * without any visibility filter.
	 * @return [{type, id, displayName, mood, userId, photo}]
	 */
	Future<JsonArray> listClassMembers(String userId, String classId) {
		final Promise<JsonArray> promise = Promise.promise();
		final String matchClass;
		final JsonObject params = new JsonObject();
		if (classId == null || classId.trim().isEmpty()) {
			matchClass = "(n:User {id : {userId}})-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(c:Class) ";
			params.put("userId", userId);
		} else {
			matchClass = "(c:Class {id : {classId}}) ";
			params.put("classId", classId);
		}
		final String query = "MATCH " + matchClass +
				"WITH c " +
				"MATCH c<-[:DEPENDS]-(cpg:ProfileGroup)<-[:IN]-(m:User) " +
				"WHERE head(m.profiles) IN ['Student','Teacher'] " +
				"OPTIONAL MATCH m-[:USERBOOK]->u " +
				"RETURN distinct head(m.profiles) as type, m.id as id, " +
				"m.displayName as displayName, u.mood as mood, " +
				"u.userid as userId, u.picture as photo " +
				"ORDER BY type DESC, displayName ";
		neo.execute(query, params, validResultHandler(result -> {
			if (result.isRight()) {
				promise.complete(result.right().getValue());
			} else {
				promise.fail(result.left().getValue());
			}
		}));
		return promise.future();
	}

	/**
	 * Flag each member with its visibility, and drop the userbook details of the members the user cannot see.
	 */
	static JsonArray markVisibles(JsonArray members, Set<String> visibleIds) {
		final JsonArray marked = new JsonArray();
		members.forEach(o -> {
			final JsonObject member = ((JsonObject) o).copy();
			final boolean visible = visibleIds.contains(member.getString("id"));
			member.put("isVisible", visible);
			if (!visible) {
				member.remove("mood");
				member.remove("userId");
				member.remove("photo");
			}
			marked.add(member);
		});
		return marked;
	}
}
