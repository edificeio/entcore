package org.entcore.common.datavalidation.impl;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static fr.wseduc.webutils.Utils.getOrElse;
import static org.entcore.common.user.SessionAttributes.*;
import static org.entcore.common.neo4j.Neo4jResult.*;

import java.io.StringReader;
import java.io.Writer;

import java.util.Map;

import org.entcore.common.datavalidation.EmailValidation;
import org.entcore.common.datavalidation.UserValidationService;
import org.entcore.common.datavalidation.utils.DataStateUtils;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sms.Sms;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.Mfa;
import org.entcore.common.utils.StringUtils;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.email.EmailSender;


/**
 * @see {@link EmailValidation} {@link MobileValidation} utility classes for easier use.
 */
public class DefaultUserValidationService implements UserValidationService {
	private static final Logger logger = LoggerFactory.getLogger(DefaultUserValidationService.class);

    /** Inner service for the "mobile" field validation. */
    //---------------------------------------------------------------
    private class MobileField extends AbstractDataValidationService {
    //---------------------------------------------------------------
        MobileField(io.vertx.core.Vertx vertx, io.vertx.core.json.JsonObject config) {
            super("mobile", "mobileState", vertx, config);
        }

        @Override
        public Future<Long> sendValidationMessage( final HttpServerRequest request, String mobile, JsonObject templateParams ) {
            Sms sms = Sms.getFactory().newInstance( this );
            return sms.send(request, mobile, "phone/mobileVerification.txt", templateParams).map( j -> 0L );
        }
    }

    /** Inner service for the "email" field validation. */
    //---------------------------------------------------------------
    private class EmailField extends AbstractDataValidationService {
    //---------------------------------------------------------------
        private EmailSender emailSender = null;

        EmailField(io.vertx.core.Vertx vertx, io.vertx.core.json.JsonObject config) {
            super("email", "emailState", vertx, config);
            emailSender = new EmailFactory(this.vertx, config).getSenderWithPriority(EmailFactory.PRIORITY_HIGH);
        }

        @Override
        public Future<Long> sendValidationMessage( final HttpServerRequest request, String email, JsonObject templateParams ) {
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
    }

    //---------------------------------------------------------------
	private final Neo4j neo = Neo4j.getInstance();
    private EmailField emailSvc = null;
    private MobileField mobileSvc = null;
    private int ttlInSeconds     = 600;  // Validation codes are valid 10 minutes by default
    private int retryNumber      = 5;    // Validation code can be typed in 5 times by default
    private int waitInSeconds    = 10;   // Email is awaited 10 seconds by default (it's a front-side parameter)

    public DefaultUserValidationService(final io.vertx.core.Vertx vertx, final io.vertx.core.json.JsonObject config, final JsonObject params) {
        if( params != null ) {
            ttlInSeconds    = params.getInteger("ttlInSeconds", 600);
            retryNumber     = params.getInteger("retryNumber",  5);
            waitInSeconds   = params.getInteger("waitInSeconds", 10);
        }
        emailSvc = new EmailField(vertx, config);
        mobileSvc= new MobileField(vertx, config);
    }

	/** 
	 * @return {
	 * 	forceChangePassword: true|false,
     *  needRevalidateTerms: true|false
	 * }
	 */
	private Future<JsonObject> retrieveUncachedData(String userId) {
		final Promise<JsonObject> promise = Promise.promise();
		String query =
				"MATCH (u:`User` { id : {id}}) " +
				"RETURN COALESCE(u.changePw, FALSE) as forceChangePassword, COALESCE(u.needRevalidateTerms, FALSE) as needRevalidateTerms ";
		JsonObject params = new JsonObject().put("id", userId);
		neo.execute(query, params, m -> {
			Either<String, JsonObject> r = validUniqueResult(m);
			if (r.isRight()) {
				promise.complete( r.right().getValue() );
			} else {
				promise.fail(r.left().getValue());
			}
		});
		return promise.future();
    }

    @Override
	public Boolean getIsMFA(final JsonObject session) {
        return Boolean.valueOf( session.getJsonObject("cache").getString(IS_MFA, "false") );
    }

    @Override
	public Future<Boolean> setIsMFA(final EventBus eb, final String sessionId, final boolean status) {
        Promise<Boolean> promise = Promise.promise();
        if( StringUtils.isEmpty(sessionId) ) {
            promise.fail("Session ID is null");
        } else {
            UserUtils.addSessionAttributeOnId( eb, sessionId, IS_MFA, Boolean.toString(status), result -> {
                promise.complete(result);
            });
        }
        return promise.future();
    }

    @Override
	public Future<Boolean> needMFA(final JsonObject session) {
        final UserInfos infos = UserUtils.sessionToUserInfos(session);
        if( infos == null ) {
            // Non-connected users do not have to perform MFA
            return Future.succeededFuture(Boolean.FALSE);
        }
        return needMFA(session, infos);
    }

	private Future<Boolean> needMFA(final JsonObject session, final UserInfos infos) {
        // As of 2023-01-27, an MFA is needed to access protected zones if and only if :
        // - no MFA has already been performed during this session,
        // - user is ADMx,
        // - MFA is activated at platform-level,
        // - all structures, the user is attached to, are not ignoring MFA
        if( Boolean.TRUE.equals(getIsMFA(session))
         || infos == null
         || !(infos.isADMC() || infos.isADML())
         || Mfa.isNotActivatedForUser(infos)
            ) {
            return Future.succeededFuture(Boolean.FALSE);
        }

        // otherwise MFA is needed
        return Future.succeededFuture(Boolean.TRUE);
    }

    @Override
    public int getDefaultTtlInSeconds() {
        return ttlInSeconds;
    }

    @Override
    public int getDefaultRetryNumber() {
        return retryNumber;
    }

    @Override
    public int getDefaultWaitInSeconds() {
        return waitInSeconds;
    }

    @Override
    public Future<JsonObject> getMandatoryUserValidation(final JsonObject session, final boolean forced) {
        Promise<JsonObject> promise = Promise.promise();

        final JsonObject required = new JsonObject()
        .put(FIELD_MUST_CHANGE_PWD, getOrElse(session.getBoolean("forceChangePassword"), false))
        .put(FIELD_MUST_VALIDATE_TERMS, false)
        .put(FIELD_MUST_VALIDATE_EMAIL, false)
        .put(FIELD_MUST_VALIDATE_MOBILE, false)
        .put(FIELD_NEED_MFA, false);

        final UserInfos userInfos = UserUtils.sessionToUserInfos( session );
        if (userInfos == null) {
            // Disconnected user => nothing to validate
            //---
            promise.complete( required );
        } else {
            // Default data
            Future<JsonObject> checkedValidations = Future.succeededFuture(required);

            if( session != null ) {
                // Fresh data
                // #WB-1023 + #WB-1541 : Temporary dirty fix
                // forceChangePassword and needRevalidateTerms are retrieved from the session (see above), but may be cached and outdated.
                // Until session cache management is improved, when forced, read the value from the DB directly.
                checkedValidations = forced 
                    ? retrieveUncachedData(userInfos.getUserId())
                    : Future.succeededFuture(session);
            }

            checkedValidations.onComplete( dataReading -> {
                if( dataReading.succeeded() ) {
                    JsonObject data = dataReading.result();
                    required.put(FIELD_MUST_CHANGE_PWD, getOrElse(data.getBoolean("forceChangePassword"), false));
                    required.put(FIELD_MUST_VALIDATE_TERMS, getOrElse(data.getBoolean("needRevalidateTerms"), false));
                }
                
                if( dataReading.failed() || !forced ) {
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
                }

                CompositeFuture
                .all(
                    isEmailValidationRequired(userInfos, required),
                    isMobileValidationRequired(userInfos, required),
                    isMfaNeeded(session, userInfos, required)
                )
                .onComplete( ar -> {
                    promise.complete(required);
                });
            });
        }
		return promise.future();
    }

    /**
     * 
     * @return
     */
    private Future<Boolean> isMfaNeeded(final JsonObject session, final UserInfos userInfos, final JsonObject required) {
        return needMFA(session, userInfos)
            .map( needed -> {
                required.put( FIELD_NEED_MFA, needed );
                return needed;
            });
    }

    //////////////// Mobile-related methods ////////////////

    /**
     * Check if a user needs validating his mobile phone number.
     * 
     * As of 2023-01-23, a user is required to validate his mobile phone number, if and only if :
     * - user is ADMx,
     * - MFA is set to "sms",
     * - user's structures do not ignore MFA,
     * - mobile phone number is not already validated.
     * 
     * @return the required map parameter, updated
     */
    private Future<JsonObject> isMobileValidationRequired(final UserInfos userInfos, final JsonObject required) {
        if( (userInfos.isADML() || userInfos.isADMC()) && !Boolean.TRUE.equals(userInfos.getIgnoreMFA()) && Mfa.withSms() ){
            final Promise<JsonObject> promise = Promise.promise();
            hasValidMobile(userInfos.getUserId())
            .onSuccess( mobileState -> {
                if( ! "valid".equals(mobileState.getString("state")) ) {
                    required.put(FIELD_MUST_VALIDATE_MOBILE, true);
                }
                promise.complete(required);
            })
            .onFailure( e -> {promise.complete(required);} );
            return promise.future();
        } else {
            required.put(FIELD_MUST_VALIDATE_MOBILE, false);
            return Future.succeededFuture(required);
        }
    }

	@Override
	public Future<JsonObject> hasValidMobile(String userId) {
        return mobileSvc.hasValid(userId);
    }

	@Override
	public Future<JsonObject> setPendingMobile(String userId, String mobile) {
        return mobileSvc.startUpdate(userId, mobile, ttlInSeconds, retryNumber);
    }

	@Override
	public Future<JsonObject> tryValidateMobile(String userId, String code) {
        return mobileSvc.tryValidate(userId, code);
    }

	@Override
	public Future<JsonObject> getMobileState(String userId) {
        return mobileSvc.getCurrentState(userId)
        .map( t -> {
            // Add missing data
            t.put("waitInSeconds", waitInSeconds);
            return t;
        });
    }

	@Override
	public Future<Long> sendValidationSMS(HttpServerRequest request, UserInfos infos, JsonObject mobileState) {
        final Long expires = getOrElse(DataStateUtils.getTtl(mobileState), waitInSeconds*1000l);

        JsonObject templateParams = new JsonObject()
        .put("scheme", Renders.getScheme(request))
        .put("host", Renders.getHost(request))
        .put("userId", infos.getUserId())
        .put("firstName", infos.getFirstName())
        .put("lastName", infos.getLastName())
        .put("userName", infos.getUsername())
        .put("duration", Math.round(DataStateUtils.ttlToRemainingSeconds(expires) / 60f))
        .put("code", DataStateUtils.getKey(mobileState));

        return mobileSvc.sendValidationMessage( request, DataStateUtils.getPending(mobileState), templateParams );
    }    

    //////////////// Email-related methods ////////////////

    /**
     * Check if a user needs validating his email address.
     * 
     * As of 2023-01-23, a user is required to validate his email address, if and only if :
     * - user is ADMx,
     * - MFA is set to "email",
     * - user's structures do not ignore MFA,
     * - email address is not already validated.
     * 
     * @return the required map parameter, updated
     */
    private Future<JsonObject> isEmailValidationRequired(final UserInfos userInfos, final JsonObject required) {
        if( (userInfos.isADML() || userInfos.isADMC()) && !Boolean.TRUE.equals(userInfos.getIgnoreMFA()) && Mfa.withEmail() ){
            final Promise<JsonObject> promise = Promise.promise();
            hasValidEmail(userInfos.getUserId())
            .onSuccess( emailState -> {
                if( ! "valid".equals(emailState.getString("state")) ) {
                    required.put(FIELD_MUST_VALIDATE_EMAIL, true);
                }
                promise.complete(required);
            })
            .onFailure( e -> {promise.complete(required);} );
            return promise.future();
        } else {
            required.put(FIELD_MUST_VALIDATE_EMAIL, false);
            return Future.succeededFuture(required);
        }
    }

	@Override
    public Future<JsonObject> hasValidEmail(String userId) {
        return emailSvc.hasValid(userId);
    }

    @Override
	public Future<JsonObject> setPendingEmail(String userId, String email) {
        return emailSvc.startUpdate(userId, email, ttlInSeconds, retryNumber);
    }

    @Override
	public Future<JsonObject> tryValidateEmail(String userId, String code) {
        return emailSvc.tryValidate(userId, code);
    }

    @Override
	public Future<JsonObject> getEmailState(String userId) {
        return emailSvc.getCurrentState(userId)
        .map( t -> {
            // Add missing data
            t.put("waitInSeconds", waitInSeconds);
            return t;
        });
    }

    @Override
	public Future<Long> sendValidationEmail(HttpServerRequest request, UserInfos infos, JsonObject emailState) {
        final Long expires = getOrElse(DataStateUtils.getTtl(emailState), waitInSeconds*1000l);

        JsonObject templateParams = new JsonObject()
        .put("scheme", Renders.getScheme(request))
        .put("host", Renders.getHost(request))
        .put("userId", infos.getUserId())
        .put("firstName", infos.getFirstName())
        .put("lastName", infos.getLastName())
        .put("userName", infos.getUsername())
        .put("duration", Math.round(DataStateUtils.ttlToRemainingSeconds(expires) / 60f))
        .put("code", DataStateUtils.getKey(emailState));

        return emailSvc.sendValidationMessage( request, DataStateUtils.getPending(emailState), templateParams );
    }
}
