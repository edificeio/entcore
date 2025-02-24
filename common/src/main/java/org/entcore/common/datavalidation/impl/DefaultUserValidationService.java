package org.entcore.common.datavalidation.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.datavalidation.EmailValidation;
import org.entcore.common.datavalidation.UserValidationService;
import org.entcore.common.datavalidation.metrics.DataValidationMetricsFactory;
import org.entcore.common.datavalidation.utils.DataStateUtils;
import org.entcore.common.datavalidation.utils.UserValidationFactory;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.events.EventStore;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sms.SmsSender;
import org.entcore.common.sms.SmsSenderFactory;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.Mfa;
import org.entcore.common.utils.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static fr.wseduc.webutils.Utils.getOrElse;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResult;
import static org.entcore.common.user.SessionAttributes.IS_MFA;
import static org.entcore.common.user.SessionAttributes.NEED_REVALIDATE_TERMS;


/**
 * @see {@link EmailValidation} {@link MobileValidation} utility classes for easier use.
 */
public class DefaultUserValidationService implements UserValidationService {
	private static final Logger logger = LoggerFactory.getLogger(DefaultUserValidationService.class);

    /** Inner service for the "mobile" field validation. */
    //---------------------------------------------------------------
    private class MobileField extends AbstractDataValidationService {
    //---------------------------------------------------------------
        private EmailSender emailSender = null;

        MobileField(io.vertx.core.Vertx vertx, io.vertx.core.json.JsonObject config, io.vertx.core.json.JsonObject params) {
            super("mobile", "mobileState", vertx, config, params);
            emailSender = new EmailFactory(this.vertx, config).getSenderWithPriority(EmailFactory.PRIORITY_HIGH);
        }

        @Override
        public Future<String> sendValidationMessage( final HttpServerRequest request, String mobile, JsonObject templateParams, final String module ) {
            final SmsSender sms = SmsSenderFactory.getInstance().newInstance( this, eventStore );
            return sms.sendUnique(request, mobile, "phone/mobileVerification.txt", templateParams, module);
        }

        @Override
        public Future<String> sendWarningMessage(HttpServerRequest request, Map<String, String> targets, JsonObject templateParams) {
            Promise<String> promise = Promise.promise();
            if (!StringUtils.isEmpty(targets.get("mobile"))) {
                sendWarningSMS(request, targets.get("mobile"), templateParams)
                        .onFailure(e -> log.error("Failed to send mobile update warning SMS", e));
            }
            if (!StringUtils.isEmpty(targets.get("email"))) {
                sendWarningEmail(request, targets.get("email"), templateParams)
                        .onFailure(e -> log.error("Failed to send mobile update warning email", e));
            }
            return promise.future();
        }

        private Future<String> sendWarningSMS(final HttpServerRequest request, String mobile, JsonObject templateParams) {
            final SmsSender sms = SmsSenderFactory.getInstance().newInstance(this, eventStore);
            return sms.sendUnique(request, mobile, "phone/mobileUpdateWarning.txt", templateParams, "CHANGE_NOTICE");
        }

        private Future<String> sendWarningEmail(HttpServerRequest request, String email, JsonObject templateParams) {
            Promise<String> promise = Promise.promise();
            if (emailSender == null) {
                promise.complete(null);
            } else if (StringUtils.isEmpty((email))) {
                promise.fail("Invalid email address.");
            } else {
                processEmailTemplate(request, templateParams, "email/mobileUpdateWarning.html", false, processedTemplate -> {
                    // Generate email subject
                    final JsonObject timelineI18n = (requestThemeKV == null ? getThemeDefaults() : requestThemeKV).getOrDefault(I18n.acceptLanguage(request).split(",")[0].split("-")[0], new JsonObject());
                    final String title = timelineI18n.getString("timeline.immediate.mail.subject.header", "")
                            + I18n.getInstance().translate("mobile.update.warning.subject", getHost(request), I18n.acceptLanguage(request));

                    emailSender.sendEmail(
                            request,
                            email,
                            null,
                            null,
                            title,
                            processedTemplate,
                            null,
                            false,
                            ar -> {
                                if (ar.succeeded()) {
                                    Message<JsonObject> reply = ar.result();
                                    if ("ok".equals(reply.body().getString("status"))) {
                                        Object r = reply.body().getValue("result");
                                        promise.complete( "" );
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
    }

    /** Inner service for the "email" field validation. */
    //---------------------------------------------------------------
    private class EmailField extends AbstractDataValidationService {
    //---------------------------------------------------------------
        private EmailSender emailSender = null;

        EmailField(io.vertx.core.Vertx vertx, io.vertx.core.json.JsonObject config, io.vertx.core.json.JsonObject params) {
            super("email", "emailState", vertx, config, params);
            emailSender = new EmailFactory(this.vertx, config).getSenderWithPriority(EmailFactory.PRIORITY_HIGH);
        }

        @Override
        public Future<String> sendValidationMessage( final HttpServerRequest request, String email, JsonObject templateParams, final String module) {
            Future<String> future;
            if (templateParams == null || StringUtils.isEmpty(templateParams.getString("code"))) {
                future = Future.failedFuture("Invalid parameters.");
            } else {
                future = formatEmailSubject(request,"email.validation.subject", templateParams)
                        .compose(subject -> sendEmail(request, email, subject, "email/emailValidationCode.html", templateParams)
                        );
            }
            return future;
        }

        @Override
        public Future<String> sendWarningMessage(HttpServerRequest request, Map<String, String> targets, JsonObject templateParams) {
            Future<String> future;
            if (StringUtils.isEmpty(targets.get("email"))){
                future = Future.failedFuture("Email not provided.");
            } else {
                future = formatEmailSubject(request, "email.update.warning.subject", templateParams)
                        .compose(subject -> sendEmail(request, targets.get("email"), subject, "email/emailUpdateWarning.html", templateParams));
            }
            return future;
        }

        private Future<String> sendEmail(HttpServerRequest request, String to, String subject, String templateName, JsonObject templateParams) {
            Promise<String> promise = Promise.promise();
            if (emailSender == null) {
                promise.complete(null);
            } else if (StringUtils.isEmpty((to))) {
                promise.fail("Invalid email address.");
            } else {
                processEmailTemplate(request, templateParams, templateName, false, processedTemplate -> {
                    emailSender.sendEmail(
                            request,
                            to,
                            null,
                            null,
                            subject,
                            processedTemplate,
                            null,
                            false,
                            ar -> {
                                if (ar.succeeded()) {
                                    Message<JsonObject> reply = ar.result();
                                    if ("ok".equals(reply.body().getString("status"))) {
                                        promise.complete("");
                                    } else {
                                        promise.fail(reply.body().getString("message", ""));
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

    }

    //---------------------------------------------------------------
    private EventBus eb = null;
	private final Neo4j neo = Neo4j.getInstance();
    private EventStore eventStore;
    private String eventType;
    private EmailField emailSvc = null;
    private MobileField mobileSvc = null;
    private int ttlInSeconds     = 600;  // Validation codes are valid 10 minutes by default
    private int retryNumber      = 5;    // Validation code can be typed in 5 times by default
    private int waitInSeconds    = 10;   // Email is awaited 10 seconds by default (it's a front-side parameter)

    public DefaultUserValidationService(final io.vertx.core.Vertx vertx, final io.vertx.core.json.JsonObject config, final JsonObject params) {
        eb = Server.getEventBus(vertx);
        if( params != null ) {
            ttlInSeconds    = params.getInteger("ttlInSeconds", 600);
            retryNumber     = params.getInteger("retryNumber",  5);
            waitInSeconds   = params.getInteger("waitInSeconds", 10);
        }
        emailSvc = new EmailField(vertx, config, params);
        mobileSvc= new MobileField(vertx, config, params);
    }

	public DefaultUserValidationService setEventStore(EventStore eventStore, String eventType) {
		this.eventStore = eventStore;
        this.eventType = eventType;
        return this;
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
        return session!=null && Boolean.valueOf( session.getJsonObject("cache", new JsonObject()).getString(IS_MFA, "false") );
    }

    @Override
	public Future<Boolean> setIsMFA(final String sessionId, final boolean status) {
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
        // - no MFA has already been performed during this session, and
        // - user is ADMx, and
        // - MFA is activated at platform-level, and
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
     * - data validation is not deactivated at startup,
     * - mobile phone number is not already validated.
     * 
     * @return the required map parameter, updated
     */
    private Future<JsonObject> isMobileValidationRequired(final UserInfos userInfos, final JsonObject required) {
        if( (userInfos.isADML() || userInfos.isADMC())
         && !Boolean.TRUE.equals(userInfos.getIgnoreMFA())
         && Mfa.withSms()
         && !UserValidationFactory.getFactory().deactivateValidationAfterLogin
         ){
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
	public Future<JsonObject> tryValidateMobile(String sessionId, String userId, String code) {
        return mobileSvc.tryValidate(userId, code)
        .compose( result -> {
            if( result!=null && "valid".equalsIgnoreCase(result.getString("state")) ) {
                return (sessionId!=null
                    ? setIsMFA(sessionId, true).map(isMfaSet -> result)
                    : Future.succeededFuture(result)
                )
                // Code was consumed => this is a metric to follow
                .onComplete( ar -> DataValidationMetricsFactory.getRecorder().onMobileCodeConsumed() );
            }
            return Future.succeededFuture(result);
        });
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
	public Future<String> sendValidationSMS(HttpServerRequest request, UserInfos infos, JsonObject mobileState) {
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

        return mobileSvc.sendValidationMessage( request, DataStateUtils.getPending(mobileState), templateParams, "VALIDATION" )
        .map( id -> {
            // Code was sent => this is a metric to follow
            DataValidationMetricsFactory.getRecorder().onMobileCodeGenerated();
            // Code was sent => trace this event
            if( eventStore != null && eventType != null ) {
                eventStore.createAndStoreEvent(eventType, request, new JsonObject().put("override-module", "MFA"));
            }
            return id;
        });
    }

    @Override
    public Future<String> sendUpdateMobileWarning(HttpServerRequest request, UserInfos userInfos, JsonObject mobileState) {
        JsonObject templateParams = new JsonObject()
                .put("scheme", Renders.getScheme(request))
                .put("host", Renders.getHost(request))
                .put("userId", userInfos.getUserId())
                .put("firstName", userInfos.getFirstName())
                .put("lastName", userInfos.getLastName())
                .put("date", LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) * 1000)
                .put("oldPhoneNumber", userInfos.getMobile())
                .put("newPhoneNumber", DataStateUtils.getValid(mobileState));

        Map<String, String> targets = new HashMap<>();
        targets.put("email", userInfos.getEmail());
        targets.put("mobile", userInfos.getMobile());

        return mobileSvc.sendWarningMessage(request, targets, templateParams);
    }

    //////////////// Email-related methods ////////////////

    /**
     * Check if a user needs validating his email address.
     * 
     * As of 2023-01-23, a user is required to validate his email address, if and only if :
     * - user is ADMx,
     * - MFA is set to "email",
     * - user's structures do not ignore MFA,
     * - data validation is not deactivated at startup,
     * - email address is not already validated.
     * 
     * @return the required map parameter, updated
     */
    private Future<JsonObject> isEmailValidationRequired(final UserInfos userInfos, final JsonObject required) {
        if( ((userInfos.isADML() || userInfos.isADMC())
         && !Boolean.TRUE.equals(userInfos.getIgnoreMFA()) 
         && Mfa.withEmail()
         && !UserValidationFactory.getFactory().deactivateValidationAfterLogin)
       || ("Relative".equals(userInfos.getType()) && UserValidationFactory.getFactory().activateValidationRelative )
         ){
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
        return emailSvc.tryValidate(userId, code)
        .map( result -> {
            // Code was consumed => this is a metric to follow
            DataValidationMetricsFactory.getRecorder().onEmailCodeConsumed();
            return result;
        });
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
	public Future<String> sendValidationEmail(HttpServerRequest request, UserInfos infos, JsonObject emailState) {
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

        return emailSvc.sendValidationMessage( request, DataStateUtils.getPending(emailState), templateParams, "VALIDATION")
        .map( id -> {
            // Code was sent => this is a metric to follow
            DataValidationMetricsFactory.getRecorder().onEmailCodeGenerated();
            return id;
        });
    }

    @Override
    public Future<String> sendUpdateEmailWarning(HttpServerRequest request, UserInfos userInfos, JsonObject emailState) {
        JsonObject templateParams = new JsonObject()
                .put("scheme", Renders.getScheme(request))
                .put("host", Renders.getHost(request))
                .put("firstName", userInfos.getFirstName())
                .put("lastName", userInfos.getLastName())
                .put("date", LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) * 1000)
                .put("newEmail", DataStateUtils.getValid(emailState));

        Map<String, String> targets = new HashMap<>();
        targets.put("email", userInfos.getEmail());

        return emailSvc.sendWarningMessage(request, targets, templateParams)
                .map(id -> id);
    }

}
