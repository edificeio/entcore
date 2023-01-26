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

package org.entcore.common.datavalidation;

import org.entcore.common.user.UserInfos;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public interface UserValidationService {
	static public String FIELD_MUST_CHANGE_PWD     	= "forceChangePassword";
	static public String FIELD_MUST_VALIDATE_TERMS 	= "needRevalidateTerms";
	static public String FIELD_MUST_VALIDATE_EMAIL 	= "needRevalidateEmail";
	static public String FIELD_MUST_VALIDATE_MOBILE	= "needRevalidateMobile";
	static public String FIELD_NEED_MFA				= "needMfa";

	/**
	 * Get the current user MFA status.
	 * @return truthy when the user should perform a MFA to access protected zones.
	 */
	Future<Boolean> getMFA(final JsonObject session);

	/**
	 * Set the current user MFA status.
	 * @param status the new status
	 * @return an async future.
	 */
	Future<Boolean> setMFA(final JsonObject session, final boolean status);

	/** 
	 * Check if the user has to fulfill some mandatory actions, such as :
	 * - re/validate terms of use,
	 * - validating his email address,
	 * - change his passsword.
	 * 
	 * @param session
	 * @param forced When truthy, read fresh data from the DB instead of the session. WARNING: performance loss.
	 * @return { 
	 * 	forceChangePassword: boolean, 
	 *  needRevalidateTerms: boolean, 
	 *  needRevalidateEmail: boolean,
	 *  needRevalidateMobile: boolean
	 * }
	 */
	Future<JsonObject> getMandatoryUserValidation(final JsonObject session, final boolean forced);

	//////////////// Mobile-related methods ////////////////

	/**
	 * Check if a user has a verified mobile phone
	 * @param userId user ID
	 * @return { state: "unchecked"|"pending"|"outdated"|"valid", valid: latest known valid email address }
	 */
	Future<JsonObject> hasValidMobile(String userId);

	/**
	 * Start a new mail validation workflow.
	 * @param userId user ID
	 * @param mobile the mobile phone number to be checked
	 * @return the new mobileState
	 */
	Future<JsonObject> setPendingMobile(String userId, String mobile);

	/**
	 * Verify the pending mobile phone number of a user, by checking a code.
	 * @param userId user ID
	 * @param code validation code to check
	 * @return a mobileState like { 
	 * 	state: "unchecked"|"pending"|"outdated"|"valid", 
	 *  valid: latest known valid mobile phone number,
	 * 	tries?: number of remaining retries,
	 *  ttl?: number of seconds remaining before expiration of the code
	 * }
	 */
	Future<JsonObject> tryValidateMobile(String userId, String code);

	/**
	 * Get current mobile validation state.
	 * @param userId user ID
	 * @return {mobile:string, mobileState:object|null}
	 */
	Future<JsonObject> getMobileState(String userId);

	/**
	 * Send the SMS.
	 * @param request required to translate things...
	 * @param infos
	 * @param mobileState
	 * @return the SMS ID
	 */
	Future<Long> sendValidationSMS(HttpServerRequest request, UserInfos infos, JsonObject mobileState);

	
	//////////////// Email-related methods ////////////////

	/**
	 * Check if a user has a verified email address
	 * @param userId user ID
	 * @param force When truthy, read fresh data from the DB instead of the session. WARNING: performance loss.
	 * @return { state: "unchecked"|"pending"|"outdated"|"valid", valid: latest known valid email address }
	 */
	Future<JsonObject> hasValidEmail(String userId);

	/**
	 * Start a new mail validation workflow.
	 * @param userId user ID
	 * @param email the mail address to be checked
	 * @return the new emailState
	 */
	Future<JsonObject> setPendingEmail(String userId, String email);

	/**
	 * Verify a pending email address of a user, by checking a code.
	 * @param userId user ID
	 * @param code validation code to check
	 * @return an emailState like { 
	 * 	state: "unchecked"|"pending"|"outdated"|"valid", 
	 *  valid: latest known valid email address,
	 * 	tries?: number of remaining retries,
	 *  ttl?: number of seconds remaining before expiration of the code
	 * }
	 */
	Future<JsonObject> tryValidateEmail(String userId, String code);

	/**
	 * Get current mail validation state.
	 * @param userId user ID
	 * @return {email:string, emailState:object|null}
	 */
	Future<JsonObject> getEmailState(String userId);

	/**
	 * Send the validation email.
	 * @param request required for EmailSender to translate things...
	 * @param infos
	 * @param emailState
	 * @return the email ID
	 */
	Future<Long> sendValidationEmail(HttpServerRequest request, UserInfos infos, JsonObject emailState);
}
