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

package org.entcore.auth.users;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import org.entcore.common.neo4j.Neo;
import fr.wseduc.webutils.NotificationHelper;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.security.BCrypt;

public class DefaultUserAuthAccount implements UserAuthAccount {

	private final Neo neo;
	private final Vertx vertx;
	private final Container container;
	private final NotificationHelper notification;

	public DefaultUserAuthAccount(Vertx vertx, Container container) {
		EventBus eb = Server.getEventBus(vertx);
		this.neo = new Neo(vertx, eb, container.logger());
		this.vertx = vertx;
		this.container = container;
		notification = new NotificationHelper(vertx, eb, container);
	}

	@Override
	public void activateAccount(final String login, String activationCode, final String password,
			final Handler<Either<String, String>> handler) {
		String query =
				"MATCH (n:User) " +
				"WHERE n.login = {login} AND n.activationCode = {activationCode} AND n.password IS NULL " +
				"SET n.password = {password}, n.activationCode = null " +
				"RETURN n.password as password, n.id as id";
		Map<String, Object> params = new HashMap<>();
		params.put("login", login);
		params.put("activationCode", activationCode);
		params.put("password", BCrypt.hashpw(password, BCrypt.gensalt()));
		neo.send(query, params, new Handler<Message<JsonObject>>(){

			@Override
			public void handle(Message<JsonObject> res) {
				if ("ok".equals(res.body().getString("status"))
						&& res.body().getObject("result").getObject("0") != null) {
					JsonObject jo = new JsonObject().putString(
							"userId", 
							res.body().getObject("result").getObject("0").getString("id"));
					Server.getEventBus(vertx).publish("activation.ack", jo);
					handler.handle(new Either.Right<String, String>(
							res.body().getObject("result").getObject("0").getString("id")));
				} else {
					String q =
							"MATCH (n:User) " +
							"WHERE n.login = {login} AND n.activationCode IS NULL " +
							"AND NOT(n.password IS NULL) " +
							"RETURN n.password as password, n.id as id";
					Map<String, Object> p = new HashMap<>();
					p.put("login", login);
					neo.send(q, p, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							if ("ok".equals(event.body().getString("status")) &&
									event.body().getObject("result").getObject("0") != null &&
									BCrypt.checkpw(password, event.body().getObject("result").getObject("0")
											.getString("password", ""))) {
								handler.handle(new Either.Right<String, String>(
										event.body().getObject("result").getObject("0").getString("id")));
							} else {
								handler.handle(new Either.Left<String, String>("invalid.activation"));
							}
						}
					});
				}
			}
		});
	}

	@Override
	public void forgotPassword(final HttpServerRequest request, final String login,
			final Handler<Boolean> handler) {
		String query =
				"MATCH (n:User) " +
				"WHERE n.login = {login} AND NOT(n.email IS NULL) AND n.activationCode IS NULL " +
				"SET n.resetCode = {resetCode} " +
				"RETURN n.email as email";
		final String query2 =
				"MATCH (n:User)-[:IN]->(sg:ProfileGroup)-[:DEPENDS]->(m:Class)" +
				"<-[:DEPENDS]-(tg:ProfileGroup)<-[:IN]-(p:User), " +
				"sg-[:DEPENDS]->(psg:ProfileGroup)-[:HAS_PROFILE]->(sp:Profile {name:'Student'}), " +
				"tg-[:DEPENDS]->(ptg:ProfileGroup)-[:HAS_PROFILE]->(tp:Profile {name:'Teacher'}) " +
				"WHERE n.login = {login} AND NOT(p.email IS NULL) AND n.activationCode IS NULL " +
				"SET n.resetCode = {resetCode} " +
				"RETURN p.email as email";
		final Map<String, Object> params = new HashMap<>();
		params.put("login", login);
		final String resetCode = UUID.randomUUID().toString();
		params.put("resetCode", resetCode);
		neo.send(query, params, new Handler<Message<JsonObject>>(){

			@Override
			public void handle(Message<JsonObject> res) {
				if ("ok".equals(res.body().getString("status"))) {
					JsonObject json = res.body().getObject("result");
					if (json.getObject("0") != null &&
							json.getObject("0").getString("email") != null &&
							!json.getObject("0").getString("email").trim().isEmpty()) {
						sendResetPasswordLink(request, login, json.getObject("0")
								.getString("email"), resetCode, handler);
					} else if (container.config().getBoolean("teacherForgotPasswordEmail", false)) {
						neo.send(query2, params, new Handler<Message<JsonObject>>(){

							@Override
							public void handle(Message<JsonObject> event) {
								JsonObject j = event.body().getObject("result");
								if ("ok".equals(event.body().getString("status")) &&
										j.getObject("0") != null &&
										j.getObject("0").getString("email") != null &&
										!j.getObject("0").getString("email").trim().isEmpty()) {
									sendResetPasswordLink(request, login, j.getObject("0")
											.getString("email"), resetCode, handler);
								} else {
									handler.handle(false);
								}
							}
						});
					} else {
						handler.handle(false);
					}
				} else {
					handler.handle(false);
				}
			}
		});
	}

	private void sendResetPasswordLink(HttpServerRequest request, String login, String email, String resetCode,
			final Handler<Boolean> handler) {
		if (email == null || resetCode == null || email.trim().isEmpty() || resetCode.trim().isEmpty()) {
			handler.handle(false);
			return;
		}
		JsonObject json = new JsonObject()
				.putString("login", login)
				.putString("host", notification.getHost())
				.putString("resetUri", notification.getHost() + "/auth/reset/" + resetCode);
		container.logger().debug(json.encode());
		notification.sendEmail(request, email, container.config()
				.getString("email", "noreply@one1d.fr"), null, null,
				"mail.reset.pw.subject", "email/forgotPassword.html", json, true,
				new Handler<Message<JsonObject>>() {

				@Override
				public void handle(Message<JsonObject> message) {
					handler.handle("ok".equals(message.body().getString("status")));
				}
			});
	}

	@Override
	public void resetPassword(String login, String resetCode, String password, final Handler<Boolean> handler) {
		String query =
				"MATCH (n:User) " +
				"WHERE n.login = {login} AND n.resetCode = {resetCode} " +
				"SET n.password = {password}, n.resetCode = null " +
				"RETURN n.password as pw";
		Map<String, Object> params = new HashMap<>();
		params.put("login", login);
		params.put("resetCode", resetCode);
		updatePassword(handler, query, password, params);
	}

	@Override
	public void changePassword(String login, String password, final Handler<Boolean> handler) {
		String query =
				"MATCH (n:User) " +
				"WHERE n.login = {login} AND NOT(n.password IS NULL) " +
				"SET n.password = {password} " +
				"RETURN n.password as pw";
		Map<String, Object> params = new HashMap<>();
		params.put("login", login);
		updatePassword(handler, query, password, params);
	}

	@Override
	public void sendResetCode(final HttpServerRequest request, final String login, final String email,
			final Handler<Boolean> handler) {
		String query =
				"MATCH (n:User) " +
				"WHERE n.login = {login} AND n.activationCode IS NULL " +
				"SET n.resetCode = {resetCode} " +
				"RETURN count(n) as nb";
		final String code = UUID.randomUUID().toString();
		JsonObject params = new JsonObject().putString("login", login).putString("resetCode", code);
		neo.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status")) &&
						event.body().getArray("result") != null && event.body().getArray("result").size() == 1 &&
						1 == ((JsonObject) event.body().getArray("result").get(0)).getInteger("nb")) {
					sendResetPasswordLink(request, login, email, code, handler);
				} else {
					handler.handle(false);
				}
			}
		});
	}

	@Override
	public void blockUser(String id, boolean block, final Handler<Boolean> handler) {
		String query = "MATCH (n:`User` { id : {id}}) SET n.blocked = {block} return count(*) = 1 as exists";
		JsonObject params = new JsonObject().putString("id", id).putBoolean("block", block);
		neo.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				handler.handle("ok".equals(r.body().getString("status")) &&
						r.body().getArray("result") != null && r.body().getArray("result").get(0) != null &&
						((JsonObject) r.body().getArray("result").get(0)).getBoolean("exists", false));
			}
		});
	}

	private void updatePassword(final Handler<Boolean> handler, String query, String password, Map<String, Object> params) {
		final String pw = BCrypt.hashpw(password, BCrypt.gensalt());
		params.put("password", pw);
		neo.send(query, params, new Handler<Message<JsonObject>>(){

			@Override
			public void handle(Message<JsonObject> res) {
				JsonObject r = res.body().getObject("result");
				handler.handle("ok".equals(res.body().getString("status"))
						&& r.getObject("0") != null
						&& pw.equals(r.getObject("0").getString("pw")));
			}
		});
	}

}
