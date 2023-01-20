package org.entcore.common.datavalidation;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import org.entcore.common.datavalidation.utils.UserValidationFactory;

public class UserValidation {

	/**
	 * Get the current user MFA status.
	 * @return truthy when the user should perform a MFA to access protected zones.
	 */
	static public Future<Boolean> getMFA(final EventBus unused, final JsonObject session) {
		return UserValidationFactory.getInstance().getMFA(session);
	}

	/**
	 * Set the current user MFA status.
	 * @param status the new status
	 * @return an async future.
	 */
	static public Future<Boolean> setMFA(final EventBus unused, final JsonObject session, final boolean status) {
		return UserValidationFactory.getInstance().setMFA(session, status);
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
