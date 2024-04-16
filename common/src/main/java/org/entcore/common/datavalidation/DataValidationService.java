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

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public interface DataValidationService {

	/**
	 * Retrieve the data valid state.
	 * @param userId user ID
	 * @return { state: "unchecked"|"pending"|"outdated"|"valid", valid: latest known valid data }
	 */
	Future<JsonObject> hasValid(String userId);

	/**
	 * Start a data validation workflow.
	 * @param userId user ID
	 * @param data the value to be checked
	 * @return the new state
	 */
	Future<JsonObject> startUpdate(String userId, String data, final long validDurationS, final int triesLimit);

	/**
	 * Try to validate a data in pending state, by checking a code.
	 * @param userId user ID
	 * @param code validation code to check
	 * @return a dataState like { 
	 * 	state: "unchecked"|"pending"|"outdated"|"valid", 
	 *  valid: latest known valid email address or mobile phone number,
	 * 	tries?: number of remaining retries,
	 *  ttl?: number of seconds remaining before expiration of the code
	 * }
	 */
	Future<JsonObject> tryValidate(String userId, String code);

	/**
	 * Retrieve the current validation state.
	 * @param userId user ID
	 * @return {value:string, state:object|null}
	 */
	Future<JsonObject> getCurrentState(String userId);

	/**
	 * Send the validation message (email or sms).
	 * @param request required to translate things...
	 * @param target address where to send
	 * @param templateParams for the "email/emailValidationCode.html" template
	 * @param module name of the module which is sending a SMS for validation
	 * @return the message ID
	 */
	Future<String> sendValidationMessage(HttpServerRequest request, String target, JsonObject templateParams, String module);

	/**
	 * Send the warning message (email or sms or both).
	 * @param request required to translate things...
	 * @param target address where to send
	 * @param templateParams email template data
	 * @return the message ID
	 */
	Future<String> sendWarningMessage(HttpServerRequest request, String target, JsonObject templateParams);
}
