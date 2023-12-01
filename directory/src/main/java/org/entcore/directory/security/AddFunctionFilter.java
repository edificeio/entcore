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
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
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
					checkScope(event, user, adminLocal, resourceRequest)
							.onSuccess(handler)
							.onFailure(th -> handler.handle(false));
				} else {
					handler.handle(false);
				}
			}
		});
		resourceRequest.resume();
	}

	private Future<Boolean> checkScope(JsonObject requestBody, UserInfos user, UserInfos.Function adminLocal, HttpServerRequest request) {
		final Promise<Boolean> promise = Promise.promise();
		final JsonArray scope = requestBody.getJsonArray("scope");
		final JsonArray filteredStructures = new JsonArray();
		scope.stream()
				.filter(s -> adminLocal.getScope().contains((String)s))
				.forEach(filteredStructures::add);
		final String query =
				"MATCH (u:User {id: {userId}})-[:IN]->(:Group)-[:DEPENDS]->(s:Structure) " +
						"WHERE s.id IN {structures} " +
						"RETURN count(s) == {nbStructures}";
		final JsonObject params = new JsonObject()
				.put("structures", filteredStructures)
				.put("nbStructures", scope.size())
				.put("userId", user.getUserId());
		request.pause();
		validateQuery(request, e -> {

		}, query, params);
		neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				request.resume();
				JsonArray res = r.body().getJsonArray("result");
				handler.handle(
						"ok".equals(r.body().getString("status")) &&
								res.size() == 1 && (res.getJsonObject(0)).getBoolean("exists", false)
				);
			}
		});
		return promise.future();
	}

}
