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
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.IgnoreCsrf;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sql.Sql;
import org.entcore.infra.services.CspReportService;
import org.entcore.infra.services.impl.MongoDbCspReportService;
import org.vertx.java.core.http.RouteMatcher;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;


public class MonitoringController extends BaseController {
	private boolean postgresql;
	private long dbCheckTimeout;
	private boolean enableNeo4jMetrics;
	private CspReportService cspReportSvc; 

	@Override
	public void init(Vertx vertx, JsonObject config, RouteMatcher rm, Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);
		dbCheckTimeout = config.getLong("dbCheckTimeout", 5000l);
		postgresql = config.getBoolean("sql", true);
		enableNeo4jMetrics = config.getBoolean("neo4jMetricsEnable", false);
		cspReportSvc = new MongoDbCspReportService();
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
		MongoDb.getInstance().command("{ \"dbStats\": 1 }",
				getResponseHandler("mongodb", timerId,  result, count, request, closed));
	}

	@Get("/monitoring/db/neo4j/metrics")
	public void checkDbNeo4j(final HttpServerRequest request) {
		if(enableNeo4jMetrics){
			final JsonObject metrics = Neo4j.getInstance().getMetrics();
			final StringBuilder text = new StringBuilder();
			for(final String key : metrics.fieldNames()){
				text.append(key).append(" ").append(metrics.getValue(key).toString()).append("\n");
			}
			request.response().putHeader("content-type", "text/plain");
			request.response().putHeader("Cache-Control", "no-cache, must-revalidate");
			request.response().putHeader("Expires", "-1");
			request.response().end(text.toString());
		}else{
			notFound(request);
		}
	}

	@Get("/monitoring/versions/all")
	@SecuredAction(value = "",  type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void checkVersionsAll(final HttpServerRequest request) {
		final JsonArray versions = new JsonArray();
		LocalMap<String, JsonObject> versionMap = vertx.sharedData().getLocalMap("modsInfoMap");
		for (Map.Entry<String,JsonObject> entry : versionMap.entrySet()) {
			versions.add(new JsonObject().put(entry.getKey(), entry.getValue()));
		}
		Renders.renderJson(request, versions);
	}

	@Get("/monitoring/versions")
	@SecuredAction(value = "",  type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void checkVersions(final HttpServerRequest request) {
		final JsonArray versions = new JsonArray();
		LocalMap<String, String> versionMap = vertx.sharedData().getLocalMap("versions");
		for (Map.Entry<String,String> entry : versionMap.entrySet()) {
			versions.add(new JsonObject().put(entry.getKey(), entry.getValue()));
		}
		Renders.renderJson(request, versions);
	}

	@Get("/monitoring/detailedVersions")
	@SecuredAction(value = "",  type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void checkDetailedVersions(final HttpServerRequest request) {
		final JsonArray versions = new JsonArray();
		LocalMap<String, JsonObject> versionMap = vertx.sharedData().getLocalMap("detailedVersions");
		for (Map.Entry<String,JsonObject> entry : versionMap.entrySet()) {
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

	/**
	 * This endpoint receives CSP reports objects.
	 * See <a href="https://www.w3.org/TR/CSP2/#violation-reports">...</a>
	 */
	@Post("/monitoring/csp")
	@SecuredAction(type = ActionType.AUTHENTICATED, value = "")
	@IgnoreCsrf
	public void cspReport(final HttpServerRequest request) {
		bodyToJson(request, pathPrefix + "cspReport", body -> {
			final JsonObject report = body.getJsonObject("csp-report");
			cspReportSvc.store(report);
			Renders.ok(request);
		});
	}

}
