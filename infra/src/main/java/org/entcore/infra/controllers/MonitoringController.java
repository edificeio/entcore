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

package org.entcore.infra.controllers;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.shareddata.LocalMap;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sql.Sql;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.vertx.java.core.http.RouteMatcher;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class MonitoringController extends BaseController {

	private boolean postgresql;
	private long dbCheckTimeout;

	@Override
	public void init(Vertx vertx, JsonObject config, RouteMatcher rm, Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);
		dbCheckTimeout = config.getLong("dbCheckTimeout", 5000l);
		postgresql = config.getBoolean("sql", true);
	}

	@Get("/monitoring/db")
	public void checkDb(final HttpServerRequest request) {
		final JsonObject result = new JsonObject();
		final AtomicBoolean closed = new AtomicBoolean(false);
		final long timerId = vertx.setTimer(dbCheckTimeout, new Handler<Long>() {
			@Override
			public void handle(Long event) {
				closed.set(true);
				result.put("status", "timeout");
				renderError(request, result);
			}
		});
		final AtomicInteger count = new AtomicInteger(3);
		if (postgresql) {
			Sql.getInstance().raw("SELECT count(*) FROM information_schema.tables",
					getResponseHandler("postgresql", timerId,  result, count, request, closed));
		} else {
			count.decrementAndGet();
		}
		Neo4j.getInstance().execute("MATCH (:Structure) RETURN count(*)", (JsonObject) null,
				getResponseHandler("neo4j", timerId,  result, count, request, closed));
		MongoDb.getInstance().command("{ dbStats: 1 }",
				getResponseHandler("mongodb", timerId,  result, count, request, closed));
	}

	@Get("/monitoring/versions")
	@SecuredAction(value = "",  type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void checkVersions(final HttpServerRequest request) {
		final JsonArray versions = new fr.wseduc.webutils.collections.JsonArray();
		LocalMap<String, String> versionMap = vertx.sharedData().getLocalMap("versions");
		for (Map.Entry<String,String> entry : versionMap.entrySet()) {
			versions.add(new JsonObject().put(entry.getKey(), entry.getValue()));
		}
		Renders.renderJson(request, versions);
	}

	private Handler<Message<JsonObject>> getResponseHandler(final String module, final long timerId,
			final JsonObject result, final AtomicInteger count, final HttpServerRequest request, final AtomicBoolean closed) {
		return new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				result.put(module, event.body().getString("status"));
				if (count.decrementAndGet() <= 0 && !closed.get()) {
					vertx.cancelTimer(timerId);
					boolean error = false;
					for (String element : result.fieldNames()) {
						if (!"ok".equals(result.getString(element))) {
							error = true;
							break;
						}
					}
					if (error) {
						renderError(request, result);
					} else {
						renderJson(request, result);
					}
				}
			}
		};
	}

}
