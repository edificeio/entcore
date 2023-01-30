package org.entcore.common.datavalidation;

import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

import org.entcore.common.datavalidation.utils.UserValidationFactory;

public class UserValidation {

	/** Validation codes are valid these many seconds, by default. */
    static public int getDefaultTtlInSeconds() {
		return UserValidationFactory.getInstance().getDefaultTtlInSeconds();
	}

	/** Validation code can be tested these many times, by default. */
    static public int getDefaultRetryNumber() {
		return UserValidationFactory.getInstance().getDefaultRetryNumber();
	}

	/** Receipt of a code should be awaited these many seconds, by default. */
    static public int getDefaultWaitInSeconds() {
		return UserValidationFactory.getInstance().getDefaultWaitInSeconds();
	}

	/**
	 * Get the current user MFA status.
	 * @return truthy when the user is authentified with 2 factors.
	 */
	static public Boolean getIsMFA(final EventBus unused, final JsonObject session) {
		return UserValidationFactory.getInstance().getIsMFA(session);
	}

	/**
	 * Set the current user MFA status.
	 * @param status the new status
	 * @return a truthy async future when successful ?
	 */
	static public Future<Boolean> setIsMFA(final EventBus eb, final String sessionId, final boolean status) {
		return UserValidationFactory.getInstance().setIsMFA(eb, sessionId, status);
	}

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
		return UserValidationFactory.getInstance().getMandatoryUserValidation(session, forced);
	}
}
