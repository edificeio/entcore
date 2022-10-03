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
import fr.wseduc.webutils.email.EmailSender;

import org.entcore.common.neo4j.Neo4j;

import static org.entcore.common.emailstate.EmailStateUtils.*;
import static org.entcore.common.neo4j.Neo4jResult.*;

import org.entcore.common.emailstate.EmailStateUtils;
import org.entcore.common.utils.StringUtils;
import org.entcore.directory.services.MailValidationService;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public class DefaultMailValidationService implements MailValidationService {
	private final Neo4j neo = Neo4j.getInstance();
	private EmailSender emailSender = null;

	public DefaultMailValidationService(EmailSender sender) {
		this.emailSender = sender;
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

			if (email == null || emailState == null) {
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
			/*
			JsonObject json = new JsonObject()
				.put("activationUri", notification.getHost(request) +
						"/auth/activation?login=" + login +
						"&activationCode=" + activationCode)
				.put("host", notification.getHost(request))
				.put("login", login);
			*/
			//Promise<Message<JsonObject>> handleSend = Promise.promise();
			emailSender.sendEmail(
					request, email, null, null,
					"email.validation.subject", "email/emailValidationCode.html", templateParams, true, ar -> {
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
			});

		}

/*
		try {
    		String locale = email.getString("locale");
	        JsonObject i18n = I18n.getInstance().load( locale );
	        JsonArray recipients = new JsonArray( email.getString("recipients", "[]") );
	        JsonObject content = new JsonObject( email.getString("content", "{}") );
	        JsonArray headers = new JsonArray( email.getString("headers", "[]") );
	        String templateName = email.getString("template_name");
	        String titleKey = email.getString("title_key");
	        Long orderId = email.getLong("order_id");
	        
	        content.put("host", config.getString("host"));
	        
	        this.renderer.readTemplate(templateName).setHandler(templateBufferResult -> {
	            if (templateBufferResult.failed()) {
	                log.error("[CRM] Error reading template on file system: " + templateBufferResult.cause().toString());
	                promise.complete(false);
	                return;
	            }
                Buffer templateBuffer = templateBufferResult.result();
                StringReader reader = new StringReader(templateBuffer.toString("UTF-8"));
                HttpServerRequest request = new JsonHttpServerRequest(new JsonObject()
                    .put("headers", new JsonObject().put("Accept-Language", locale)));
                
            	for (int i = 0; i < recipients.size(); i++) {
                    JsonObject mail_data = recipients.getJsonObject(i);
                    mail_data.mergeIn(content, true);
                    
                    this.renderer.generateHtmlFile(request, mail_data, templateName, reader).setHandler(htmlContentResult -> {
                    	if( htmlContentResult.failed() ) {
                    		log.error("[CRM] Error generating email from template: " + htmlContentResult.cause().toString());
                    		promise.complete(false);
                    		return;
                    	}
                    	final String mailTitle = i18n.getString(titleKey);
                    	final String htmlContent = htmlContentResult.result();
                        this.emailSender.sendEmail(
                            request,
                            mail_data.getString("email"),	// to
                            null,							// cc
                            null,							// bcc
                            mailTitle,						// subject
                            htmlContent,					// templateBody
                            null,							// templateParams 
                            false, 							// translateSubject
                            headers,						// headers
                            handlerToAsyncHandler( event -> {
                                if ("error".equals(event.body().getString("status"))) {
                                    log.error( "[CRM] Error while sending mail (" + event.body().getString("message", "") + ")" );
                                    promise.complete( false );
                                } else {
                                	if( orderId!=null ) {
                                		try {
		                                    emailService.create(
		                                		mail_data.getString("firstname"), mail_data.getString("lastname"),
		                                		mail_data.getString("email"), mail_data.getString("relation", ""), 
		                                        mailTitle, StringUtils.stripSpacesSentence(substringBetween(htmlContent, "<!--Content-->", "<!--End-->")),
		                                        orderId, done -> {
		                                        if (done.isLeft()) {
		                                            log.error("[CRM] Error while creating email_history row (" + done.left().getValue() + ")");
		                                        }
		                                    });
                                		} catch (NumberFormatException nfe ) {
                                			// no order id => no email history.
                                		}
                                	}
                                    promise.complete(true);
                                }
                            })
                        );
                	});
                }
            });
    	} catch( Exception e ) {
    		log.error("[CRM] Unknown error while sending an email from the sendbox", e);
    		promise.complete(false);
    	}
 */		
    	return promise.future();
    }

}
