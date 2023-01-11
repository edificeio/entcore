package org.entcore.common.emailstate;

import org.entcore.common.emailstate.EmailStateFactory;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

import org.entcore.common.user.UserInfos;

public class EmailState {
	static public String FIELD_MUST_CHANGE_PWD     = "forceChangePassword";
	static public String FIELD_MUST_VALIDATE_TERMS = "needRevalidateTerms";
	static public String FIELD_MUST_VALIDATE_EMAIL = "needRevalidateEmail";

	/** 
	 * Check if the user has to fulfill some mandatory actions, such as :
	 * - re/validate terms of use,
	 * - validating his email address,
	 * - change his passsword.
	 * 
	 * @param session
	 * @return {forceChangePassword: boolean, needRevalidateTerms: boolean, needRevalidateEmail: boolean}
	*/
	static public Future<JsonObject> getMandatoryUserValidation(final EventBus unused, final JsonObject session) {
		return getMandatoryUserValidation(unused, session, false);
	}

	/** 
	 * Check if the user has to fulfill some mandatory actions, such as :
	 * - re/validate terms of use,
	 * - validating his email address,
	 * - change his passsword.
	 * 
	 * @param session
	 * @param force When truthy, read fresh data from the DB instead of the session. WARNING: performance loss.
	 * @return {forceChangePassword: boolean, needRevalidateTerms: boolean, needRevalidateEmail: boolean}
	*/
	static public Future<JsonObject> getMandatoryUserValidation(final EventBus unused, final JsonObject session, final boolean forced) {
		return EmailStateFactory.getInstance().getMandatoryUserValidation(session, forced);
	}

	/**
	 * Start a new email validation workflow.
	 * @param userId user ID
	 * @param email the mail address to be checked
	 * @return the new emailState
	 */
    static public Future<JsonObject> setPending(final EventBus unused, String userId, String email) {
		return EmailStateFactory.getInstance().setPendingEmail(userId, email);
    }

	/**
	 * Check if a user has a verified email address
	 * @param userId user ID
	 * @return { state: "unchecked"|"pending"|"outdated"|"valid", valid: latest known valid email address }
	 */
    static public Future<JsonObject> isValid(final EventBus unused, String userId) {
		return EmailStateFactory.getInstance().hasValidEmail(userId);
    }

	/**
	 * Verify a pending email address of a user, by checking a code.
	 * @param userId user ID
	 * @param code validation code to check
	 * @return { 
	 * 	state: "unchecked"|"pending"|"outdated"|"valid", 
	 * 	tries?: number of remaining retries,
	 *  ttl: number of seconds remaining before expiration of the code
	 * }
	 */
    static public Future<JsonObject> tryValidate(final EventBus unused, String userId, String code) {
		return EmailStateFactory.getInstance().tryValidateEmail(userId, code);
    }

	/**
	 * Get current email validation details.
	 * @param userId user ID
	 * @return {email:string, emailState:object|null, waitInSeconds:number}
	 */
    static public Future<JsonObject> getDetails(final EventBus unused, String userId) {
		return EmailStateFactory.getInstance().getEmailState(userId);
    }

	/** 
	 * Send an email with actual validation code.
	 * @param infos User infos
	 * @param email address where to send
	 * @param pendingEmailState with code to send
	 * @return email ID
	*/
	static public Future<Long> sendEmail(final EventBus unused, final HttpServerRequest request, UserInfos infos, JsonObject pendingEmailState) {
        return EmailStateFactory.getInstance().sendValidationEmail(
			request, 
			infos, 
			pendingEmailState
		);
	}
}
