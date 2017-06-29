/*
 * Copyright © WebServices pour l'Éducation, 2015
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.infra.controllers;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sql.Sql;
import org.entcore.infra.Starter;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.shareddata.ConcurrentSharedMap;
import org.vertx.java.platform.Container;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class MonitoringController extends BaseController {

	private boolean postgresql;
	private long dbCheckTimeout;

	@Override
	public void init(Vertx vertx, Container container, RouteMatcher rm, Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, container, rm, securedActions);
		dbCheckTimeout = container.config().getLong("dbCheckTimeout", 5000l);
		for (Object o : container.config().getArray("pre-required-modules", new JsonArray())) {
			if (!(o instanceof JsonObject)) continue;
			if (((JsonObject) o).getString("name", "").startsWith("fr.wseduc~mod-postgresql")) {
				postgresql = true;
				break;
			}
		}
	}

	@Get("/monitoring/db")
	public void checkDb(final HttpServerRequest request) {
		final JsonObject result = new JsonObject();
		final AtomicBoolean closed = new AtomicBoolean(false);
		final long timerId = vertx.setTimer(dbCheckTimeout, new Handler<Long>() {
			@Override
			public void handle(Long event) {
				closed.set(true);
				result.putString("status", "timeout");
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
		final JsonArray versions = new JsonArray();
		ConcurrentSharedMap<String, String> versionMap = vertx.sharedData().getMap("versions");
		for (Map.Entry<String,String> entry : versionMap.entrySet()) {
			versions.addObject(new JsonObject().putString(entry.getKey(), entry.getValue()));
		}
		Renders.renderJson(request, versions);
	}

	private Handler<Message<JsonObject>> getResponseHandler(final String module, final long timerId,
			final JsonObject result, final AtomicInteger count, final HttpServerRequest request, final AtomicBoolean closed) {
		return new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				result.putString(module, event.body().getString("status"));
				if (count.decrementAndGet() <= 0 && !closed.get()) {
					vertx.cancelTimer(timerId);
					boolean error = false;
					for (String element : result.getFieldNames()) {
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
