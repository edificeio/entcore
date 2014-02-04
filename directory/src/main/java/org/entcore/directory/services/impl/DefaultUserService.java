package org.entcore.directory.services.impl;

import edu.one.core.infra.Either;
import edu.one.core.infra.Utils;
import org.entcore.common.neo4j.Neo;
import org.entcore.datadictionary.generation.ActivationCodeGenerator;
import org.entcore.datadictionary.generation.DisplayNameGenerator;
import org.entcore.datadictionary.generation.IdGenerator;
import org.entcore.datadictionary.generation.LoginGenerator;
import org.entcore.directory.services.UserService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.UUID;

import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;
import static org.entcore.common.neo4j.Neo4jUtils.nodeSetPropertiesFromJson;

public class DefaultUserService implements UserService {

	private final Neo neo;
	private final LoginGenerator loginGenerator;
	private final DisplayNameGenerator displayNameGenerator;
	private final ActivationCodeGenerator activationCodeGenerator;
	private final IdGenerator idGenerator;

	public DefaultUserService(Neo neo) {
		this.neo = neo;
		activationCodeGenerator = new ActivationCodeGenerator();
		displayNameGenerator = new DisplayNameGenerator();
		loginGenerator = new LoginGenerator();
		idGenerator = new IdGenerator();
	}

	@Override
	public void createInClass(String classId, JsonObject user, Handler<Either<String, JsonObject>> result) {
		if (user == null) {
			user = new JsonObject();
		}
		UserType type;
		try {
			type = UserType.valueOf(user.getString("type"));
		} catch (Exception e) {
			result.handle(new Either.Left<String, JsonObject>("invalid.user.type"));
			return;
		}
		JsonObject c = Utils.validAndGet(user, type.getFields(), type.getRequiredFields());
		if (Utils.defaultValidationError(c, result, classId)) return;
		c.putString("id", UUID.randomUUID().toString())
				.putString("displayName", displayNameGenerator.generate(
						c.getString("firstName"), c.getString("lastName")))
				.putString("login", loginGenerator.generate(
						c.getString("firstName"), c.getString("lastName")))
				.putString("activationCode", activationCodeGenerator.generate())
				.putString("externalId", idGenerator.generate());
		switch (type) {
			case Student: createStudent(classId, c, result);
				break;
			case Teacher: createTeacher(classId, c, result);
				break;
			case Relative: createRelative(classId, c, result);
				break;
			case Principal: createPrincipal(classId, c, result);
				break;
			default: result.handle(new Either.Left<String, JsonObject>("unexpected.user.type"));
		}
	}

	@Override
	public void update(final String id, final JsonObject user, final Handler<Either<String, JsonObject>> result) {
		neo.execute("MATCH (u:`User` {id : {id}}) RETURN HEAD(filter(x IN labels(u)" +
				" WHERE x <> 'Visible' AND x <> 'User')) as type", new JsonObject().putString("id", id),
				new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				JsonArray res = r.body().getArray("result");
				if ("ok".equals(r.body().getString("status")) && res != null && res.size() == 1) {
					UserType type;
					try {
						JsonObject t = res.get(0);
						type = UserType.valueOf(t.getString("type"));
					} catch (Exception e) {
						result.handle(new Either.Left<String, JsonObject>("invalid.user.type"));
						return;
					}
					JsonObject u = Utils.validAndGet(user, type.getUpdateFields(), type.getUpdateRequiredFields());
					if (Utils.defaultValidationError(u, result, id)) return;
					String query =
							"MATCH (u:`User` {id : {id}}) " +
							"SET " + nodeSetPropertiesFromJson("u", u) +
							"RETURN u.id as id";
					neo.execute(query, u.putString("id", id), validUniqueResultHandler(result));
				} else {
					result.handle(new Either.Left<String, JsonObject>("invalid.user"));
				}
			}
		});
	}

	private void createStudent(String classId, JsonObject c, Handler<Either<String, JsonObject>> result) {
		JsonObject params = new JsonObject().putString("classId", classId).putObject("props", c);
		String query =
				"MATCH (c:`Class` { id : {classId}})<-[:DEPENDS]-(csg:ClassStudentGroup)" +
				"-[:DEPENDS]->(ssg:SchoolStudentGroup)-[:DEPENDS]->(s:School) " +
				"CREATE c<-[:APPARTIENT]-(u:Student:User:Visible {props}), " +
				"csg<-[:APPARTIENT]-u, ssg<-[:APPARTIENT]-u, s<-[:APPARTIENT]-u " +
				"RETURN u.id as id";
		neo.execute(query, params, validUniqueResultHandler(result));
	}

	private void createTeacher(String classId, JsonObject c, Handler<Either<String, JsonObject>> result) {
		JsonObject params = new JsonObject().putString("classId", classId).putObject("props", c);
		String query =
				"MATCH (c:`Class` { id : {classId}})<-[:DEPENDS]-(csg:ClassTeacherGroup)" +
				"-[:DEPENDS]->(ssg:SchoolTeacherGroup)-[:DEPENDS]->(s:School) " +
				"CREATE c<-[:APPARTIENT]-(u:Teacher:User:Visible {props}), " +
				"csg<-[:APPARTIENT]-u, ssg<-[:APPARTIENT]-u, s<-[:APPARTIENT]-u " +
				"RETURN u.id as id";
		neo.execute(query, params, validUniqueResultHandler(result));
	}

	private void createPrincipal(String classId, JsonObject c, Handler<Either<String, JsonObject>> result) {
		JsonObject params = new JsonObject().putString("classId", classId).putObject("props", c);
		String query =
				"MATCH (c:`Class` { id : {classId}})<-[:DEPENDS]-(csg:ClassTeacherGroup)" +
				"-[:DEPENDS]->(ssg:SchoolTeacherGroup)-[:DEPENDS]->(s:School)<-[:DEPENDS]-(spg:SchoolPrincipalGroup) " +
				"CREATE c<-[:APPARTIENT]-(u:Teacher:User:Visible {props}), " +
				"csg<-[:APPARTIENT]-u, ssg<-[:APPARTIENT]-u, s<-[:APPARTIENT]-u, spg<-[:APPARTIENT]-u " +
				"RETURN u.id as id";
		neo.execute(query, params, validUniqueResultHandler(result));
	}

	private void createRelative(String classId, JsonObject c, Handler<Either<String, JsonObject>> result) {
		JsonObject params = new JsonObject().putString("classId", classId).putObject("props", c)
				.putArray("childrenIds", c.getArray("childrenIds"));
		c.removeField("childrenIds");
		String query =
				"MATCH (student:Student)-[:APPARTIENT]->(c:`Class` { id : {classId}})" +
				"<-[:DEPENDS]-(csg:ClassRelativeGroup)-[:DEPENDS]->(ssg:SchoolRelativeGroup) " +
				"WHERE student.id IN {childrenIds} " +
				"CREATE student-[:EN_RELATION_AVEC]->(u:Relative:User:Visible {props}), " +
				"csg<-[:APPARTIENT]-u, ssg<-[:APPARTIENT]-u " +
				"RETURN u.id as id";
		neo.execute(query, params, validUniqueResultHandler(result));
	}

}
