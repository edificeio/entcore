package org.entcore.directory.services.impl;

import edu.one.core.infra.Either;
import edu.one.core.infra.NotificationHelper;
import edu.one.core.infra.Utils;
import org.entcore.common.neo4j.Neo;
import org.entcore.datadictionary.generation.ActivationCodeGenerator;
import org.entcore.datadictionary.generation.DisplayNameGenerator;
import org.entcore.datadictionary.generation.IdGenerator;
import org.entcore.datadictionary.generation.LoginGenerator;
import org.entcore.directory.services.UserService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.UUID;

import static org.entcore.common.neo4j.Neo4jResult.*;
import static org.entcore.common.neo4j.Neo4jUtils.nodeSetPropertiesFromJson;

public class DefaultUserService implements UserService {

	private final Neo neo;
	private final LoginGenerator loginGenerator;
	private final DisplayNameGenerator displayNameGenerator;
	private final ActivationCodeGenerator activationCodeGenerator;
	private final IdGenerator idGenerator;
	private final NotificationHelper notification;
	private Logger logger = LoggerFactory.getLogger(DefaultUserService.class);

	public DefaultUserService(Neo neo, NotificationHelper notification) {
		this.neo = neo;
		this.notification = notification;
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

	@Override
	public void sendUserCreatedEmail(final HttpServerRequest request, String userId,
			final Handler<Either<String, Boolean>> result) {
		String query =
				"MATCH (u:`User` { id : {id}}) WHERE NOT(u.email IS NULL) AND NOT(u.activationCode IS NULL) " +
				"RETURN u.login as login, u.email as email, u.activationCode as activationCode ";
		JsonObject params = new JsonObject().putString("id", userId);
		neo.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				Either<String, JsonObject> r = validUniqueResult(m);
				if (r.isRight()) {
					JsonObject j = r.right().getValue();
					String email = j.getString("email");
					String login = j.getString("login");
					String activationCode = j.getString("activationCode");
					if (email == null || login == null || activationCode == null ||
							email.trim().isEmpty() || login.trim().isEmpty() || activationCode.trim().isEmpty()) {
						result.handle(new Either.Left<String, Boolean>("user.invalid.values"));
						return;
					}
					JsonObject json = new JsonObject()
							.putString("activationUri", notification.getHost() +
									"/auth/activation?login=" + login +
									"&activationCode=" + activationCode)
							.putString("login", login);
					logger.debug(json.encode());
					notification.sendEmail(request, email, null, null,
							"email.user.created", "email/userCreated.txt", json, true,
							new Handler<Message<JsonObject>>() {

								@Override
								public void handle(Message<JsonObject> message) {
									if ("ok".equals(message.body().getString("status"))) {
										result.handle(new Either.Right<String, Boolean>(true));
									} else {
										result.handle(new Either.Left<String, Boolean>(
												message.body().getString("message")));
									}
								}
							});
				} else {
					result.handle(new Either.Left<String, Boolean>(r.left().getValue()));
				}
			}
		});
	}

	@Override
	public void get(String id, Handler<Either<String, JsonObject>> result) {
		String query =
				"MATCH (u:`User` { id : {id}}) " +
				"RETURN HEAD(filter(x IN labels(u) WHERE x <> 'Visible' AND x <> 'User')) as type, u";
		neo.execute(query, new JsonObject().putString("id", id), fullNodeMergeHandler("u", result));
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
				"MATCH (c:`Class` { id : {classId}}) " +
				"<-[:DEPENDS]-(csg:ClassRelativeGroup)-[:DEPENDS]->(ssg:SchoolRelativeGroup) " +
				"CREATE csg<-[:APPARTIENT]-(u:Relative:User:Visible {props}), " +
				"ssg<-[:APPARTIENT]-u " +
				"WITH u, c " +
				"MATCH (student:Student)-[:APPARTIENT]->(c) " +
				"WHERE student.id IN {childrenIds} " +
				"CREATE student-[:EN_RELATION_AVEC]->u " +
				"RETURN DISTINCT u.id as id";
		neo.execute(query, params, validUniqueResultHandler(result));
	}

}
