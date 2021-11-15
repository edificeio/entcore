/*
 * Copyright © "Open Digital Education", 2016
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

 */

package org.entcore.auth.services;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.http.HttpServerRequest;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.security.BCrypt;
import fr.wseduc.webutils.security.Md5;
import fr.wseduc.webutils.security.Sha256;
import static fr.wseduc.webutils.Utils.isNotEmpty;

import java.security.NoSuchAlgorithmException;

public interface OpenIdConnectServiceProvider {

	public static final String UNRECOGNIZED_USER_IDENTITY = "unrecognized.user.identity";
	public static final String DEFAULT_SCOPE_OPENID = "openid profile";

	void executeFederate(JsonObject payload, Handler<Either<String, Object>> handler);

	void mappingUser(String login, String password, JsonObject payload, Handler<Either<String, Object>> handler);

	void setSetFederated(boolean setFederated);

	default boolean isPayloadValid(JsonObject payload, String issuer)
	{
		return issuer.equals(payload.getString("iss"))
				&& payload.getLong("exp", 0l) > (System.currentTimeMillis() / 1000)
				&& isNotEmpty(payload.getString("sub"));
	}

	default boolean checkPassword(String password, String neo4jPassword, String neo4jActivationCode) throws NoSuchAlgorithmException
	{
		boolean success = password.equals(neo4jActivationCode);
		if (!success && isNotEmpty(neo4jPassword)) {
			switch (neo4jPassword.length()) {
				case 32: // md5
					success = neo4jPassword.equals(Md5.hash(password));
					break;
				case 64: // sha-256
					success = neo4jPassword.equals(Sha256.hash(password));
					break;
				default: // BCrypt
					success = BCrypt.checkpw(password, neo4jPassword);
			}
		}
		return success;
	}

	default void webhook(HttpServerRequest request)
	{
		request.response().setStatusCode(501).setStatusMessage("Not Implemented").end();
	}

	default String getScope()
	{
		return DEFAULT_SCOPE_OPENID;
	}

}
