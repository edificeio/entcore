package org.entcore.directory.services.impl;

import fr.wseduc.webutils.Either;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.Directory;
import org.entcore.directory.services.ClassService;
import org.entcore.directory.services.UserService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;

public class DefaultClassService implements ClassService {

	private final Neo neo;
	private final EventBus eb;

	public DefaultClassService(Neo neo, EventBus eb) {
		this.neo = neo;
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
