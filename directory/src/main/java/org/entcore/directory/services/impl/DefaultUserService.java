package org.entcore.directory.services.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.NotificationHelper;
import org.entcore.common.neo4j.Neo;
import org.entcore.directory.Directory;
import org.entcore.directory.services.UserService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import static org.entcore.common.neo4j.Neo4jResult.*;

public class DefaultUserService implements UserService {

	private final Neo neo;
	private final NotificationHelper notification;
	private final EventBus eb;
	private Logger logger = LoggerFactory.getLogger(DefaultUserService.class);

	public DefaultUserService(Neo neo, NotificationHelper notification, EventBus eb) {
		this.neo = neo;
		this.notification = notification;
		this.eb = eb;
	}

	@Override
	public void createInClass(String classId, JsonObject user, Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.putString("action", "manual-create-user")
				.putString("classId", classId)
				.putString("profile", user.getString("type"))
				.putObject("data", user);
		eb.send(Directory.FEEDER, action, validUniqueResultHandler(result));
	}

	@Override
	public void update(final String id, final JsonObject user, final Handler<Either<String, JsonObject>> result) {
		JsonObject action = new JsonObject()
				.putString("action", "manual-update-user")
				.putString("userId", id)
				.putObject("data", user);
		eb.send(Directory.FEEDER, action, validUniqueResultHandler(result));
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
							.putString("host", notification.getHost())
							.putString("login", login);
					logger.debug(json.encode());
					notification.sendEmail(request, email, null, null,
							"email.user.created.info", "email/userCreated.html", json, true,
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
				"OPTIONAL MATCH u-[:IN]->(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
				"RETURN DISTINCT p.name as type, u";
		neo.execute(query, new JsonObject().putString("id", id), fullNodeMergeHandler("u", result));
	}

}
