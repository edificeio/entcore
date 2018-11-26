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

package org.entcore.auth.users;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import fr.wseduc.webutils.Either;

import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.email.EmailSender;

import org.entcore.auth.pojo.SendPasswordDestination;
import io.vertx.core.shareddata.LocalMap;
import org.entcore.common.email.EmailFactory;
import org.joda.time.DateTime;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.validation.StringValidation;

import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.security.BCrypt;

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.Utils.isNotEmpty;

public class DefaultUserAuthAccount implements UserAuthAccount {

	private final Neo neo;
	private final Vertx vertx;
	private final JsonObject config;
	private final EmailSender notification;
	private final Renders render;

	private String smsProvider;
	private final String smsAddress;
	private final JsonArray allowActivateDuplicateProfiles;

	public DefaultUserAuthAccount(Vertx vertx, JsonObject config) {
		EventBus eb = Server.getEventBus(vertx);
		this.neo = new Neo(vertx, eb, null);
		this.vertx = vertx;
		this.config = config;
		EmailFactory emailFactory = new EmailFactory(vertx, config);
		notification = emailFactory.getSender();
		render = new Renders(vertx, config);
		LocalMap<Object, Object> server = vertx.sharedData().getLocalMap("server");
		if(server != null && server.get("smsProvider") != null) {
			smsProvider = (String) server.get("smsProvider");
			final String node = (String) server.get("node");
			smsAddress = (node != null ? node : "") + "entcore.sms";
		} else {
			smsAddress = "entcore.sms";
		}
		this.allowActivateDuplicateProfiles = getOrElse(
				config.getJsonArray("allow-activate-duplicate"), new JsonArray().add("Relative"));
	}

	@Override
	public void activateAccount(final String login, String activationCode, final String password,
			String email, String phone, final String theme, final HttpServerRequest request, final Handler<Either<String, String>> handler) {
		activateAccount("login", login, activationCode, password, email, phone, theme, request, handler);
	}

	@Override
	public void activateAccountByLoginAlias(final String login, String activationCode, final String password,
		String email, String phone, final String theme, final HttpServerRequest request, final Handler<Either<String, String>> handler) {
		activateAccount("loginAlias", login, activationCode, password, email, phone, theme, request, handler);
	}

	private void activateAccount(final String loginFieldName, final String login, String activationCode, final String password,
	 	String email, String phone, final String theme, final HttpServerRequest request, final Handler<Either<String, String>> handler) {
		String query =
				"MATCH (n:User) " +
				"WHERE n." + loginFieldName + "={login} AND n.activationCode = {activationCode} AND n.password IS NULL " +
				"AND (NOT EXISTS(n.blocked) OR n.blocked = false) " +
				"OPTIONAL MATCH n-[r:DUPLICATE]-() " +
				"WHERE NOT(head(n.profiles) IN {allowActivateDuplicate}) " +
				"OPTIONAL MATCH (p:Profile) " +
				"WHERE HAS(n.profiles) AND p.name = head(n.profiles) " +
				"WITH n, LENGTH(FILTER(x IN COLLECT(distinct r.score) WHERE x > 3)) as duplicates, p.blocked as blockedProfile " +
				"WHERE (blockedProfile IS NULL OR blockedProfile = false) " +
				"FOREACH (duplicate IN CASE duplicates WHEN 0 THEN [1] ELSE [] END | " +
				"SET n.password = {password}, n.activationCode = null, n.email = {email}, n.mobile = {phone}) " +
				"RETURN n.password as password, n.id as id, HEAD(n.profiles) as profile, duplicates > 0 as hasDuplicate ";
		Map<String, Object> params = new HashMap<>();
		params.put("login", login);
		params.put("activationCode", activationCode);
		params.put("password", BCrypt.hashpw(password, BCrypt.gensalt()));
		params.put("email", email);
		params.put("phone", phone);
		params.put("allowActivateDuplicate", allowActivateDuplicateProfiles);
		neo.send(query, params, new Handler<Message<JsonObject>>(){

			@Override
			public void handle(Message<JsonObject> res) {
				if ("ok".equals(res.body().getString("status"))
						&& res.body().getJsonObject("result").getJsonObject("0") != null) {
					if(res.body().getJsonObject("result").getJsonObject("0").getBoolean("hasDuplicate")){
						handler.handle(new Either.Left<String, String>("activation.error.duplicated"));
						return;
					}
					JsonObject jo = new JsonObject()
							.put("userId", res.body().getJsonObject("result").getJsonObject("0").getString("id"))
							.put("profile", res.body().getJsonObject("result").getJsonObject("0").getString("profile"))
							.put("request", new JsonObject()
									.put("headers", new JsonObject()
											.put("Accept-Language", I18n.acceptLanguage(request))
											.put("Host", Renders.getHost(request))
									)
							);
					if (isNotEmpty(theme)) {
						jo.put("theme", theme);
					}
					Server.getEventBus(vertx).publish("activation.ack", jo);
					handler.handle(new Either.Right<String, String>(
							res.body().getJsonObject("result").getJsonObject("0").getString("id")));
				} else {
					String q =
							"MATCH (n:User) " +
							"WHERE n." + loginFieldName + "={login} AND n.activationCode IS NULL " +
							"AND NOT(n.password IS NULL) " +
							"RETURN n.password as password, n.id as id";
					Map<String, Object> p = new HashMap<>();
					p.put("login", login);
					neo.send(q, p, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							if ("ok".equals(event.body().getString("status")) &&
									event.body().getJsonObject("result").getJsonObject("0") != null &&
									BCrypt.checkpw(password, event.body().getJsonObject("result").getJsonObject("0")
											.getString("password", ""))) {
								handler.handle(new Either.Right<String, String>(
										event.body().getJsonObject("result").getJsonObject("0").getString("id")));
							} else {
								handler.handle(new Either.Left<String, String>("activation.error"));
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
		matchActivationCode("login", login, potentialActivationCode, handler);
	}

	@Override
	public void matchActivationCodeByLoginAlias(final String login, String potentialActivationCode,
			final Handler<Boolean> handler) {
		matchActivationCode("loginAlias", login, potentialActivationCode, handler);
	}

	private void matchActivationCode(final String loginFieldName, final String login, String potentialActivationCode,
		 final Handler<Boolean> handler) {
		String query =
				"MATCH (n:User) " +
				"WHERE n." + loginFieldName + "={login} AND n.activationCode = {activationCode} AND n.password IS NULL " +
				"AND (NOT EXISTS(n.blocked) OR n.blocked = false) " +
				"RETURN true as exists";

		JsonObject params = new JsonObject()
			.put("login", login)
			.put("activationCode", potentialActivationCode);
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
		matchResetCode("login", login, potentialResetCode, handler);
	}

	@Override
	public void matchResetCodeByLoginAlias(final String login, String potentialResetCode,
		   final Handler<Boolean> handler) {
		matchResetCode("loginAlias", login, potentialResetCode, handler);
	}

	private void matchResetCode(final String loginFieldName, final String login, String potentialResetCode,
		final Handler<Boolean> handler) {
		String query =
				"MATCH (n:User) " +
				"WHERE n." + loginFieldName + "={login} AND n.resetCode = {resetCode} " +
				"RETURN true as exists";

		JsonObject params = new JsonObject()
			.put("login", login)
			.put("resetCode", potentialResetCode);
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
		JsonObject params = new JsonObject().put("mail", email);

		neo.execute(query, params, Neo4jResult.validUniqueResultHandler(handler));
	}

	@Override
	public void findByMailAndFirstNameAndStructure(final String email, String firstName, String structure, final Handler<Either<String,JsonArray>> handler) {
		boolean setFirstname = firstName != null && !firstName.trim().isEmpty();
		boolean setStructure = structure != null && !structure.trim().isEmpty();

		String query = "MATCH (u:User)-[:IN]->(sg:Group)-[:DEPENDS]->(s:Structure) WHERE u.email = {mail} " +
				(setFirstname ? " AND u.firstName =~ {firstName}" : "") +
				(setStructure ? " AND s.id = {structure}" : "") +
				" AND u.activationCode IS NULL RETURN DISTINCT u.login as login, u.mobile as mobile, s.name as structureName, s.id as structureId";
		JsonObject params = new JsonObject().put("mail", email);
		if(setFirstname)
			params.put("firstName", "(?i)"+firstName);
		if(setStructure)
			params.put("structure", structure);
		neo.execute(query, params, Neo4jResult.validResultHandler(handler));
	}

	@Override
	public void findByLogin(final String login, final String resetCode,boolean checkFederatedLogin, final Handler<Either<String,JsonObject>> handler) {
		boolean setResetCode = resetCode != null && !resetCode.trim().isEmpty();

		final String baseQuery =
				"WHERE n.activationCode IS NULL " +
				(checkFederatedLogin ? "AND (NOT(HAS(n.federated)) OR n.federated = false) " : "") +
				(setResetCode ? "SET n.resetCode = {resetCode}, n.resetDate = {today} " : "") +
				"RETURN n.email as email, n.mobile as mobile";
		final String basicQuery = "MATCH (n:User {login:{login}}) " + baseQuery;

		final JsonObject params = new JsonObject().put("login", login);
		if(setResetCode)
			params.put("resetCode", resetCode)
				.put("today", new Date().getTime());

		neo.execute(basicQuery, params, Neo4jResult.validUniqueResultHandler(result -> {
			if(result.isLeft()){
				handler.handle(result);
			} else if (result.right().getValue().size() == 0) {
				final String basicAliasQuery = "MATCH (n:User {loginAlias:{login}}) " + baseQuery;
				neo.execute(basicAliasQuery, params, Neo4jResult.validUniqueResultHandler(result2 -> {
					if(result2.isLeft()){
						handler.handle(result2);
					} else {
						teacherForgotPassword(handler, setResetCode, params, result2, "Alias");
					}
				}));
			} else {
				teacherForgotPassword(handler, setResetCode, params, result, "");
			}
		}));
	}

	private void teacherForgotPassword(Handler<Either<String, JsonObject>> handler, boolean setResetCode, JsonObject params, Either<String, JsonObject> result, String alias) {
		final String mail = result.right().getValue().getString("email");
		final String mobile = result.right().getValue().getString("mobile");

		if(mail != null && config.getBoolean("teacherForgotPasswordEmail", false)){
			final String teacherQuery =
					"MATCH (n:User {login" + alias + ":{login}})-[:IN]->(sg:ProfileGroup)-[:DEPENDS]->(m:Class)" +
					"<-[:DEPENDS]-(tg:ProfileGroup)<-[:IN]-(p:User), " +
					"sg-[:DEPENDS]->(psg:ProfileGroup)-[:HAS_PROFILE]->(sp:Profile {name:'Student'}), " +
					"tg-[:DEPENDS]->(ptg:ProfileGroup)-[:HAS_PROFILE]->(tp:Profile {name:'Teacher'}) " +
					"WHERE NOT(p.email IS NULL) AND n.activationCode IS NULL AND " +
					"(NOT(HAS(n.federated)) OR n.federated = false) " +
					(setResetCode ? "SET n.resetCode = {resetCode}, n.resetDate = {today} " : "") +
					"RETURN p.email as email";
			neo.execute(teacherQuery, params, Neo4jResult.validUniqueResultHandler(new Handler<Either<String,JsonObject>>() {
				public void handle(Either<String, JsonObject> resultTeacher) {
					if(resultTeacher.isLeft()){
						handler.handle(resultTeacher);
						return;
					}

					resultTeacher.right().getValue().put("mobile", mobile);
					handler.handle(resultTeacher);
				}
			}));
		} else {
			handler.handle(result);
		}
	}

	@Override
	public void sendResetPasswordMail(HttpServerRequest request, String email, String resetCode,
			final Handler<Either<String, JsonObject>> handler) {
		if (email == null || resetCode == null || email.trim().isEmpty() || resetCode.trim().isEmpty()) {
			handler.handle(new Either.Left<String, JsonObject>("invalid.mail"));
			return;
		}
		JsonObject json = new JsonObject()
				.put("host", notification.getHost(request))
				.put("resetUri", notification.getHost(request) + "/auth/reset/" + resetCode);
		notification.sendEmail(
				request,
				email,
				config.getString("email", "noreply@one1d.fr"),
				null,
				null,
				"mail.reset.pw.subject",
				"email/forgotPassword.html",
				json,
				true,
				handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
					public void handle(Message<JsonObject> event) {
						if("error".equals(event.body().getString("status"))){
							handler.handle(new Either.Left<String, JsonObject>(event.body().getString("message", "")));
						} else {
							handler.handle(new Either.Right<String, JsonObject>(event.body()));
						}
					}
				}));
	}

	@Override
	public void sendForgottenIdMail(HttpServerRequest request, String login, String email, final Handler<Either<String, JsonObject>> handler){
		if (email == null || email.trim().isEmpty()) {
			handler.handle(new Either.Left<String, JsonObject>("invalid.mail"));
			return;
		}

		JsonObject json = new JsonObject()
			.put("login", login)
			.put("host", notification.getHost(request));
		notification.sendEmail(
				request,
				email,
				config.getString("email", "noreply@one1d.fr"),
				null,
				null,
				"mail.reset.id.subject",
				"email/forgotId.html",
				json,
				true,
				handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
					public void handle(Message<JsonObject> event) {
						if("error".equals(event.body().getString("status"))){
							handler.handle(new Either.Left<String, JsonObject>(event.body().getString("message", "")));
						} else {
							handler.handle(new Either.Right<String, JsonObject>(event.body()));
						}
					}
				}));
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
						.put("provider", smsProvider)
		    			.put("action", "send-sms")
		    			.put("parameters", new JsonObject()
		    				.put("receivers", new fr.wseduc.webutils.collections.JsonArray().add(formattedPhone))
		    				.put("message", body)
		    				.put("senderForResponse", true)
		    				.put("noStopClause", true));

					vertx.eventBus().send(smsAddress, smsObject, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
						public void handle(Message<JsonObject> event) {
							if("error".equals(event.body().getString("status"))){
								handler.handle(new Either.Left<String, JsonObject>(event.body().getString("message", "")));
							} else {
								handler.handle(new Either.Right<String, JsonObject>(event.body()));
							}
						}
					}));
				} else {
					handler.handle(new Either.Left<String, JsonObject>("template.error"));
				}
			}
		});
	}

	@Override
	public void sendResetPasswordSms(HttpServerRequest request, String phone, String resetCode, final Handler<Either<String, JsonObject>> handler){
		JsonObject params = new JsonObject()
			.put("resetCode", resetCode)
			.put("resetUri", resetCode);

		sendSms(request, phone, "phone/forgotPassword.txt", params, handler);
	}

	@Override
	public void sendForgottenIdSms(HttpServerRequest request, String login, String phone, final Handler<Either<String, JsonObject>> handler){
		JsonObject params = new JsonObject()
			.put("login", login);

		sendSms(request, phone, "phone/forgotId.txt", params, handler);
	}

	@Override
	public void resetPassword(String login, String resetCode, String password, final Handler<Boolean> handler) {
		final long codeDelay = config.getInteger("resetCodeDelay", 0);

		String query =
				"MATCH (n:User) " +
				"WHERE n.login={login} AND n.resetCode = {resetCode} " +
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
				"WHERE n.login={login} AND NOT(n.password IS NULL) " +
				"SET n.password = {password} " +
				"RETURN n.password as pw";
		Map<String, Object> params = new HashMap<>();
		params.put("login", login);
		updatePassword(handler, query, password, params);
	}

	@Override
	public void sendResetCode(final HttpServerRequest request, final String login, final SendPasswordDestination dest,boolean checkFederatedLogin ,
			final Handler<Boolean> handler) {
		String query =
				"MATCH (n:User) " +
				"WHERE n.login={login} AND n.activationCode IS NULL " +
				(checkFederatedLogin ? "AND (NOT(HAS(n.federated)) OR n.federated = false) " : "") +
				"SET n.resetCode = {resetCode}, n.resetDate = {today} " +
				"RETURN count(n) as nb";
		final String code = StringValidation.generateRandomCode(8);
		JsonObject params = new JsonObject().put("login", login).put("resetCode", code).put("today", new Date().getTime());
		neo.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status")) &&
						event.body().getJsonArray("result") != null && event.body().getJsonArray("result").size() == 1 &&
						1 == ((JsonObject) event.body().getJsonArray("result").getJsonObject(0)).getInteger("nb")) {
					if ("email".equals(dest.getType())) {
						sendResetPasswordMail(request, dest.getValue(), code, new Handler<Either<String, JsonObject>>() {
							public void handle(Either<String, JsonObject> event) {
								handler.handle(event.isRight());
							}
						});
					} else if ("mobile".equals(dest.getType())) {
						sendResetPasswordSms(request, dest.getValue(), code, new Handler<Either<String, JsonObject>>() {
							public void handle(Either<String, JsonObject> event) {
								handler.handle(event.isRight());
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

	@Override
	public void blockUser(String id, boolean block, final Handler<Boolean> handler) {
		String query = "MATCH (n:`User` { id : {id}}) SET n.blocked = {block} return count(*) = 1 as exists";
		JsonObject params = new JsonObject().put("id", id).put("block", block);
		neo.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				handler.handle("ok".equals(r.body().getString("status")) &&
						r.body().getJsonArray("result") != null && r.body().getJsonArray("result").getValue(0) != null &&
						(r.body().getJsonArray("result").getJsonObject(0)).getBoolean("exists", false));
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
			.put("id", id)
			.put("domain", domain)
			.put("scheme", scheme)
			.put("now", DateTime.now().toString());
		neo.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				handler.handle("ok".equals(r.body().getString("status")) &&
						r.body().getJsonArray("result") != null && r.body().getJsonArray("result").getValue(0) != null &&
						(r.body().getJsonArray("result").getJsonObject(0)).getBoolean("exists", false));
			}
		});
	};

	private void updatePassword(final Handler<Boolean> handler, String query, String password, Map<String, Object> params) {
		final String pw = BCrypt.hashpw(password, BCrypt.gensalt());
		params.put("password", pw);
		neo.send(query, params, new Handler<Message<JsonObject>>(){

			@Override
			public void handle(Message<JsonObject> res) {
				JsonObject r = res.body().getJsonObject("result");
				handler.handle("ok".equals(res.body().getString("status"))
						&& r.getJsonObject("0") != null
						&& pw.equals(r.getJsonObject("0").getString("pw")));
			}
		});
	}

}
