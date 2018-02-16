/* Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
