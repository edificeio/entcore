/*
 * Copyright © "Open Digital Education", 2015
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

package org.entcore.directory.security;

import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;

public class AddFunctionFilter extends AdmlOfUser {

	@Override
	protected void additionnalsChecks(HttpServerRequest resourceRequest,
			Binding binding, UserInfos user, UserInfos.Function adminLocal, final Handler<Boolean> handler) {
		bodyToJson(resourceRequest, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject event) {
				String function = event.getString("functionCode", "").trim();
				if(!function.isEmpty() && !"SUPER_ADMIN".equals(function)) {
					checkScope(event, adminLocal)
							.onSuccess(handler)
							.onFailure(th -> handler.handle(false));
				} else {
					handler.handle(false);
				}
			}
		});
		resourceRequest.resume();
	}

	/**
	 * Method checking that current user is central admin, or local admin on requested structures scope
	 * @param requestBody the request body
	 * @param adminUserFunction the function of the admin user currently granting rights
	 * @return True if
	 *   - current user granting requested rights is central admin, or
	 *   - the requested structures scope matches the local admin structures scope
	 * False otherwise
	 */
	private Future<Boolean> checkScope(JsonObject requestBody, UserInfos.Function adminUserFunction) {
		final Promise<Boolean> promise = Promise.promise();
		if ("SUPER_ADMIN".equals(adminUserFunction.getCode())) {
			promise.complete(true);
		} else {
			promise.complete(requestBody.getJsonArray("scope").stream()
					.allMatch(requestedStructure -> adminUserFunction.getScope().contains((String)requestedStructure)));
		}
		return promise.future();
	}

}
