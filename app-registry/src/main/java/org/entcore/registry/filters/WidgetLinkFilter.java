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

package org.entcore.registry.filters;

import java.util.Map;

import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.webutils.http.Binding;

public class WidgetLinkFilter implements ResourcesProvider{

	private final Neo4j neo4j = Neo4j.getInstance();

	@Override
	public void authorize(HttpServerRequest request, Binding binding, UserInfos user, final Handler<Boolean> handler) {

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

		final String groupId = request.params().get("groupId");
		if(groupId == null || groupId.trim().isEmpty()){
			handler.handle(false);
			return;
		}

		String query =
			"MATCH (s:Structure)<-[:BELONGS*0..1]-()<-[:DEPENDS]-(g:Group {id : {groupId}}) "+
			"WHERE s.id IN {adminScope} " +
			"RETURN count(g) = 1 as exists";
		JsonObject params = new JsonObject()
			.put("groupId", groupId)
			.put("adminScope", new fr.wseduc.webutils.collections.JsonArray(adminLocal.getScope()));

		neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> event) {
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
