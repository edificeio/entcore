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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import fr.wseduc.webutils.Either;

import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.email.EmailSender;
import org.entcore.common.email.EmailFactory;
import org.joda.time.DateTime;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.shareddata.ConcurrentSharedMap;
import org.vertx.java.platform.Container;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.validation.StringValidation;

import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.security.BCrypt;

public class DefaultUserAuthAccount implements UserAuthAccount {

	private final Neo neo;
	private final Vertx vertx;
	private final Container container;
	private final EmailSender notification;
	private final Renders render;

	private String smsProvider;
	private final String smsAddress;

	public DefaultUserAuthAccount(Vertx vertx, Container container) {
		EventBus eb = Server.getEventBus(vertx);
		this.neo = new Neo(vertx, eb, container.logger());
		this.vertx = vertx;
		this.container = container;
		EmailFactory emailFactory = new EmailFactory(vertx, container, container.config());
		notification = emailFactory.getSender();
		render = new Renders(vertx, container);
		ConcurrentSharedMap<Object, Object> server = vertx.sharedData().getMap("server");
		if(server != null && server.get("smsProvider") != null) {
			smsProvider = (String) server.get("smsProvider");
			final String node = (String) server.get("node");
			smsAddress = (node != null ? node : "") + "entcore.sms";
		} else {
			smsAddress = "entcore.sms";
		}
	}

	@Override
	public void activateAccount(final String login, String activationCode, final String password,
			String email, String phone, final HttpServerRequest request, final Handler<Either<String, String>> handler) {
		String query =
				"MATCH (n:User) " +
				"WHERE n.login = {login} AND n.activationCode = {activationCode} AND n.password IS NULL " +
				"AND (NOT EXISTS(n.blocked) OR n.blocked = false) " +
				"OPTIONAL MATCH n-[r:DUPLICATE]-() " +
				"OPTIONAL MATCH (p:Profile) " +
				"WHERE HAS(n.profiles) AND p.name = head(n.profiles) " +
				"WITH n, FILTER(x IN COLLECT(distinct r.score) WHERE x > 3) as duplicates, p.blocked as blockedProfile " +
				"WHERE LENGTH(duplicates) = 0 AND (blockedProfile IS NULL OR blockedProfile = false) " +
				"SET n.password = {password}, n.activationCode = null, n.email = {email}, n.mobile = {phone} " +
				"RETURN n.password as password, n.id as id, HEAD(n.profiles) as profile ";
		Map<String, Object> params = new HashMap<>();
		params.put("login", login);
		params.put("activationCode", activationCode);
		params.put("password", BCrypt.hashpw(password, BCrypt.gensalt()));
		params.put("email", email);
		params.put("phone", phone);
		neo.send(query, params, new Handler<Message<JsonObject>>(){

			@Override
			public void handle(Message<JsonObject> res) {
				if ("ok".equals(res.body().getString("status"))
						&& res.body().getObject("result").getObject("0") != null) {
					JsonObject jo = new JsonObject()
							.putString("userId", res.body().getObject("result").getObject("0").getString("id"))
							.putString("profile", res.body().getObject("result").getObject("0").getString("profile"))
							.putObject("request", new JsonObject()
									.putObject("headers", new JsonObject()
											.putString("Accept-Language", I18n.acceptLanguage(request))
											.putString("Host", Renders.getHost(request))
									)
							);
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
	public void matchActivationCode(final String login, String potentialActivationCode,
			final Handler<Boolean> handler) {
		String query =
				"MATCH (n:User) " +
				"WHERE n.login = {login} AND n.activationCode = {activationCode} AND n.password IS NULL " +
				"AND (NOT EXISTS(n.blocked) OR n.blocked = false) " +
				"RETURN true as exists";

		JsonObject params = new JsonObject()
			.putString("login", login)
			.putString("activationCode", potentialActivationCode);
		neo.execute(query, params, Neo4jResult.validUniqueResultHandler(new Handler<Either<String,JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if(event.isLeft() || !event.right().getValue().getBoolean("exists", false))
					handler.handle(false);
				else
					handler.handle(true);
			}
		}));
	}

	@Override
	public void matchResetCode(final String login, String potentialResetCode,
			final Handler<Boolean> handler) {
		String query =
				"MATCH (n:User) " +
				"WHERE n.login = {login} AND n.resetCode = {resetCode} " +
				"RETURN true as exists";

		JsonObject params = new JsonObject()
			.putString("login", login)
			.putString("resetCode", potentialResetCode);
		neo.execute(query, params, Neo4jResult.validUniqueResultHandler(new Handler<Either<String,JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if(event.isLeft() || !event.right().getValue().getBoolean("exists", false))
					handler.handle(false);
				else
					handler.handle(true);
			}
		}));
	}

	@Override
	public void findByMail(final String email, final Handler<Either<String,JsonObject>> handler) {
		String query = "MATCH (u:User) WHERE u.email = {mail} AND u.activationCode IS NULL RETURN u.login as login, u.mobile as mobile";
		JsonObject params = new JsonObject().putString("mail", email);

		neo.execute(query, params, Neo4jResult.validUniqueResultHandler(handler));
	}

	@Override
	public void findByLogin(final String login, final String resetCode, final Handler<Either<String,JsonObject>> handler) {
		boolean setResetCode = resetCode != null && !resetCode.trim().isEmpty();

		String basicQuery =
			"MATCH (n:User) " +
			"WHERE n.login = {login} " +
			"AND n.activationCode IS NULL " +
			"AND (NOT(HAS(n.federated)) OR n.federated = false) " +
			(setResetCode ? "SET n.resetCode = {resetCode}, n.resetDate = {today} " : "") +
			"RETURN n.email as email, n.mobile as mobile";

		final String teacherQuery =
			"MATCH (n:User)-[:IN]->(sg:ProfileGroup)-[:DEPENDS]->(m:Class)" +
			"<-[:DEPENDS]-(tg:ProfileGroup)<-[:IN]-(p:User), " +
			"sg-[:DEPENDS]->(psg:ProfileGroup)-[:HAS_PROFILE]->(sp:Profile {name:'Student'}), " +
			"tg-[:DEPENDS]->(ptg:ProfileGroup)-[:HAS_PROFILE]->(tp:Profile {name:'Teacher'}) " +
			"WHERE n.login = {login} AND NOT(p.email IS NULL) AND n.activationCode IS NULL AND " +
			"(NOT(HAS(n.federated)) OR n.federated = false) " +
			(setResetCode ? "SET n.resetCode = {resetCode}, n.resetDate = {today} " : "") +
			"RETURN p.email as email";

		final JsonObject params = new JsonObject().putString("login", login);
		if(setResetCode)
			params.putString("resetCode", resetCode)
				.putNumber("today", new Date().getTime());

		neo.execute(basicQuery, params, Neo4jResult.validUniqueResultHandler(new Handler<Either<String,JsonObject>>() {
			public void handle(Either<String, JsonObject> result) {
				if(result.isLeft()){
					handler.handle(result);
					return;
				}

				final String mail = result.right().getValue().getString("email");
				final String mobile = result.right().getValue().getString("mobile");

				if(mail != null && container.config().getBoolean("teacherForgotPasswordEmail", false)){
					neo.execute(teacherQuery, params, Neo4jResult.validUniqueResultHandler(new Handler<Either<String,JsonObject>>() {
						public void handle(Either<String, JsonObject> resultTeacher) {
							if(resultTeacher.isLeft()){
								handler.handle(resultTeacher);
								return;
							}

							resultTeacher.right().getValue().putString("mobile", mobile);
							handler.handle(resultTeacher);
						}
					}));
				}
				handler.handle(result);
			}
		}));
	}

	@Override
	public void sendResetPasswordMail(HttpServerRequest request, String email, String resetCode,
			final Handler<Either<String, JsonObject>> handler) {
		if (email == null || resetCode == null || email.trim().isEmpty() || resetCode.trim().isEmpty()) {
			handler.handle(new Either.Left<String, JsonObject>("invalid.mail"));
			return;
		}
		JsonObject json = new JsonObject()
				.putString("host", notification.getHost(request))
				.putString("resetUri", notification.getHost(request) + "/auth/reset/" + resetCode);
		container.logger().debug(json.encode());
		notification.sendEmail(
				request,
				email,
				container.config().getString("email", "noreply@one1d.fr"),
				null,
				null,
				"mail.reset.pw.subject",
				"email/forgotPassword.html",
				json,
				true,
				new Handler<Message<JsonObject>>() {
					public void handle(Message<JsonObject> event) {
						if("error".equals(event.body().getString("status"))){
							handler.handle(new Either.Left<String, JsonObject>(event.body().getString("message", "")));
						} else {
							handler.handle(new Either.Right<String, JsonObject>(event.body()));
						}
					}
				});
	}

	@Override
	public void sendForgottenIdMail(HttpServerRequest request, String login, String email, final Handler<Either<String, JsonObject>> handler){
		if (email == null || email.trim().isEmpty()) {
			handler.handle(new Either.Left<String, JsonObject>("invalid.mail"));
			return;
		}

		JsonObject json = new JsonObject()
			.putString("login", login)
			.putString("host", notification.getHost(request));
		container.logger().debug(json.encode());
		notification.sendEmail(
				request,
				email,
				container.config().getString("email", "noreply@one1d.fr"),
				null,
				null,
				"mail.reset.id.subject",
				"email/forgotId.html",
				json,
				true,
				new Handler<Message<JsonObject>>() {
					public void handle(Message<JsonObject> event) {
						if("error".equals(event.body().getString("status"))){
							handler.handle(new Either.Left<String, JsonObject>(event.body().getString("message", "")));
						} else {
							handler.handle(new Either.Right<String, JsonObject>(event.body()));
						}
					}
				});
	}

	private void sendSms(HttpServerRequest request, final String phone, String template, JsonObject params, final Handler<Either<String, JsonObject>> handler){
		if (phone == null || phone.trim().isEmpty()) {
			handler.handle(new Either.Left<String, JsonObject>("invalid.phone"));
			return;
		}

		final String formattedPhone = StringValidation.formatPhone(phone);

		render.processTemplate(request, template, params, new Handler<String>() {
			@Override
			public void handle(String body) {
				if (body != null) {
					JsonObject smsObject = new JsonObject()
						.putString("provider", smsProvider)
		    			.putString("action", "send-sms")
		    			.putObject("parameters", new JsonObject()
		    				.putArray("receivers", new JsonArray().add(formattedPhone))
		    				.putString("message", body)
		    				.putBoolean("senderForResponse", true)
		    				.putBoolean("noStopClause", true));

					vertx.eventBus().send(smsAddress, smsObject, new Handler<Message<JsonObject>>() {
						public void handle(Message<JsonObject> event) {
							if("error".equals(event.body().getString("status"))){
								handler.handle(new Either.Left<String, JsonObject>(event.body().getString("message", "")));
							} else {
								handler.handle(new Either.Right<String, JsonObject>(event.body()));
							}
						}
					});
				} else {
					handler.handle(new Either.Left<String, JsonObject>("template.error"));
				}
			}
		});
	}

	@Override
	public void sendResetPasswordSms(HttpServerRequest request, String phone, String resetCode, final Handler<Either<String, JsonObject>> handler){
		JsonObject params = new JsonObject()
			.putString("resetCode", resetCode)
			.putString("resetUri", resetCode);

		sendSms(request, phone, "phone/forgotPassword.txt", params, handler);
	}

	@Override
	public void sendForgottenIdSms(HttpServerRequest request, String login, String phone, final Handler<Either<String, JsonObject>> handler){
		JsonObject params = new JsonObject()
			.putString("login", login);

		sendSms(request, phone, "phone/forgotId.txt", params, handler);
	}

	@Override
	public void resetPassword(String login, String resetCode, String password, final Handler<Boolean> handler) {
		final long codeDelay = container.config().getInteger("resetCodeDelay", 0);

		String query =
				"MATCH (n:User) " +
				"WHERE n.login = {login} AND n.resetCode = {resetCode} " +
				(codeDelay > 0 ? "AND coalesce({today} - n.resetDate < {delay}, true) " : "") +
				"SET n.password = {password}, n.resetCode = null, n.resetDate = null " +
				"RETURN n.password as pw";
		Map<String, Object> params = new HashMap<>();
		params.put("login", login);
		params.put("resetCode", resetCode);
		if(codeDelay > 0){
			params.put("today", new Date().getTime());
			params.put("delay", codeDelay);
		}
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
				"WHERE n.login = {login} AND n.activationCode IS NULL AND " +
				"(NOT(HAS(n.federated)) OR n.federated = false) " +
				"SET n.resetCode = {resetCode}, n.resetDate = {today} " +
				"RETURN count(n) as nb";
		final String code = StringValidation.generateRandomCode(8);
		JsonObject params = new JsonObject().putString("login", login).putString("resetCode", code).putNumber("today", new Date().getTime());
		neo.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status")) &&
						event.body().getArray("result") != null && event.body().getArray("result").size() == 1 &&
						1 == ((JsonObject) event.body().getArray("result").get(0)).getInteger("nb")) {
					sendResetPasswordMail(request, email, code, new Handler<Either<String, JsonObject>>() {
						public void handle(Either<String, JsonObject> event) {
							handler.handle(event.isRight());
						}
					});
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

	@Override
	public void storeDomain(String id, String domain, String scheme, final Handler<Boolean> handler) {
		String query =
				"MATCH (u:User {id: {id}}) " +
				"SET u.lastDomain = {domain}, u.lastScheme = {scheme}, u.lastLogin = {now} " +
				"return count(*) = 1 as exists";
		JsonObject params = new JsonObject()
			.putString("id", id)
			.putString("domain", domain)
			.putString("scheme", scheme)
			.putString("now", DateTime.now().toString());
		neo.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				handler.handle("ok".equals(r.body().getString("status")) &&
						r.body().getArray("result") != null && r.body().getArray("result").get(0) != null &&
						((JsonObject) r.body().getArray("result").get(0)).getBoolean("exists", false));
			}
		});
	};

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
