package org.entcore.directory.services.impl;

import edu.one.core.infra.Either;
import edu.one.core.infra.Utils;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.services.ClassService;
import org.entcore.directory.services.UserService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Collections;
import java.util.UUID;

import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;
import static org.entcore.common.neo4j.Neo4jUtils.nodeSetPropertiesFromJson;

public class DefaultClassService implements ClassService {

	private final Neo neo;
	private final EventBus eb;

	public DefaultClassService(Neo neo, EventBus eb) {
		this.neo = neo;
		this.eb = eb;
	}

	@Override
	public void create(String schoolId, JsonObject classe, final Handler<Either<String, JsonObject>> result) {
		if (classe == null) {
			classe = new JsonObject();
		}
		String classId = UUID.randomUUID().toString();
		classe.putString("id", classId);
		JsonObject c = Utils.validAndGet(classe, CLASS_FIELDS, CLASS_REQUIRED_FIELDS);
		if (validationError(c, result)) return;
		String query =
				"MATCH (n:`School` { id : {schoolId}}) " +
				"CREATE n<-[:APPARTIENT]-(c:Class {props})," +
				"c<-[:DEPENDS]-(spg:ProfileGroup:Visible:ClassProfileGroup:ClassStudentGroup {studentGroup})," +
				"c<-[:DEPENDS]-(tpg:ProfileGroup:Visible:ClassProfileGroup:ClassTeacherGroup {teacherGroup})," +
				"c<-[:DEPENDS]-(rpg:ProfileGroup:Visible:ClassProfileGroup:ClassRelativeGroup {relativeGroup}) " +
				"RETURN c.id as id";
		final String className = c.getString("name");
		JsonObject params = new JsonObject()
				.putString("schoolId", schoolId)
				.putObject("props", c)
				.putObject("studentGroup", new JsonObject()
						.putString("id", UUID.randomUUID().toString())
						.putString("name", className + "_ELEVE")
				).putObject("teacherGroup", new JsonObject()
						.putString("id", UUID.randomUUID().toString())
						.putString("name", className + "_ENSEIGNANT")
				).putObject("relativeGroup", new JsonObject()
						.putString("id", UUID.randomUUID().toString())
						.putString("name", className + "_PERSRELELEVE")
				);
		JsonObject p = new JsonObject().putString("schoolId", schoolId).putString("classId", classId);
		StatementsBuilder queries = new StatementsBuilder()
				.add(query, params)
				.add("MATCH (n:`School` { id : {schoolId}})<-[:DEPENDS]-(sg:SchoolStudentGroup), " +
						"(c:`Class` { id : {classId}})<-[:DEPENDS]-(cg:ClassStudentGroup) " +
						"CREATE UNIQUE cg-[:DEPENDS]->sg", p)
				.add("MATCH (n:`School` { id : {schoolId}})<-[:DEPENDS]-(sg:SchoolTeacherGroup), " +
						"(c:`Class` { id : {classId}})<-[:DEPENDS]-(cg:ClassTeacherGroup) " +
						"CREATE UNIQUE cg-[:DEPENDS]->sg", p)
				.add("MATCH (n:`School` { id : {schoolId}})<-[:DEPENDS]-(sg:SchoolRelativeGroup), " +
						"(c:`Class` { id : {classId}})<-[:DEPENDS]-(cg:ClassRelativeGroup) " +
						"CREATE UNIQUE cg-[:DEPENDS]->sg", p);
		neo.executeTransaction(queries.build(), null, true, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				JsonArray results = r.body().getArray("results");
				if ("ok".equals(r.body().getString("status")) && results != null) {
					JsonArray createResult = results.get(0);
					result.handle(new Either.Right<String, JsonObject>((JsonObject) createResult.get(0)));
				} else {
					result.handle(new Either.Left<String, JsonObject>(r.body().getString("errors")));
				}
			}
		});
	}

	@Override
	public void update(String classId, JsonObject classe, Handler<Either<String, JsonObject>> result) {
		JsonObject c = Utils.validAndGet(classe, UPDATE_CLASS_FIELDS, Collections.<String>emptyList());
		if (validationError(c, result, classId)) return;
		String name = c.getString("name");
		if (name != null && name.trim().isEmpty()) {
			result.handle(new Either.Left<String, JsonObject>("invalid.empty.name"));
			return;
		}
		String query;
		c.putString("classId", classId);
		if (name != null) {
			query =
					"match (c:`Class` { id : {classId}}), c<-[:DEPENDS]-(sg:ClassStudentGroup), " +
					"c<-[:DEPENDS]-(tg:ClassTeacherGroup), c<-[:DEPENDS]-(rg:ClassRelativeGroup) " +
					"SET " + nodeSetPropertiesFromJson("c", c) +
					", sg.name = {studentName}, tg.name = {teacherName}, rg.name = {relativeName}";
			c.putString("studentName", name + "_ELEVE");
			c.putString("teacherName", name + "_ENSEIGNANT");
			c.putString("relativeName", name + "_PERSRELELEVE");
		} else {
			query = "match (c:`Class` { id : {classId}) SET " + nodeSetPropertiesFromJson("c", c);
		}
		neo.execute(query, c, validUniqueResultHandler(result));
	}

	@Override
	public void findUsers(String classId, UserService.UserType[] expectedTypes,
						  Handler<Either<String, JsonArray>> results) {
		StringBuilder type = new StringBuilder();
		if (expectedTypes == null || expectedTypes.length < 1) {
			type.append("m:User");
		} else {
			for (UserService.UserType t : expectedTypes) {
				type.append(" OR m:").append(t.name());
			}
			type.delete(0, 4);
		}
		String query =
				"MATCH (c:`Class` { id : {classId}})<-[:APPARTIENT]-(n:User)-[:EN_RELATION_AVEC*0..1]->(m) " +
				"WHERE (" + type + ") " +
				"RETURN distinct m.lastName as lastName, m.firstName as firstName, m.id as id, " +
				"m.login as login, m.activationCode as activationCode, m.birthDate as birthDate, " +
				"HEAD(filter(x IN labels(m) WHERE x <> 'Visible' AND x <> 'User')) as type, m.blocked as blocked " +
				"ORDER BY type, login ";
		neo.execute(query, new JsonObject().putString("classId", classId), validResultHandler(results));
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
		neo.execute("MATCH (u:`User` {id : {id}}) RETURN HEAD(filter(x IN labels(u)" +
				" WHERE x <> 'Visible' AND x <> 'User')) as type", new JsonObject().putString("id", userId),
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> r) {
						JsonArray res = r.body().getArray("result");
						if ("ok".equals(r.body().getString("status")) && res != null && res.size() == 1) {
							String t = ((JsonObject)res.get(0)).getString("type");
							String addRelativeToClassGroup = " ";
							if ("Relative".equals(t)) {
								result.handle(new Either.Left<String, JsonObject>("forbidden.add.relative.in.class"));
								return;
							} else if ("Student".equals(t)) {
								addRelativeToClassGroup = ", p-[:APPARTIENT]->rg ";
							}
							String customReturn =
									"MATCH (c:`Class` { id : {classId}})<-[:DEPENDS]-(cg:Class" + t + "Group), " +
									"c<-[:DEPENDS]-(rg:ClassRelativeGroup), c-[:APPARTIENT]->(s:School) " +
									"WHERE visibles.id = {uId} " +
									"OPTIONAL MATCH visibles-[:EN_RELATION_AVEC]->(p:Relative) " +
									"CREATE UNIQUE visibles-[:APPARTIENT]->cg, visibles-[:APPARTIENT]->c" +
									addRelativeToClassGroup +
									"RETURN DISTINCT visibles.id as id, s.id as schoolId";
							JsonObject params = new JsonObject()
									.putString("classId", classId)
									.putString("uId", userId);
							UserUtils.findVisibleUsers(eb, user.getUserId(), customReturn, params,
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

	private boolean validationError(JsonObject c,
		Handler<Either<String, JsonObject>> result, String ... params) {
		if (c == null) {
			result.handle(new Either.Left<String, JsonObject>("school.invalid.fields"));
			return true;
		}
		return validationParamsError(result, params);
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
