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

package org.entcore.directory.services.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Utils;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.CookieHelper;

import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import static org.entcore.common.user.SessionAttributes.*;
import static org.entcore.common.emailstate.EmailState.FIELD_MUST_CHANGE_PWD;
import static org.entcore.common.emailstate.EmailState.FIELD_MUST_VALIDATE_TERMS;
import static org.entcore.common.emailstate.EmailState.FIELD_MUST_VALIDATE_EMAIL;
import static org.entcore.common.emailstate.EmailStateUtils.*;
import static org.entcore.common.neo4j.Neo4jResult.*;
import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.isNotEmpty;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.entcore.common.email.EmailFactory;
import org.entcore.common.emailstate.EmailStateUtils;
import org.entcore.common.utils.StringUtils;
import org.entcore.directory.services.MailValidationService;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.FileProps;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public class DefaultMailValidationService extends Renders implements MailValidationService {
	private final Neo4j neo = Neo4j.getInstance();
	private EmailSender emailSender = null;
	Map<String, JsonObject> requestThemeKV = null;

	public DefaultMailValidationService(io.vertx.core.Vertx vertx, io.vertx.core.json.JsonObject config) {
		super(vertx, config);
		emailSender = new EmailFactory(this.vertx, config).getSenderWithPriority(EmailFactory.PRIORITY_HIGH);
	}

	@Override
	public Future<JsonObject> getMandatoryUserValidation(String userId) {
        Promise<JsonObject> promise = Promise.promise();
        UserUtils.getSessionByUserId(vertx.eventBus(), userId, session -> {
			final UserInfos userInfos = UserUtils.sessionToUserInfos(session);
			final JsonObject required = new JsonObject()
				.put(FIELD_MUST_CHANGE_PWD, getOrElse(session.getBoolean("forceChangePassword"), false))
				.put(FIELD_MUST_VALIDATE_EMAIL, false)
				.put(FIELD_MUST_VALIDATE_TERMS, false);

            if (userInfos == null) {
                // Disconnected user => nothing to validate
				//---
				promise.complete( required );
            } else {
				// force change password ?
				//---
				if( session != null ) {
					required.put(FIELD_MUST_CHANGE_PWD, getOrElse(session.getBoolean("forceChangePassword"), false));
				}
	
				// Connected users with a truthy "needRevalidateTerms" attributes are required to validate the Terms of use.
				//---
				boolean needRevalidateTerms = false;
				//check whether user has validate terms in current session
				final Object needRevalidateTermsFromSession = userInfos.getAttribute(NEED_REVALIDATE_TERMS);
				if (needRevalidateTermsFromSession != null) {
					needRevalidateTerms = Boolean.valueOf(needRevalidateTermsFromSession.toString());
				} else {
					//check whether he has validated previously
					final Map<String, Object> otherProperties = userInfos.getOtherProperties();
					if (otherProperties != null && otherProperties.get(NEED_REVALIDATE_TERMS) != null) {
						needRevalidateTerms = (Boolean) otherProperties.get(NEED_REVALIDATE_TERMS);
					} else {
						needRevalidateTerms = true;
					}
				}
				required.put(FIELD_MUST_VALIDATE_TERMS, needRevalidateTerms);
				
				// As of 2022-11-23, only ADMLs are required to validate their email address (if not done already).
				//---
				if( ! userInfos.isADML() ) {
					promise.complete( required );
				} else {
					hasValidMail(userId)
					.onSuccess( emailState -> {
						if( ! "valid".equals(emailState.getString("state")) ) {
							required.put(FIELD_MUST_VALIDATE_EMAIL, true);
						}
						promise.complete( required );
					})
					.onFailure( e -> {promise.complete(required);} );
				}
			}
		});
		return promise.future();
	}

	/** 
	 * @return {
	 * 	email: String|null, emailState: JsonObject|null,
	 *  firstName:string, lastName:string, displayName:string
	 * }
	 */
	private Future<JsonObject> retrieveFullMailState(String userId) {
		final Promise<JsonObject> promise = Promise.promise();
		String query =
				"MATCH (u:`User` { id : {id}}) " +
				"RETURN COALESCE(u.email, null) as email, COALESCE(u.emailState, null) as emailState, " + 
					   "u.firstName as firstName, u.lastName as lastName, u.displayName as displayName ";
		JsonObject params = new JsonObject().put("id", userId);
		neo.execute(query, params, m -> {
			Either<String, JsonObject> r = validUniqueResult(m);
			if (r.isRight()) {
				final JsonObject result = r.right().getValue();
				result.put("emailState", fromRaw(result.getString("emailState")));
				promise.complete( result );
			} else {
				promise.fail(r.left().getValue());
			}
		});
		return promise.future();
	}

	/**
	 * @return emailState
	 */
	private Future<JsonObject> updateMailState(String userId, final JsonObject emailState) {
		final Promise<JsonObject> promise = Promise.promise();
		StringBuilder query = new StringBuilder(
			"MATCH (u:`User` { id : {id}}) " +
			"SET u.emailState = {state} "
		);
		JsonObject params = new JsonObject()
			.put("id", userId)
			.put("state", toRaw(emailState));
		if( EmailStateUtils.getState(emailState) == EmailStateUtils.VALID 
				&& !StringUtils.isEmpty(EmailStateUtils.getValid(emailState)) ) {
			query.append(", u.email = {email}, u.emailSearchField = LOWER({email}) ");
			params.put("email", EmailStateUtils.getValid(emailState));
		}
		neo.execute(query.toString(), params, m -> {
			Either<String, JsonObject> r = validEmpty(m);
			if (r.isRight()) {
				promise.complete(emailState);
			} else {
				promise.fail(r.left().getValue());
			}
		});
		return promise.future();
	}

    /**
     * Since Neo4j does not allow JSON objects to be node properties,
     * User.emailState is stored as a JSON string
	 * => serialize it
     * @param emailState as JSON object
     * @return emailState as JSON string
     */
    private String toRaw(final JsonObject emailState) {
        if( emailState==null ) return null;
        return emailState.encode();
    }

    /**
     * Since Neo4j does not allow JSON objects to be node properties,
     * User.emailState is stored as a JSON string
	 * => deserialize it
     * @param emailState as JSON string
     * @return emailState as JSON object
     */
    private JsonObject fromRaw(final String emailState) {
        if( emailState==null ) return null;
        return new JsonObject(emailState);
    }

	/** Generate a pseudo-random code of 6 digits length. */
	private String generateRandomCode() {
		return String.format("%06d", Math.round(Math.random()*999999D));
	}

	private JsonObject formatAsResponse(final int state, final String valid, final Integer tries, final Long ttl) {
		final JsonObject o = new JsonObject()
		.put("state", stateToString(state))
		.put("valid", (valid!=null) ? valid : "");
		if( state==PENDING || state==OUTDATED ) {
			if(tries!=null) o.put("tries", tries);
			if(ttl!=null) o.put("ttl", ttlToRemainingSeconds(ttl.longValue()));
		}
		return o;
	}

	@Override
	public Future<JsonObject> setPendingMail(String userId, String email, final long validDurationS, final int triesLimit) {
		return retrieveFullMailState(userId)
		.compose( j -> {
			// Reset the mailState to a pending state
			final JsonObject originalState = j.getJsonObject("emailState", new JsonObject());
			setState(originalState, PENDING);
			// Valid mail must stay unchanged if not null, otherwise initialize to an empty string.
			if( getValid(originalState) == null ) {
				setValid(originalState, "");
			}
			setPending(originalState, email);
			setKey(originalState, generateRandomCode());
			setTtl(originalState, System.currentTimeMillis() + validDurationS * 1000l);
			setTries(originalState, triesLimit);

			return updateMailState(userId, originalState);
		});
	}

	@Override
	public Future<JsonObject> hasValidMail(String userId) {
		return retrieveFullMailState(userId)
		.map( j -> {
			Integer state = null;
			String email = j.getString("email");
			JsonObject emailState = j.getJsonObject("emailState");

			if (StringUtils.isEmpty(email) || emailState == null) {
				state = UNCHECKED;
			} else if( !email.equalsIgnoreCase( getValid(emailState) )) {
				// Case where the email was first validated and then changed.
				state = getState(emailState);
				if( state == VALID ) {
					state = UNCHECKED;
				}
			} else {
				// If email===valid, then state must be valid
				state = VALID; // /!\ do not replace by   state = getState(emailState); /!\
			}
			return formatAsResponse(state, getValid(emailState), null, null);
		});
	}

	@Override
	public Future<JsonObject> tryValidateMail(String userId, String code) {
		return retrieveFullMailState(userId)
		.compose( j -> {
			JsonObject emailState = j.getJsonObject("emailState");

			// Check business rules
			do {
				if( emailState == null ) {
					// Unexpected data, should never happen
					emailState = new JsonObject();
					j.put("emailState", emailState);
					setState(emailState, OUTDATED);
					break;
				}
				// if TTL or max tries reached don't check code
				if (getState(emailState) == OUTDATED) {
					break;
				}
				// Check code
				String key = StringUtils.trimToNull( getKey(emailState) );
				if( key == null || !key.equals(StringUtils.trimToNull(code)) ) {
					// Invalid
					Integer tries = getTries(emailState);
					if(tries==null) {
						tries = 0;
					} else {
						tries = Math.max(0, tries.intValue() - 1 );
					}
					if( tries <= 0 ) {
						setState(emailState, OUTDATED);
					}
					setTries(emailState, tries);
					break;
				}
				// Check time to live
				Long ttl = getTtl(emailState);
				if( ttl==null || ttl.compareTo(System.currentTimeMillis()) < 0 ) {
					// TTL reached
					setState(emailState, OUTDATED);
					break;
				}
				// Check pending mail address
				String pending = StringUtils.trimToNull( getPending(emailState) );
				if( pending == null) {
					// This should never happen, but treat it like TTL was reached
					setState(emailState, OUTDATED);
					break;
				}
				// ---Validation succeeded---
				setState(emailState, VALID);
				setValid(emailState, pending);
				setPending(emailState, null);
				setKey(emailState, null);
				setTtl(emailState, null);
				setTries(emailState, null);
			} while(false);

			// ---Validation results---
			return updateMailState(userId, emailState)
			.map( newState -> {
				return formatAsResponse(getState(newState), getValid(newState), getTries(newState), getTtl(newState));
			});
		});
	}

	@Override
	public Future<JsonObject> getMailState(String userId) {
		return retrieveFullMailState(userId);
	}

	@Override
    public Future<Long> sendValidationEmail( final HttpServerRequest request, String email, JsonObject templateParams ) {
    	Promise<Long> promise = Promise.promise();
		if( emailSender == null ) {
			promise.complete(null);
		} else if( StringUtils.isEmpty((email)) ) {
			promise.fail("Invalid email address.");
		} else if( templateParams==null || StringUtils.isEmpty(templateParams.getString("code")) ) {
			promise.fail("Invalid parameters.");
		} else {
			String code = templateParams.getString("code");
			processEmailTemplate(request, templateParams, "email/emailValidationCode.html", false, processedTemplate -> {
				// Generate email subject
				final JsonObject timelineI18n = (requestThemeKV==null ? getThemeDefaults():requestThemeKV).getOrDefault( I18n.acceptLanguage(request).split(",")[0].split("-")[0], new JsonObject() );
				final String title = timelineI18n.getString("timeline.immediate.mail.subject.header", "") 
					+ I18n.getInstance().translate("email.validation.subject", getHost(request), I18n.acceptLanguage(request), code);
				
				emailSender.sendEmail(request, email, null, null,
					title,
					processedTemplate,
					null,
					false,
					ar -> {
						if (ar.succeeded()) {
							Message<JsonObject> reply = ar.result();
							if ("ok".equals(reply.body().getString("status"))) {
								Object r = reply.body().getValue("result");
								promise.complete( 0l );
							} else {
								promise.fail( reply.body().getString("message", "") );
							}
						} else {
							promise.fail(ar.cause().getMessage());
						}
					}
				);
			});
		}
    	return promise.future();
    }

	private void processEmailTemplate(
			final HttpServerRequest request, 
			JsonObject parameters, 
			String template, 
			boolean reader, 
			final Handler<String> handler
			) {
		// From now until the end of the template processing, code execution cannot be async.
		// So initialize requestedThemeKV here and now.
		loadThemeKVs(request)
		.onSuccess( themeKV -> {
			this.requestThemeKV = themeKV;
			if(reader){
				final StringReader templateReader = new StringReader(template);
				processTemplate(request, parameters, "", templateReader, new Handler<Writer>() {
					public void handle(Writer writer) {
						handler.handle(writer.toString());
					}
				});
	
			} else {
				processTemplate(request, template, parameters, handler);
			}
		});
	}

	//FIXME The whole methods below are intended to retrieve overloaded i18n from Timeline because it contains variables for email templating...

	private Future<String> getThemePath(HttpServerRequest request) {
        Promise<String> promise = Promise.promise();
		vertx.eventBus().request( 
			"portal",
			new JsonObject().put("action", "getTheme"),
			new DeliveryOptions().setHeaders(request.headers()), 
			handlerToAsyncHandler( reply -> {
				promise.complete( String.join(File.separator, config.getString("assets-path", "../.."), "assets", "themes", reply.body().getString("theme")) );
			})
		);
		return promise.future();
	}

	private Future<Map<String, JsonObject>> loadThemeKVs(final HttpServerRequest request) {
        Promise<Map<String, JsonObject>> promise = Promise.promise();
		getThemePath(request).onComplete( result -> {
			if( result.succeeded() ) {
				final String i18nDirectory = String.join(File.separator, result.result(), "i18n", "Timeline");
				vertx.fileSystem().exists(i18nDirectory, ar -> {
					if (ar.succeeded() && ar.result()) {
						vertx.fileSystem().readDir(i18nDirectory, asyncResult -> {
							if (asyncResult.succeeded()) {
								readI18nTimeline(asyncResult.result())
								.onSuccess( themeKV -> promise.complete(themeKV) );
							} else {
								log.error("Error loading assets at "+i18nDirectory, asyncResult.cause());
								promise.complete(null);
							}
						});
					} else if (ar.failed()) {
						log.error("Error loading assets at "+i18nDirectory, ar.cause());
						promise.complete(null);
					}
				});
			}
		});
		return promise.future();
	}

	private Map<String, JsonObject> getThemeDefaults() {
		Map<String, JsonObject> themeKVs = new HashMap<String, JsonObject>();
		themeKVs.put("fr", new JsonObject()
			.put("timeline.mail.body.bgcolor", "#f9f9f9")
			.put("timeline.mail.body.bg", "background-color: #f9f9f9;")
			.put("timeline.mail.main", "background-color: #fff;")
			.put("timeline.mail.main.border", "border: 1px solid #e9e9e9;")
			.put("timeline.mail.maincolor", "#fff")
			.put("timeline.mail.text.color", "color: #fff;")
			.put("timeline.mail.header.bg", "background-color: #209DCC;")
			.put("timeline.mail.header.bgcolor", "#209DCC")
			.put("timeline.mail.main.text.color", "color: #fff;")
			.put("timeline.mail.footer.color", "color: #999;")
		);
		return themeKVs;
	}

	private Future<Map<String, JsonObject>> readI18nTimeline(List<String> filePaths) {
		Promise<Map<String, JsonObject>> promise = Promise.promise();
		final Map<String, JsonObject> themeKV = new HashMap<String, JsonObject>();
		final AtomicInteger count = new AtomicInteger(filePaths.size());
		for(final String path : filePaths) {
			vertx.fileSystem().props(path, new Handler<AsyncResult<FileProps>>() {
				@Override
				public void handle(AsyncResult<FileProps> ar) {
					if (ar.succeeded() && ar.result().isRegularFile()) {
						final String k = new File(path).getName().split("\\.")[0];
						vertx.fileSystem().readFile(path, ar2 -> {
							if (ar2.succeeded()) {
								JsonObject jo = new JsonObject(ar2.result().toString("UTF-8"));
								themeKV.put(k, jo);
							}
							if (count.decrementAndGet() == 0) {
								promise.complete(themeKV);
							}
						});
					} else {
						if (count.decrementAndGet() == 0) {
							promise.complete(themeKV);
						}
					}
				}
			});
		}
		return promise.future();
	}

	/* Override i18n to use additional theme variables */
	@Override
	protected void setLambdaTemplateRequest(final HttpServerRequest request) {
		super.setLambdaTemplateRequest(request);

		this.templateProcessor.setLambda("theme", new Mustache.Lambda() {
			@Override
			public void execute(Template.Fragment frag, Writer out) throws IOException {
				String key = frag.execute();
				String language = getOrElse(I18n.acceptLanguage(request), "fr", false);

				// #46383, translations from the theme takes precedence over those from the domain
				final String translatedContents = I18n.getInstance().translate(key, Renders.getHost(request), I18n.getTheme(request), I18n.getLocale(language));
				if (!translatedContents.equals(key)) {
					Mustache.compiler().compile(translatedContents).execute(frag, out);
				} else {
					JsonObject timelineI18n = (requestThemeKV==null ? getThemeDefaults():requestThemeKV).getOrDefault( language.split(",")[0].split("-")[0], new JsonObject() );
					Mustache.compiler().compile(timelineI18n.getString(key, key)).execute(frag, out);
				}
			}
		});
	}
}
