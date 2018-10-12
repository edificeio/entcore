/*
 * Copyright © "Open Digital Education", 2014
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

package org.entcore.registry.filters;

import fr.wseduc.webutils.http.Binding;

import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Map;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;

public class LinkRoleGroupFilter implements ResourcesProvider {

	private final Neo4j neo4j = Neo4j.getInstance();

	@Override
	public void authorize(final HttpServerRequest resourceRequest, Binding binding, UserInfos user,
						  final Handler<Boolean> handler) {
		Map<String, UserInfos.Function> functions = user.getFunctions();

		if (functions == null || functions.isEmpty()) {
			handler.handle(false);
			return;
		}
		if (functions.containsKey(DefaultFunctions.SUPER_ADMIN)) {
			handler.handle(true);
			return;
		}
		final UserInfos.Function adminLocal = functions.get(DefaultFunctions.ADMIN_LOCAL);
		if (adminLocal == null || adminLocal.getScope() == null) {
			handler.handle(false);
			return;
		}

		bodyToJson(resourceRequest, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				final JsonArray roleIds = body.getJsonArray("roleIds");
				final String groupId = body.getString("groupId");
				JsonObject params = new JsonObject();
				params.put("structures", new fr.wseduc.webutils.collections.JsonArray(adminLocal.getScope()));
				if (roleIds != null && groupId != null &&
						roleIds.size() > 0 && !groupId.trim().isEmpty()) {
					String query =
							"MATCH (s:Structure)<-[:BELONGS*0..1]-()<-[:DEPENDS]-(:Group {id : {groupId}}), (r:Role) " +
							"WHERE s.id IN {structures} AND r.id IN {roles} AND (NOT(HAS(r.structureId)) OR r.structureId IN {structures}) " +
							"RETURN count(distinct r) = {nb} as exists ";
					params.put("groupId", groupId);
					params.put("roles", roleIds);
					params.put("nb", roleIds.size());
					check(resourceRequest, query, params, handler);
				} else {
					handler.handle(false);
				}
			}
		});
	}

	private void check(final HttpServerRequest request, String query, JsonObject params, final Handler<Boolean> handler) {
		request.pause();
		neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				request.resume();
				JsonArray r = event.body().getJsonArray("result");
				handler.handle(
						"ok".equals(event.body().getString("status")) &&
								r != null && r.size() == 1 &&
								r.getJsonObject(0).getBoolean("exists", false)
				);
			}
		});
	}

}
