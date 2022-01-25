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

import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import fr.wseduc.webutils.Either;

import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.security.NTLM;

import org.entcore.auth.pojo.SendPasswordDestination;
import io.vertx.core.shareddata.LocalMap;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.events.EventStore;
import org.entcore.common.neo4j.Neo4j;
import org.joda.time.DateTime;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.entcore.common.neo4j.Neo;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.user.UserUtils;
import org.entcore.common.validation.StringValidation;

import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.security.BCrypt;

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.Utils.isNotEmpty;

public class DefaultUserAuthAccount implements UserAuthAccount {

	private static final Logger log = LoggerFactory.getLogger(DefaultUserAuthAccount.class);

	private final Neo neo;
	private final Vertx vertx;
	private final JsonObject config;
	private final EmailSender notification;
	private final Renders render;
	private final EventBus eb;

	private String smsProvider;
	private final String smsAddress;
	private final JsonArray allowActivateDuplicateProfiles;
	private final EventStore eventStore;
	private final boolean storePasswordEventEnabled;
	private final long resetCodeExpireDelay;

	public DefaultUserAuthAccount(Vertx vertx, JsonObject config, EventStore eventStore) {
		this.eb = Server.getEventBus(vertx);
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
		this.allowActivateDuplicateProfiles = getOrElse(config.getJsonArray("allow-activate-duplicate"),
				new JsonArray().add("Relative"));
		this.eventStore = eventStore;
		this.storePasswordEventEnabled = (config.getString("password-event-min-date") != null);
		this.resetCodeExpireDelay = getOrElse(config.getLong("reset-code-expire-delay"), 3600000l);
	}

	@Override
	public void activateAccountWithRevalidateTerms(final String login, String activationCode, final String password,
								String email, String phone, final String theme, final HttpServerRequest request, final Handler<Either<String, String>> handler) {
		activateAccount("login", login, activationCode, password, email, phone, theme, true, request, handler);
	}

	@Override
	public void activateAccount(final String login, String activationCode, final String password,
			String email, String phone, final String theme, final HttpServerRequest request, final Handler<Either<String, String>> handler) {
		activateAccount("login", login, activationCode, password, email, phone, theme, false, request, handler);
	}

	@Override
	public void activateAccountByLoginAlias(final String login, String activationCode, final String password,
		String email, String phone, final String theme, final HttpServerRequest request, final Handler<Either<String, String>> handler) {
		activateAccount("loginAlias", login, activationCode, password, email, phone, theme, false, request, handler);
	}

	private void activateAccount(final String loginFieldName, final String login, String activationCode, final String password,
	 	String email, String phone, final String theme, final Boolean needRevalidateTerms, final HttpServerRequest request, final Handler<Either<String, String>> handler) {
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
				"SET n.password = {password}, n.activationCode = null, n.email = {email}, n.emailSearchField=LOWER({email}), n.mobile = {phone}, n.needRevalidateTerms = {needRevalidateTerms})  " +
				"RETURN n.password as password, n.id as id, HEAD(n.profiles) as profile, duplicates > 0 as hasDuplicate, " +
				"n.login as login, n.loginAlias as loginAlias ";
		Map<String, Object> params = new HashMap<>();
		params.put("login", login);
		params.put("activationCode", activationCode);
		params.put("password", BCrypt.hashpw(password, BCrypt.gensalt()));
		params.put("email", email);
		params.put("phone", phone);
		params.put("allowActivateDuplicate", allowActivateDuplicateProfiles);
		params.put("needRevalidateTerms", needRevalidateTerms);
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
					storePasswordEvent(res.body().getJsonObject("result").getJsonObject("0").getString("login"),
							res.body().getJsonObject("result").getJsonObject("0").getString("loginAlias"),
							password,
							res.body().getJsonObject("result").getJsonObject("0").getString("id"),
							res.body().getJsonObject("result").getJsonObject("0").getString("profile"));
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

	private void storePasswordEvent(final String login, final String loginAlias, final String password, String userId, String profile) {
		if (storePasswordEventEnabled) {
			try {
				final JsonObject j = new JsonObject()
						.put("event_type", "PASSWORD")
						.put("login", login)
						.put("password", NTLM.ntHash(password));
				if (isNotEmpty(userId)) {
					j.put("user_id", userId);
				}
				if (isNotEmpty(profile)) {
					j.put("profile", profile);
				}
				if (isNotEmpty(loginAlias)) {
					j.put("login_alias", loginAlias);
				}
				eventStore.storeCustomEvent("auth", j);
			} catch (NoSuchAlgorithmException ex) {
				log.error("Error sending PASSWORD Event account", ex);
			}
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public void revalidateCgu(String userId, Handler<Boolean> handler) {
		String query = "MATCH(u:User{id:{userId}}) SET u.needRevalidateTerms=false RETURN u";
		JsonObject params = new JsonObject().put("userId", userId);
		neo.execute(query, params, Neo4jResult.validUniqueResultHandler(res-> {
			if(res.isRight()) {
				UserUtils.deleteCacheSession(eb, userId, false, session -> {
					handler.handle(session);
				});
			}else {
				handler.handle(false);
			}
		}));
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
				"WHERE n." + loginFieldName + "={login} AND has(n.resetDate) " +
				"AND n.resetDate > {nowMinusDelay} AND n.resetCode = {resetCode} " +
				"RETURN true as exists";

		JsonObject params = new JsonObject()
			.put("login", login)
			.put("resetCode", potentialResetCode)
			.put("nowMinusDelay", (System.currentTimeMillis() - resetCodeExpireDelay));
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

		String query = "MATCH (u:User)-[:IN]->(sg:Group)-[:DEPENDS]->(s:Structure) WHERE u.emailSearchField = {mail} " +
				(setFirstname ? " AND u.firstNameSearchField = {firstName}" : "") +
				(setStructure ? " AND s.id = {structure}" : "") +
				//" AND u.activationCode IS NULL RETURN DISTINCT u.login as login, u.mobile as mobile, s.name as structureName, s.id as structureId";
				" RETURN DISTINCT u.login as login, u.activationCode as activationCode, u.mobile as mobile, s.name as structureName, s.id as structureId";
		//Feat #20790 match only lowercases values
		JsonObject params = new JsonObject().put("mail", email.toLowerCase());
		if(setFirstname)
			params.put("firstName", StringValidation.sanitize(firstName));
		if(setStructure)
			params.put("structure", structure);
		neo.execute(query, params, Neo4jResult.validResultHandler(handler));
	}

	@Override
	public void findByLogin(final String login, final String resetCode,boolean checkFederatedLogin, final Handler<Either<String,JsonObject>> handler) {
		boolean setResetCode = resetCode != null && !resetCode.trim().isEmpty();

		final String baseQuery =
				//"WHERE n.activationCode IS NULL " +
				"WHERE 1=1 " +
				(checkFederatedLogin ? "AND (NOT(HAS(n.federated)) OR n.federated = false) " : "") +
				(setResetCode ? "SET n.resetCode = {resetCode}, n.resetDate = {today} " : "") +
				"RETURN n.email as email, n.mobile as mobile, n.displayName as displayName, n.activationCode as activationCode";
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
					//"WHERE NOT(p.email IS NULL) AND n.activationCode IS NULL AND " +
					"WHERE NOT(p.email IS NULL) AND " +
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
	public void sendChangedPasswordMail(HttpServerRequest request, String email, String displayName, String login, final Handler<Either<String, JsonObject>> handler)
	{
		if (email == null || email.trim().isEmpty())
		{
			handler.handle(new Either.Left<String, JsonObject>("invalid.mail"));
			return;
		}

		log.info("Sending changedPassword by email: "+login+"/"+email);
		JsonObject json = new JsonObject()
				.put("host", notification.getHost(request))
				.put("displayName", displayName);

		notification.sendEmail(
				request,
				email,
				config.getString("email", "noreply@one1d.fr"),
				null,
				null,
				"mail.change.pw.subject",
				"email/changedPassword.html",
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
	public void sendResetPasswordMail(HttpServerRequest request, String email, String resetCode, String displayName,
									  String login, final Handler<Either<String, JsonObject>> handler) {
		if (email == null || resetCode == null || email.trim().isEmpty() || resetCode.trim().isEmpty()) {
			handler.handle(new Either.Left<String, JsonObject>("invalid.mail"));
			return;
		}
		log.info("Sending resetCode by email: "+login+"/"+email+"/"+resetCode);
		JsonObject json = new JsonObject()
				.put("host", notification.getHost(request))
				.put("resetUri", notification.getHost(request) + "/auth/reset/" + resetCode)
				.put("displayName", displayName);



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
		log.info("Sending forgotId by mail: "+login+"/"+email);

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
	public void sendResetPasswordSms(HttpServerRequest request, String phone, String resetCode, String displayName,
									 String login, final Handler<Either<String, JsonObject>> handler)
	{
		log.info("Sending resetCode by sms: "+login+"/"+phone+"/"+resetCode);
		JsonObject params = new JsonObject()
			.put("resetCode", resetCode)
			.put("resetUri", resetCode)
			.put("displayName", displayName);

		sendSms(request, phone, "phone/forgotPassword.txt", params, handler);
	}

	@Override
	public void sendForgottenIdSms(HttpServerRequest request, String login, String phone, final Handler<Either<String, JsonObject>> handler){
		JsonObject params = new JsonObject()
			.put("login", login);
		log.info("Sending forgotId by sms: "+login+"/"+phone);

		sendSms(request, phone, "phone/forgotId.txt", params, handler);
	}

	@Override
	public void resetPassword(String login, String resetCode, String password, HttpServerRequest request, final Handler<Boolean> handler) {
		String query =
				"MATCH (n:User) " +
				"WHERE n.login={login} AND has(n.resetDate) " +
				"AND n.resetDate > {nowMinusDelay} AND n.resetCode = {resetCode} " +
				"SET n.password = {password}, n.resetCode = null, n.resetDate = null " +
				"RETURN n.password as pw, head(n.profiles) as profile, n.id as id, " +
				"n.login as login, n.loginAlias as loginAlias, n.email AS email, n.displayName AS displayName";
		Map<String, Object> params = new HashMap<>();
		params.put("login", login);
		params.put("resetCode", resetCode);
		params.put("nowMinusDelay", (System.currentTimeMillis() - resetCodeExpireDelay));
		updatePassword(new Handler<JsonObject>()
		{
			@Override
			public void handle(JsonObject user)
			{
				if(request != null && user != null)
				{
					String email = user.getString("email");
					String dName = user.getString("displayName");
					String login = user.getString("login");
					sendChangedPasswordMail(request, email, dName, login, new Handler<Either<String, JsonObject>>()
					{
						@Override
						public void handle(Either<String, JsonObject> res)
						{
							handler.handle(true); // Ignore email failures: email is optional
						}
					});
				}
				else
					handler.handle(user != null);
			}
		}, query, password, login, params);
	}

	@Override
	public void changePassword(String login, String password, HttpServerRequest request, final Handler<Boolean> handler) {
		String query =
				"MATCH (n:User) " +
				"WHERE n.login={login} AND NOT(n.password IS NULL) " +
				"SET n.password = {password}, n.changePw = null " +
				"RETURN n.password as pw, head(n.profiles) as profile, n.id as id, " +
				"n.login as login, n.loginAlias as loginAlias, n.email AS email, n.displayName as displayName";
		Map<String, Object> params = new HashMap<>();
		params.put("login", login);
		updatePassword(new Handler<JsonObject>()
		{
			@Override
			public void handle(JsonObject user)
			{
				if(request != null && user != null)
				{
					String email = user.getString("email");
					String dName = user.getString("displayName");
					String login = user.getString("login");
					sendChangedPasswordMail(request, email, dName, login, new Handler<Either<String, JsonObject>>()
					{
						@Override
						public void handle(Either<String, JsonObject> res)
						{
							handler.handle(true); // Ignore email failures: email is optional
						}
					});
				}
				else
					handler.handle(user != null);
			}
		}, query, password, login, params);
	}

	private void setResetCode(final String login, boolean checkFederatedLogin, final Handler<Either<String, JsonObject>> handler) {
		final String code = StringValidation.generateRandomCode(8);

		String query =
				"MATCH (n:User) " +
						"WHERE n.login={login} AND n.activationCode IS NULL " +
						(checkFederatedLogin ? "AND (NOT(HAS(n.federated)) OR n.federated = false) " : "") +
						"SET n.resetCode = {resetCode}, n.resetDate = {today} " +
						"RETURN count(n) as nb, n.displayName as displayName";
		JsonObject params = new JsonObject().put("login", login).put("resetCode", code).put("today", new Date().getTime());
		neo.execute(query, params, event -> {
			if ("ok".equals(event.body().getString("status")))
			{
				JsonArray result = event.body().getJsonArray("result");
				if(result != null && result.size() == 1)
				{
					JsonObject result_data = result.getJsonObject(0);
					if(result_data.getInteger("nb") == 1)
					{
						handler.handle(new Either.Right<>(
							new JsonObject()
								.put("code", code)
								.put("displayName", result_data.getString("displayName"))
						));
						return;
					}
				}
			}
			handler.handle(new Either.Left<>("failed to set reset code"));
		});
	}

	@Override
	public void sendResetCode(final HttpServerRequest request, final String login, final SendPasswordDestination dest,boolean checkFederatedLogin ,
			final Handler<Boolean> handler) {
		setResetCode(login, checkFederatedLogin, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> either) {
				if (either.isRight())
				{
					final JsonObject code_data = either.right().getValue();
					if ("email".equals(dest.getType()))
					{
						sendResetPasswordMail(request, dest.getValue(), code_data.getString("code"), code_data.getString("displayName"), login,
							new Handler<Either<String, JsonObject>>()
							{
								public void handle(Either<String, JsonObject> event)
								{
									handler.handle(event.isRight());
								}
							}
						);
					}
					else if ("mobile".equals(dest.getType()))
					{
						sendResetPasswordSms(request, dest.getValue(), code_data.getString("code"), code_data.getString("displayName"), login,
							new Handler<Either<String, JsonObject>>()
						{
							public void handle(Either<String, JsonObject> event)
							{
								handler.handle(event.isRight());
							}
						});
					}
					else
					{
						handler.handle(false);
					}
				} else {
					handler.handle(false);
				}
			}
		});
	}

	@Override
	public void generateResetCode(String login, boolean checkFederatedLogin, Handler<Either<String, JsonObject>> handler) {
		setResetCode(login, checkFederatedLogin, handler);
	}

	@Override
	public void massGenerateResetCode(JsonArray userIds, boolean checkFederatedLogin , Handler<Either<String, JsonObject>> handler) {

		final JsonObject resetCodes = new JsonObject();
		final Long today = new Date().getTime();
		final Map<String, String> map = new HashMap<>();

		for (int i = 0; i < userIds.size(); i++) {
			final String userId = userIds.getString(i);
			final String code = StringValidation.generateRandomCode(8);
			map.put(userId, code);
			resetCodes.put(userId, new JsonObject().put("code", code).put("date", today));
		}

		String query = "WITH {codes} AS data, [k in keys({codes})] AS userIds " +
				"MATCH (n:User) WHERE n.id IN userIds " +
				"SET n.resetCode = data[n.id], n.resetDate = {today}";
		JsonObject params = new JsonObject().put("codes", map).put("today", today);

		neo.execute(query, params, res -> {
			if ("ok".equals(res.body().getString("status"))) {
				handler.handle(new Either.Right<>(resetCodes));
			} else {
				handler.handle(new Either.Left<>(res.body().getString("message")));
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
				storeLockEvent(new JsonArray().add(id), block);
				handler.handle("ok".equals(r.body().getString("status")) &&
						r.body().getJsonArray("result") != null && r.body().getJsonArray("result").getValue(0) != null &&
						(r.body().getJsonArray("result").getJsonObject(0)).getBoolean("exists", false));
			}
		});
	}

	@Override
	public void blockUsers(JsonArray ids, boolean block, final Handler<Boolean> handler) {
		String query = "MATCH (n:`User`) WHERE n.id in {ids} SET n.blocked = {block} return count(n) = {size} as exists";
		JsonObject params = new JsonObject().put("ids", ids).put("block", block).put("size",ids.size());
		neo.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				storeLockEvent(ids, block);
				handler.handle("ok".equals(r.body().getString("status")) &&
						r.body().getJsonArray("result") != null && r.body().getJsonArray("result").getValue(0) != null &&
						(r.body().getJsonArray("result").getJsonObject(0)).getBoolean("exists", false));
			}
		});
	}

	@Override
	public void storeLockEvent(JsonArray ids, boolean block) {
		if (storePasswordEventEnabled && ids != null && !ids.isEmpty()) {
			final String query =
					"MATCH (u:User) WHERE u.id IN {ids} AND u.blocked = {block} " +
					"RETURN u.id as id, u.login as login, u.loginAlias as loginAlias, head(u.profiles) as profile ";
			final JsonObject params = new JsonObject().put("ids", ids).put("block", block);
			neo.execute(query, params, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> r) {
					final JsonArray res = r.body().getJsonArray("result");
					final String eventType = block ? "LOCKED" : "UNLOCKED";
					if ("ok".equals(r.body().getString("status")) && res != null) {
						for (Object o : res) {
							if (!(o instanceof JsonObject)) continue;
							final JsonObject item = (JsonObject) o;
							final JsonObject pEvent = new JsonObject()
							.put("event_type", eventType)
							.put("login", item.getString("login"))
							.put("user_id", item.getString("id"))
							.put("profile", item.getString("profile"));
							if (isNotEmpty(item.getString("loginAlias"))) {
								pEvent.put("login_alias", item.getString("loginAlias"));
							}
							eventStore.storeCustomEvent("auth", pEvent);
						}
					} else {
						log.error("Error sending " + eventType + " Event account : " + r.body().getString("message"));
					}
				}
			});
		}
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
	}

	@Override
	public void storeDomainByLogin(String login, String domain, String scheme, final Handler<Boolean> handler) {
		String querySet =
				"SET u.lastDomain = {domain}, u.lastScheme = {scheme}, u.lastLogin = {now} " +
				"return count(*) = 1 as exists";
		JsonObject params = new JsonObject()
			.put("login", login)
			.put("domain", domain)
			.put("scheme", scheme)
			.put("now", DateTime.now().toString());
		neo.execute("MATCH (u:User{login:{login}}) " + querySet, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				Boolean isOk = "ok".equals(r.body().getString("status")) &&
					r.body().getJsonArray("result") != null && r.body().getJsonArray("result").getValue(0) != null &&
					(r.body().getJsonArray("result").getJsonObject(0)).getBoolean("exists", false);

				if(isOk.booleanValue() == true)
					handler.handle(isOk);
				else
				{
					neo.execute("MATCH (u:User{loginAlias:{login}}) " + querySet, params, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> r2) {
							handler.handle("ok".equals(r2.body().getString("status")) &&
									r2.body().getJsonArray("result") != null && r2.body().getJsonArray("result").getValue(0) != null &&
									(r2.body().getJsonArray("result").getJsonObject(0)).getBoolean("exists", false));
						}
					});
				}
			}
		});
	}

	@Override
	public void generateOTP(String id, Handler<Either<String, JsonObject>> handler) {
		final String query =
				"MATCH (u:User {id:{id}}) " +
				"SET u.otp = {otp}, u.otpiat = {otpiat} ";
		final String otp = StringValidation.generateRandomCode(8);
		final long now = System.currentTimeMillis();
		final JsonObject params = new JsonObject()
				.put("id", id)
				.put("otp", BCrypt.hashpw(otp, BCrypt.gensalt()))
				.put("otpiat", now);
		Neo4j.getInstance().execute(query, params, res -> {
			if ("ok".equals(res.body().getString("status"))) {
				handler.handle(new Either.Right<>(new JsonObject().put("otp", otp).put("otpiat", now)));
			} else {
				handler.handle(new Either.Left<>(res.body().getString("message")));
			}
		});
	}

	private void updatePassword(final Handler<JsonObject> handler, String query, String password, String login, Map<String, Object> params) {
		final String pw = BCrypt.hashpw(password, BCrypt.gensalt());
		params.put("password", pw);
		neo.send(query, params, res -> {
			JsonObject r = res.body().getJsonObject("result");
			JsonObject user = r.getJsonObject("0");
			boolean updated = "ok".equals(res.body().getString("status"))
					&& user != null
					&& pw.equals(user.getString("pw"));
			if (updated) {
				storePasswordEvent(user.getString("login"), user.getString("loginAlias"),
						password, user.getString("id"), user.getString("profile"));
				handler.handle(user);
			} else {
				neo.send(query.replaceFirst("n.login=", "n.loginAlias="), params, event -> {
					JsonObject r2 = event.body().getJsonObject("result");
					JsonObject user2 = r2.getJsonObject("0");
					if ("ok".equals(event.body().getString("status"))
							&& user2 != null
							&& pw.equals(user2.getString("pw"))) {
						storePasswordEvent(user2.getString("login"), user2.getString("loginAlias"),
								password, user2.getString("id"), user2.getString("profile"));
						handler.handle(user2);
					} else {
						handler.handle(null);
					}
				});
			}
		});
	}

}
