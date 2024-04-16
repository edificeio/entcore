package org.entcore.common.datavalidation;

import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.datavalidation.utils.UserValidationFactory;
import org.entcore.common.user.UserInfos;

public class EmailValidation {
	/**
	 * Start a new email validation workflow.
	 * @param userId user ID
	 * @param email the mail address to be checked
	 * @return the new emailState
	 */
    static public Future<JsonObject> setPending(final EventBus unused, String userId, String email) {
		return UserValidationFactory.getInstance().setPendingEmail(userId, email);
    }

	/**
	 * Check if a user has a verified email address
	 * @param userId user ID
	 * @return { state: "unchecked"|"pending"|"outdated"|"valid", valid: latest known valid email address }
	 */
    static public Future<JsonObject> isValid(final EventBus unused, String userId) {
		return UserValidationFactory.getInstance().hasValidEmail(userId);
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
		return UserValidationFactory.getInstance().tryValidateEmail(userId, code);
    }

	/**
	 * Get current email validation details.
	 * @param userId user ID
	 * @return {email:string, emailState:object|null, waitInSeconds:number}
	 */
    static public Future<JsonObject> getDetails(final EventBus unused, String userId) {
		return UserValidationFactory.getInstance().getEmailState(userId);
    }

	/** 
	 * Send an email with actual validation code.
	 * @param infos User infos
	 * @param email address where to send
	 * @param pendingEmailState with code to send
	 * @return email ID
	*/
	static public Future<String> sendEmail(final EventBus unused, final HttpServerRequest request, UserInfos infos, JsonObject pendingEmailState) {
        return UserValidationFactory.getInstance().sendValidationEmail(
			request, 
			infos, 
			pendingEmailState
		);
	}

	/**
	 * Send a warning email to old address mail when it has been modified.
	 * @param userInfos User infos
	 * @param pendingEmailState with new email address
	 * @return email ID
	 */
	static public Future<String> sendWarningEmail(final HttpServerRequest request, UserInfos userInfos, JsonObject pendingEmailState) {
		return UserValidationFactory.getInstance().sendUpdateEmailWarning(
				request,
				userInfos,
				pendingEmailState
		);
	}
}
