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

package org.entcore.auth.users;

import org.entcore.auth.pojo.SendPasswordDestination;
import fr.wseduc.webutils.Either;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.webutils.Either;

public interface UserAuthAccount {

	void activateAccount(String login, String activationCode, String password, String email,
						 String phone, String theme, HttpServerRequest request, Handler<Either<String, String>> handler);

	void activateAccountByLoginAlias(String login, String activationCode, String password, String email,
						 String phone, String theme, HttpServerRequest request, Handler<Either<String, String>> handler);

	void resetPassword(String login, String resetCode, String password, Handler<Boolean> handler);

	void changePassword(String login, String password, Handler<Boolean> handler);

	void sendResetCode(HttpServerRequest request, String login, SendPasswordDestination dest,boolean checkFederatedLogin , Handler<Boolean> handler);

	void blockUser(String id, boolean block, Handler<Boolean> handler);

	void matchActivationCode(String login, String potentialActivationCode,
			Handler<Boolean> handler);

	void matchActivationCodeByLoginAlias(String loginAlias, String potentialActivationCode,
			 Handler<Boolean> handler);

	void matchResetCode(String login, String potentialResetCode,
			Handler<Boolean> handler);

	void matchResetCodeByLoginAlias(String loginAlias, String potentialResetCode,
			Handler<Boolean> handler);

	void findByMail(String email, Handler<Either<String, JsonObject>> handler);

	void findByMailAndFirstNameAndStructure(final String email, String firstName, String structure, final Handler<Either<String,JsonArray>> handler);

	void findByLogin(String login, String resetCode,boolean checkFederatedLogin, Handler<Either<String, JsonObject>> handler);

	void sendResetPasswordMail(HttpServerRequest request, String email,
			String resetCode, Handler<Either<String, JsonObject>> handler);

	void sendForgottenIdMail(HttpServerRequest request, String login,
			String email, Handler<Either<String, JsonObject>> handler);

	void sendResetPasswordSms(HttpServerRequest request, String phone,
			String resetCode, Handler<Either<String, JsonObject>> handler);

	void sendForgottenIdSms(HttpServerRequest request, String login,
			String phone, Handler<Either<String, JsonObject>> handler);

	void storeDomain(String id, String domain, String scheme, Handler<Boolean> handler);

	void getUserIdByLoginAlias(String username, String password, Handler<String> handler);
}
