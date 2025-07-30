/*
 * Copyright © "Open Digital Education", 2018
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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.common.http.filter;

import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.broker.api.dto.communication.CheckCommunicationExistsResponseDTO;
import org.entcore.common.migration.AppMigrationConfiguration;
import org.entcore.common.migration.BrokerSwitchConfiguration;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.StringUtils;

public class VisibleFilter implements ResourcesProvider {
	private static final Logger log = LoggerFactory.getLogger(VisibleFilter.class);

	private final Neo4j neo4j = Neo4j.getInstance();
	private AppMigrationConfiguration appMigrationConfiguration;
	private static final String query =
			"MATCH p=(n:User {id: {userId}})<-[:COMMUNIQUE*0..2]-t<-[r:COMMUNIQUE|COMMUNIQUE_DIRECT]-(m:User {id: {queryUserId}}) " +
			"WHERE ((type(r) = 'COMMUNIQUE_DIRECT' AND length(p) = 1) XOR (type(r) = 'COMMUNIQUE' AND length(p) >= 2)) " +
			"RETURN count(distinct n) = 1 as exists";

	@Override
	public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
		final String userId = request.params().get("userId");
		if (userId == null || userId.trim().isEmpty()) {
			handler.handle(false);
			return;
		}
		final JsonObject payload = new JsonObject().put("queryUserId", user.getUserId()).put("userId", userId);
		if(isReadAvailable()) {
			switchAuthorization(payload).onSuccess(handler::handle).onFailure(th -> {
				log.warn("An error occurred while authorizing a call : " + payload.encode(), th);
				handler.handle(false);
			});
		} else {
			neo4j.execute(query, payload, event -> {
				final JsonArray res = event.body().getJsonArray("result");
				handler.handle("ok".equals(event.body().getString("status")) && res != null && res.size() == 1 &&
					res.getJsonObject(0).getBoolean("exists", false));
			});
		}
	}

	private Future<Boolean> switchAuthorization(final JsonObject params) {
		final Promise<Boolean> promise = Promise.promise();
		final JsonObject payload = new JsonObject()
			.put("action", "visibleFilter")
			.put("service", "referential")
			.put("params", params);

		Vertx.currentContext().owner().eventBus().request(BrokerSwitchConfiguration.LEGACY_MIGRATION_ADDRESS, payload)
			.onSuccess(response -> promise.complete(StringUtils.parseJson((String) response.body(), CheckCommunicationExistsResponseDTO.class).isExists()))
			.onFailure(promise::fail);
		return promise.future();
	}

	private boolean isReadAvailable() {
		final AppMigrationConfiguration appMigration = getAppMigration();
		return appMigration.isEnabled() && appMigration.getAvailableReadActions().contains("visibleFilter");
	}


	private AppMigrationConfiguration getAppMigration() {
		if (appMigrationConfiguration == null) {
			appMigrationConfiguration = AppMigrationConfiguration.fromVertx("referential");
		}
		return appMigrationConfiguration;
	}

}
