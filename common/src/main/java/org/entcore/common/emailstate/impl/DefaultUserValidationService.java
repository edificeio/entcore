package org.entcore.common.emailstate.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.common.emailstate.EmailState.FIELD_MUST_CHANGE_PWD;
import static org.entcore.common.emailstate.EmailState.FIELD_MUST_VALIDATE_TERMS;
import static org.entcore.common.emailstate.EmailState.FIELD_MUST_VALIDATE_EMAIL;
import static org.entcore.common.user.SessionAttributes.*;
import static org.entcore.common.neo4j.Neo4jResult.*;

import java.util.Map;

import org.entcore.common.emailstate.EmailState;
import org.entcore.common.emailstate.EmailStateUtils;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.emailstate.EmailValidationService;
import org.entcore.common.emailstate.UserValidationService;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;

/**
 * @see {@link EmailState} utility class for easier use
 * Embraces and extends EmailValidationService
 */
public class DefaultUserValidationService implements UserValidationService {
	private final Neo4j neo = Neo4j.getInstance();
    private EmailValidationService validationSvc = null;
    private int ttlInSeconds     = 600;  // Validation codes are valid 10 minutes by default
    private int retryNumber      = 5;    // Validation code can be typed in 5 times by default
    private int waitInSeconds    = 10;   // Email is awaited 10 seconds by default (it's a front-side parameter)

    public DefaultUserValidationService(final JsonObject params, final EmailValidationService svc) {
        if( params != null ) {
            ttlInSeconds    = params.getInteger("ttlInSeconds", 600);
            retryNumber     = params.getInteger("retryNumber",  5);
            waitInSeconds   = params.getInteger("waitInSeconds", 10);
        }
        validationSvc = svc;
    }

	/** 
	 * @return {
	 * 	forceChangePassword: true|false
	 * }
	 */
	private Future<JsonObject> retrieveNeedChangePassword(String userId) {
		final Promise<JsonObject> promise = Promise.promise();
		String query =
				"MATCH (u:`User` { id : {id}}) " +
				"RETURN COALESCE(u.changePw, FALSE) as forceChangePassword ";
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
    public Future<JsonObject> getMandatoryUserValidation(final JsonObject session, final boolean forced) {
        Promise<JsonObject> promise = Promise.promise();

        final UserInfos userInfos = UserUtils.sessionToUserInfos( session );
        final JsonObject required = new JsonObject()
            .put(FIELD_MUST_CHANGE_PWD, getOrElse(session.getBoolean("forceChangePassword"), false))
            .put(FIELD_MUST_VALIDATE_EMAIL, false)
            .put(FIELD_MUST_VALIDATE_TERMS, false);

        if (userInfos == null) {
            // Disconnected user => nothing to validate
            //---
            promise.complete( required );
        } else {
            Future<JsonObject> checkedValidations = Future.succeededFuture(required);

            // force change password ?
            //---
            if( session != null ) {
                // 2023-01-11 Temporary dirty fix : 
                // forceChangePassword is retrieved from the session (see above), but may be cached and outdated.
                // Until session cache management is improved, when forced, read the value from the DB directly.
                checkedValidations = forced 
                    ? retrieveNeedChangePassword(userInfos.getUserId())
                    : Future.succeededFuture(session);
            }

            checkedValidations.onComplete( checkPw -> {
                if( checkPw.succeeded() ) {
                    JsonObject pw = checkPw.result();
                    required.put(FIELD_MUST_CHANGE_PWD, getOrElse(pw.getBoolean("forceChangePassword"), false));
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
                    hasValidEmail(userInfos.getUserId())
                    .onSuccess( emailState -> {
                        if( ! "valid".equals(emailState.getString("state")) ) {
                            required.put(FIELD_MUST_VALIDATE_EMAIL, true);
                        }
                        promise.complete( required );
                    })
                    .onFailure( e -> {promise.complete(required);} );
                }
            });

        }
		return promise.future();
    }

	@Override
    public Future<JsonObject> hasValidEmail(String userId) {
        return validationSvc.hasValidEmail(userId);
    }

    @Override
	public Future<JsonObject> setPendingEmail(String userId, String email) {
        return validationSvc.setPendingEmail(userId, email, ttlInSeconds, retryNumber);
    }

    @Override
	public Future<JsonObject> tryValidateEmail(String userId, String code) {
        return validationSvc.tryValidateEmail(userId, code);
    }

    @Override
	public Future<JsonObject> getEmailState(String userId) {
        return validationSvc.getEmailState(userId)
        .map( t -> {
            // Add missing data
            t.put("waitInSeconds", waitInSeconds);
            return t;
        });
    }

    @Override
	public Future<Long> sendValidationEmail(HttpServerRequest request, UserInfos infos, JsonObject emailState) {
        final Long expires = getOrElse(EmailStateUtils.getTtl(emailState), waitInSeconds*1000l);

        JsonObject templateParams = new JsonObject()
        .put("scheme", Renders.getScheme(request))
        .put("host", Renders.getHost(request))
        .put("userId", infos.getUserId())
        .put("firstName", infos.getFirstName())
        .put("lastName", infos.getLastName())
        .put("userName", infos.getUsername())
        .put("duration", Math.round(EmailStateUtils.ttlToRemainingSeconds(expires) / 60f))
        .put("code", EmailStateUtils.getKey(emailState));

        return validationSvc.sendValidationEmail( request, EmailStateUtils.getPending(emailState), templateParams );
    }
}
