package org.entcore.common.datavalidation;

import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.datavalidation.utils.UserValidationFactory;
import org.entcore.common.user.UserInfos;

public class MobileValidation {
	/**
	 * Start a new mobile phone number validation workflow.
	 * @param userId user ID
	 * @param mobile the mobile phone number to be checked
	 * @return the new mobileState
	 */
    static public Future<JsonObject> setPending(final EventBus unused, String userId, String mobile) {
		return UserValidationFactory.getInstance().setPendingMobile(userId, mobile);
    }

	/**
	 * Check if a user has a verified mobile phone number
	 * @param userId user ID
	 * @return { state: "unchecked"|"pending"|"outdated"|"valid", valid: latest known valid mobile phone number }
	 */
    static public Future<JsonObject> isValid(final EventBus unused, String userId) {
		return UserValidationFactory.getInstance().hasValidMobile(userId);
    }

	/**
	 * Verify a pending mobile phone number, by checking a code.
	 * @param sessionId session ID. If not null, and validation is successful, session will be granted the MFA flag.
	 * @param userId user ID
	 * @param code validation code to check
	 * @return { 
	 * 	state: "unchecked"|"pending"|"outdated"|"valid", 
	 * 	tries?: number of remaining retries,
	 *  ttl: number of seconds remaining before expiration of the code
	 * }
	 */
    static public Future<JsonObject> tryValidate(final EventBus unused, String sessionId, String userId, String code) {
		return UserValidationFactory.getInstance().tryValidateMobile(sessionId, userId, code);
    }

	/**
	 * Get current validation details.
	 * @param userId user ID
	 * @return {mobile:string, mobileState:object|null, waitInSeconds:number}
	 */
    static public Future<JsonObject> getDetails(final EventBus unused, String userId) {
		return UserValidationFactory.getInstance().getMobileState(userId);
    }

	/** 
	 * Send an SMS with actual validation code.
	 * @param infos User infos
	 * @param mobile phone number where to send
	 * @param pendingMobileState with code to send
	 * @return mobile ID
	*/
	static public Future<String> sendSMS(final EventBus unused, final HttpServerRequest request, UserInfos infos, JsonObject pendingMobileState) {
        return UserValidationFactory.getInstance().sendValidationSMS(
			request, 
			infos, 
			pendingMobileState
		);
	}

	/**
	 * Send a warning email & SMS to the user when the mobile phone number has been modified.
	 * @param userInfos User infos
	 * @param pendingMobileState with new mobile phone number
	 * @return mobile ID
	 */
	static public Future<String> sendWarning(final HttpServerRequest request, UserInfos userInfos, JsonObject pendingMobileState) {
		return UserValidationFactory.getInstance().sendUpdateMobileWarning(
				request,
				userInfos,
				pendingMobileState
		);
	}
}
