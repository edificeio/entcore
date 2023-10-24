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

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.eventbus.ResultMessage;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.security.BCrypt;
import fr.wseduc.webutils.security.NTLM;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import org.apache.commons.collections4.CollectionUtils;
import org.entcore.auth.pojo.SendPasswordDestination;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.events.EventStore;
import org.entcore.common.http.renders.TemplatedEmailRenders;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.notification.NotificationUtils;
import org.entcore.common.sms.SmsSender;
import org.entcore.common.sms.SmsSenderFactory;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.StringUtils;
import org.entcore.common.validation.StringValidation;
import org.joda.time.DateTime;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static fr.wseduc.webutils.Utils.*;
import static java.util.Collections.emptyList;
import static org.entcore.common.user.SessionAttributes.NEED_REVALIDATE_TERMS;

public class DefaultUserAuthAccount extends TemplatedEmailRenders implements UserAuthAccount {

	private static final Logger log = LoggerFactory.getLogger(DefaultUserAuthAccount.class);
	private static final long SEND_EMAIL_ACK_DELAY = 10000L;

	private final Neo neo;
	private final Vertx vertx;
	private final JsonObject config;
	private final EmailSender notification;
	private final Renders render;
	private final EventBus eb;
	private final SmsSender smsSender;

	private String smsProvider;
	private final String smsAddress;
	private final JsonArray allowActivateDuplicateProfiles;
	private final EventStore eventStore;
	private final boolean storePasswordEventEnabled;
	private final long resetCodeExpireDelay;

	private final boolean ignoreSendResetPasswordMailError;
	private final boolean ignoreSendResetPasswordSmsError;
	private final int passwordHistoryLength;

	private final boolean sendForgotPasswordEmailWithResetCode;

	public DefaultUserAuthAccount(Vertx vertx, JsonObject config, EventStore eventStore) {
		super(vertx, config);
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
		this.ignoreSendResetPasswordMailError = config.getBoolean("reset-pwd-mail-ignore-error", false);
		this.ignoreSendResetPasswordSmsError = config.getBoolean("reset-pwd-sms-ignore-error", false);
		
		SmsSenderFactory.getInstance().init(vertx, config);
		this.smsSender = SmsSenderFactory.getInstance().newInstance(eventStore);
		this.passwordHistoryLength = config.getInteger("password-history-length", 10);
		this.sendForgotPasswordEmailWithResetCode = config.getBoolean("send-forgot-password-email-with-reset-code", false);
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
				"SET n.password = {password}, n.oldPasswords = {oldPasswords}, n.activationCode = null, n.email = {email}, n.emailSearchField=LOWER({email}), n.mobile = {phone}, n.needRevalidateTerms = {needRevalidateTerms})  " +
				"RETURN n.password as password, n.id as id, HEAD(n.profiles) as profile, duplicates > 0 as hasDuplicate, " +
				"n.login as login, n.loginAlias as loginAlias ";
		final String encryptedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
		Map<String, Object> params = new HashMap<>();
		params.put("login", login);
		params.put("activationCode", activationCode);
		params.put("password", encryptedPassword);
		params.put("oldPasswords", new JsonArray().add(encryptedPassword));
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
					vertx.eventBus().request("entcore.feeder", jo.put("action", "check-duplicates"), handlerToAsyncHandler(new Handler<Message<JsonObject>>()
					{
						@Override
						public void handle(Message<JsonObject> message)
						{
							if("ok".equals(message.body().getString("status")) == false)
								log.error("Failed to check duplicates for activated user " + jo.getString("userId"));
						}
					}));

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
		validationFlag(false, userId, handler);
	}

	@Override
	public void needToValidateCgu(String userId, Handler<Boolean> handler) {
		validationFlag(true, userId, handler);
	}
	
	@Override
	public void matchActivationCode(final String login, String potentialActivationCode,
			final Handler<Either<String, JsonObject>> handler) {
		matchActivationCode("login", login, potentialActivationCode, handler);
	}

	@Override
	public void matchActivationCodeByLoginAlias(final String login, String potentialActivationCode,
			final Handler<Either<String, JsonObject>> handler) {
		matchActivationCode("loginAlias", login, potentialActivationCode, handler);
	}

	private void matchActivationCode(final String loginFieldName, final String login, String potentialActivationCode,
		 final Handler<Either<String, JsonObject>> handler) {
		String query =
				"MATCH (n:User) " +
				"WHERE n." + loginFieldName + "={login} AND n.activationCode = {activationCode} AND n.password IS NULL " +
				"AND (NOT EXISTS(n.blocked) OR n.blocked = false) " +
				"RETURN true as exists, n.displayName as displayName, n.email as email, n.mobile as mobile";

		JsonObject params = new JsonObject()
			.put("login", login)
			.put("activationCode", potentialActivationCode);
		neo.execute(query, params, Neo4jResult.validUniqueResultHandler( event -> {
			if(event.isLeft() || !event.right().getValue().getBoolean("exists", false))
				handler.handle(new Either.Left<String, JsonObject>("not.found"));
			else
				handler.handle(event);
		}));
	}

	@Override
	public void matchResetCode(final String login, String potentialResetCode,
			final Handler<Either<String, JsonObject>> handler) {
		matchResetCode("login", login, potentialResetCode, handler);
	}

	@Override
	public void matchResetCodeByLoginAlias(final String login, String potentialResetCode,
		   final Handler<Either<String, JsonObject>> handler) {
		matchResetCode("loginAlias", login, potentialResetCode, handler);
	}

	private void matchResetCode(final String loginFieldName, final String login, String potentialResetCode,
		final Handler<Either<String, JsonObject>> handler) {
		String query =
				"MATCH (n:User) " +
				"WHERE n." + loginFieldName + "={login} AND has(n.resetDate) " +
				"AND n.resetDate > {nowMinusDelay} AND n.resetCode = {resetCode} " +
				"RETURN true as exists, n.displayName as displayName, n.email as email, n.mobile as mobile";

		JsonObject params = new JsonObject()
			.put("login", login)
			.put("resetCode", potentialResetCode)
			.put("nowMinusDelay", (System.currentTimeMillis() - resetCodeExpireDelay));
			neo.execute(query, params, Neo4jResult.validUniqueResultHandler( event -> {
				if(event.isLeft() || !event.right().getValue().getBoolean("exists", false))
					handler.handle(new Either.Left<String, JsonObject>("not.found"));
				else
					handler.handle(event);
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

		String query = "MATCH (u:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure) WHERE u.emailSearchField = {mail} " +
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

	private void sendPasswordModificationMail(final HttpServerRequest request, String email, String firstName,
										 String login, String profile, JsonArray functions, final boolean reset) {
		if (StringUtils.isEmpty(email)) {
			log.error("Fail to send password change email, user email is undefined");
			return;
		}

		JsonObject sendMailFilter = config.getJsonObject("change-password-mail-filter");
		if (sendMailFilter != null) {
			boolean userAllowed = false;
			JsonArray allowedProfiles = sendMailFilter.getJsonArray("profiles");
			JsonArray allowedFunctions = sendMailFilter.getJsonArray("functions");

			if (allowedProfiles != null)
				if (allowedProfiles.contains(profile))
					userAllowed = true;

			if (allowedFunctions != null) {
				if (functions == null)
					functions = new JsonArray();

				boolean hasAllowedFunction = false;
				for (int i = allowedFunctions.size(); i-- > 0; ) {
					for (int j = functions.size(); j-- > 0; ) {
						if (functions.getString(j).equals(allowedFunctions.getString(i))) {
							hasAllowedFunction = true;
							break;
						}
					}
					if (hasAllowedFunction)
						break;
				}
				userAllowed |= hasAllowedFunction;
			}

			if (!userAllowed) {
				return;
			}
		}

		log.info("Sending changedPassword by email: " + login + "/" + email);

		final AtomicBoolean sendEmailAck = new AtomicBoolean(false);
		if (Boolean.TRUE.equals(config.getBoolean("log-send-email-ack", false))) {
			vertx.setTimer(SEND_EMAIL_ACK_DELAY, res -> {
				if (!sendEmailAck.get()) {
					log.error("No ack after 10s of sending changedPassword by email: " + login + "/" + email);
				}
			});
		}

		JsonObject templateParams = new JsonObject()
				.put("displayName", firstName)
				.put("date", LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) * 1000);

		final String emailTemplate = reset ? "email/resetPassword.html" : "email/changedPassword.html";
		final String i18nKey = reset ? "email.password.reset.subject" : "email.password.change.subject";

		processEmailTemplate(request, templateParams, emailTemplate, false, processedTemplate -> {
			final String emailSubject = getProjectNameFromTimelineI18n(request)
					+ I18n.getInstance().translate(i18nKey, getHost(request), I18n.acceptLanguage(request));

			notification.sendEmail(
					request,
					email,
					null,
					null,
					emailSubject,
					processedTemplate,
					null,
					false,
					ar -> {
						sendEmailAck.set(true);
						if (ar.succeeded()) {
							if (log.isDebugEnabled()) {
								log.debug("Success sending changedPassword by email: " + login + "/" + email);
							}
						} else {
							log.error("Error sending changedPassword by email: " + login + "/" + email, ar.cause());
						}
					}
			);
		});
	}

	@Override
	public void sendResetPasswordMail(HttpServerRequest request, String email, String resetCode, String displayName,
									  String login, final Handler<Either<String, JsonObject>> handler) {
		if (email == null || resetCode == null || email.trim().isEmpty() || resetCode.trim().isEmpty()) {
			handler.handle(new Either.Right<String, JsonObject>(new JsonObject()));
			return;
		}
		log.info("Sending resetCode by email: "+login+"/"+email+"/"+resetCode);
		JsonObject json = new JsonObject()
				.put("host", notification.getHost(request))
				.put("resetUri", notification.getHost(request) + "/auth/reset/" + resetCode)
				.put("resetCode", resetCode)
				.put("displayName", displayName);



		notification.sendEmail(
				request,
				email,
				config.getString("email", "noreply@one1d.fr"),
				null,
				null,
				"mail.reset.pw.subject",
				sendForgotPasswordEmailWithResetCode ? "email/forgotPasswordResetCode.html" : "email/forgotPassword.html",
				json,
				true,
				handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
					public void handle(Message<JsonObject> event) {
						if(!ignoreSendResetPasswordMailError) {
							if ("error".equals(event.body().getString("status"))) {
								handler.handle(new Either.Left<String, JsonObject>(event.body().getString("message", "")));
							} else {
								handler.handle(new Either.Right<String, JsonObject>(event.body()));
							}
						}
					}
				}));
		if(ignoreSendResetPasswordMailError) {
			handler.handle(new Either.Right<>(new ResultMessage().body()));
		}
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

	private void sendSms(HttpServerRequest request, final String phone, String template, JsonObject params,
											 final String module, final Handler<Either<String, JsonObject>> handler){
		if (phone == null || phone.trim().isEmpty()) {
			handler.handle(new Either.Right<String, JsonObject>(new JsonObject()));
			return;
		}
		smsSender.send(request, phone, template, params, module)
		.onSuccess(e -> {
			final boolean succeeded = e.getValidReceivers() != null && e.getValidReceivers().length == 1;
			if(!ignoreSendResetPasswordSmsError) {
				if(succeeded) {
					handler.handle(new Either.Right<>(new JsonObject().put("success", true)));
				} else {
					handler.handle(new Either.Left<>("ko"));
				}
			} else {
				handler.handle(new Either.Right<>(new ResultMessage().body()));
			}
		})
		.onFailure(th -> {
			if(!ignoreSendResetPasswordSmsError) {
				handler.handle(new Either.Left<>(th.getMessage()));
			} else {
				handler.handle(new Either.Right<>(new ResultMessage().body()));
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

		sendSms(request, phone, "phone/forgotPassword.txt", params, "RESETPWD", handler);
	}

	@Override
	public void sendForgottenIdSms(HttpServerRequest request, String login, String phone, final Handler<Either<String, JsonObject>> handler){
		JsonObject params = new JsonObject()
			.put("login", login);
		log.info("Sending forgotId by sms: "+login+"/"+phone);

		sendSms(request, phone, "phone/forgotId.txt", params, "FORGOTID", handler);
	}

	@Override
	public void resetPassword(String login, String resetCode, String password, HttpServerRequest request, final Handler<String> handler) {
			String query =
				"MATCH (n:User) " +
					"WHERE n.login={login} AND has(n.resetDate) " +
					"AND n.resetDate > {nowMinusDelay} AND n.resetCode = {resetCode} " +
					"OPTIONAL MATCH (n)-[:IN]->(f:FunctionGroup) " +
					"OPTIONAL MATCH (n)-[:HAS_FUNCTION]->(func:Function) " +
					"SET n.password = {password}, n.resetCode = null, n.resetDate = null, n.changePw = null," +
					"    n.oldPasswords = {oldPasswords} " +
					"RETURN n.password as pw, head(n.profiles) as profile, n.id as id, " +
					"COLLECT(func.name) + COLLECT(f.filter) as functions, " +
					"n.login as login, n.loginAlias as loginAlias, n.email AS email, n.firstName AS firstName";
			Map<String, Object> params = new HashMap<>();
			params.put("login", login);
			params.put("resetCode", resetCode);
			params.put("nowMinusDelay", (System.currentTimeMillis() - resetCodeExpireDelay));
			updatePassword(user -> {
        if(request != null && user != null)
        {
          String email = user.getString("email");
          String firstName = user.getString("firstName");
          String userLogin = user.getString("login");
          String profile = user.getString("profile");
          JsonArray functions = user.getJsonArray("functions");
		  sendPasswordModificationMail(request, email, firstName, userLogin, profile, functions, true);
          handler.handle(user.getString("id")); // Ignore email failures: email is optional
        }
        else
          handler.handle(user != null ? user.getString("id") : null);
      }, query, password, login, params);
	}

	/**
	 * Get the user's list of old passwords.
	 * @param login Login of the user
	 * @param fieldName Name of the field used to match the user (login or loginAlias)
	 * @return the user's list of old passwords.
	 */
	private Future<List<String>> getOldPasswords(final String login, final String fieldName) {
		final Promise<List<String>> promise = Promise.promise();
		if(passwordHistoryLength >= 1) {
			final String query = "MATCH (n:User) WHERE n." + fieldName + " = {login} " +
				" RETURN n.id as id, n.oldPasswords as oldPasswords";
			final Map<String, Object> params = new HashMap<>();
			params.put("login", login);
			neo.send(query, params, event -> {
				final JsonObject body = event.body();
				if ("ok".equals(body.getString("status"))) {
					final JsonObject result = body.getJsonObject("result");
					if(result != null && result.getJsonObject("0") != null && isNotEmpty(result.getJsonObject("0").getString("id"))) {
						final List<String> oldPasswords;
						final JsonObject element = result.getJsonObject("0");
						if (element == null || element.getValue("oldPasswords") == null) {
							oldPasswords = emptyList();
						} else {
							try {
								oldPasswords = element.getJsonArray("oldPasswords").getList();
							} catch (Exception e) {
								promise.fail(e);
								return;
							}
						}
						promise.complete(oldPasswords);
					} else if("login".equals(fieldName)){
						getOldPasswords(login, "loginAlias").onComplete(promise);
					} else {
						promise.complete(emptyList());
					}
				} else {
					promise.fail(body.getString("message"));
				}
			});
		} else {
			promise.complete(emptyList());
		}
		return promise.future();
	}

	@Override
	public void changePassword(String login, String password, HttpServerRequest request, final Handler<String> handler) {
		String query =
				"MATCH (n:User) " +
				"WHERE n.login={login} AND NOT(n.password IS NULL) " +
				"OPTIONAL MATCH (n)-[:IN]->(f:FunctionGroup) " +
				"OPTIONAL MATCH (n)-[:HAS_FUNCTION]->(func:Function) " +
				"SET n.password = {password}, n.changePw = null, n.oldPasswords = {oldPasswords} " +
				"RETURN n.password as pw, head(n.profiles) as profile, n.id as id, " +
				"COLLECT(func.name) + COLLECT(f.filter) as functions, " +
				"n.login as login, n.loginAlias as loginAlias, n.email AS email, n.firstName as firstName";
		Map<String, Object> params = new HashMap<>();
		params.put("login", login);
		updatePassword(user -> {
			if (request != null && user != null) {
				String email = user.getString("email");
				String firstName = user.getString("firstName");
				String login1 = user.getString("login");
				String profile = user.getString("profile");
				JsonArray functions = user.getJsonArray("functions");

				sendPasswordModificationMail(request, email, firstName, login1, profile, functions, false);
				handler.handle(user.getString("id")); // Ignore email failures: email is optional
			} else
				handler.handle(user != null ? user.getString("id") : null);
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
				NotificationUtils.deleteFcmTokens(new JsonArray().add(id), ar -> {
					if (ar.isLeft()) {
						log.error("Failed to delete FCM tokens when block user : " + id, ar.left().getValue());
					}
				});
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
				NotificationUtils.deleteFcmTokens(ids, ar -> {
					if (ar.isLeft()) {
						log.error("Failed to delete FCM tokens when block users.", ar.left().getValue());
					}
				});
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
		getOldPasswords(login, "login")
			.onFailure(th -> handler.handle(null))
			.onSuccess(oldPasswords -> {
				final boolean newPasswordNeverUsed = CollectionUtils.isEmpty(oldPasswords) ||
					oldPasswords.stream()
						.limit(passwordHistoryLength)
						.noneMatch(oldPdw -> BCrypt.checkpw(password, oldPdw));
				if(newPasswordNeverUsed) {
					final String pw = BCrypt.hashpw(password, BCrypt.gensalt());
					params.put("password", pw);
					params.put("oldPasswords", getUpdatedListOfPasswords(pw, oldPasswords));
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
				} else {
					log.warn("User " + login + " tried to reset their password by using an old password");
					handler.handle(null);
				}
			});
	}

	/**
	 * @param encryptedPassword Newly set encrypted password
	 * @param oldPasswords Old encrypted passwords of the user in antichronological order
	 * @return List of passwords in antichronological order including the new password and truncated to passwordHistoryLength
	 */
	private JsonArray getUpdatedListOfPasswords(final String encryptedPassword, final List<String> oldPasswords) {
		final JsonArray updatedList = new JsonArray();
		updatedList.add(encryptedPassword);
		if(CollectionUtils.isNotEmpty(oldPasswords)) {
			for (int i = 0; i < oldPasswords.size() && updatedList.size() < passwordHistoryLength; i++) {
				updatedList.add(oldPasswords.get(i));
			}
		}
		return updatedList;
	}


	/**
	 * @param  userId
	 * 			handler,
	 * 				Handler<Either<String, JsonObject>> handler
	 * 				- String : error message
	 * 				- JsonObject : success message
	 * Using this method will not force the user to change his password, userId it's use to match user and force changPw to true
	 * if user is found and force change password is successful, the handler will be called with a right value
	 * 
	 */
	@Override
	public void forceChangePassword(String userId, Handler<Either<String, JsonObject>> handler) {
		String query = "MATCH (u:User) WHERE u.id = {userId} SET u.changePw = true return count(*) = 1 as exists;";
		JsonObject params = new JsonObject().put("userId", userId);

		neo.execute(query, params, res -> {
			if ("ok".equals(res.body().getString("status"))) {
				JsonArray result = res.body().getJsonArray("result");
				if(result != null && result.size() == 1) {
					JsonObject result_data = result.getJsonObject(0);
					if(result_data != null) {
						handler.handle(new Either.Right<>(new JsonObject()));
						return;
					}
				}
			}
			handler.handle(new Either.Left<>("Failed to force change password"));
		});
	}
	private void validationFlag(Boolean flag,String userId, Handler<Boolean> handler) {
		String query = "MATCH(u:User{id:{userId}}) SET u.needRevalidateTerms= "+ flag +" RETURN u";
		JsonObject params = new JsonObject().put("userId", userId);
		neo.execute(query, params, Neo4jResult.validUniqueResultHandler(res-> {
			if(res.isRight()) {
				UserUtils.addSessionAttribute(eb, userId, NEED_REVALIDATE_TERMS, flag.toString(), addSessionAttributeRes -> {
					handler.handle(addSessionAttributeRes);
				});
			} else {
				handler.handle(false);
			}
		}));
	}

}
