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

package org.entcore.directory.security;

import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Map;

import static org.entcore.common.user.DefaultFunctions.SUPER_ADMIN;

public class RelativeStudentFilter implements ResourcesProvider {

	@Override
	public void authorize(final HttpServerRequest request, Binding binding, UserInfos user,
			final Handler<Boolean> handler) {
		Map<String, UserInfos.Function> functions = user.getFunctions();
		if (functions == null || functions.isEmpty()) {
			handler.handle(false);
			return;
		}
		if (functions.containsKey(SUPER_ADMIN)) {
			handler.handle(true);
			return;
		}
		final UserInfos.Function adminLocal = functions.get(DefaultFunctions.ADMIN_LOCAL);
		if (adminLocal == null || adminLocal.getScope() == null) {
			handler.handle(false);
			return;
		}
		final String studentId = request.params().get("studentId");
		final String relativeId = request.params().get("relativeId");
		String query =
				"MATCH (s)<-[:DEPENDS]-(:Group)<-[:IN]-(:User { id : {id}}) " +
				"WHERE (s:Structure OR s:Class) AND s.id IN {scope} " +
				"RETURN count(*) > 0 as exists ";
		JsonArray scope = new fr.wseduc.webutils.collections.JsonArray(adminLocal.getScope());
		StatementsBuilder s = new StatementsBuilder()
				.add(query, new JsonObject()
						.put("id", studentId)
						.put("scope", scope)
				)
				.add(query, new JsonObject()
						.put("id", relativeId)
						.put("scope", scope)
				);
		request.pause();
		Neo4j.getInstance().executeTransaction(s.build(), null, true, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				request.resume();
				JsonArray res = r.body().getJsonArray("results");
				handler.handle(
						"ok".equals(r.body().getString("status")) &&
								res.size() == 2 &&
								res.getJsonArray(0).getJsonObject(0).getBoolean("exists", false) &&
								res.getJsonArray(1).getJsonObject(0).getBoolean("exists", false)
				);
			}
		});
	}

}
