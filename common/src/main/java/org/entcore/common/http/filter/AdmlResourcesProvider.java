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

package org.entcore.common.http.filter;

import fr.wseduc.webutils.http.Binding;
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

public abstract class AdmlResourcesProvider implements ResourcesProvider {

	private final Neo4j neo4j = Neo4j.getInstance();

	@Override
	public void authorize(HttpServerRequest resourceRequest, Binding binding, UserInfos user, Handler<Boolean> handler) {
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
		authorizeAdml(resourceRequest, binding, user, adminLocal, handler);
	}

	protected void validateQuery(final HttpServerRequest request, final Handler<Boolean> handler,
			String query, JsonObject params) {
		request.pause();
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
	}

	protected void validateQueries(final HttpServerRequest request, final Handler<Boolean> handler,
			StatementsBuilder statementsBuilder) {
		request.pause();
		final JsonArray statements = statementsBuilder.build();
		neo4j.executeTransaction(statements, null, true, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				request.resume();
				JsonArray res = r.body().getJsonArray("results");
				if (!"ok".equals(r.body().getString("status")) || res == null || res.size() != statements.size()) {
					handler.handle(false);
					return;
				}
				for (int i = 0; i < statements.size(); i++) {
					JsonArray j = res.getJsonArray(i);
					if (j.size() != 1 || !j.getJsonObject(0).getBoolean("exists", false)) {
						handler.handle(false);
						return;
					}
				}
				handler.handle(true);
			}
		});
	}

	public abstract void authorizeAdml(HttpServerRequest resourceRequest, Binding binding,
			UserInfos user, UserInfos.Function adminLocal, Handler<Boolean> handler);

}
