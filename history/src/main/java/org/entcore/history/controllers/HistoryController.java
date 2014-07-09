/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.history.controllers;

import fr.wseduc.webutils.Controller;
import fr.wseduc.security.SecuredAction;

import java.util.Map;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;


public class HistoryController extends Controller {

	private final String logPath;

	public HistoryController(Vertx vertx, Container container,
		RouteMatcher rm, Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
			super(vertx, container, rm, securedActions);
			this.logPath = container.config().getString("log-path");
		}

	@SecuredAction("history.authent")
	public void history(HttpServerRequest request) {
		JsonObject traceFiles = new JsonObject().putArray("traceFiles", new JsonArray());
		for (String file : vertx.fileSystem().readDirSync(logPath)) {
			if (!file.endsWith(".trace")) { continue; }
			file = file.replace(logPath, "").replace(".trace", "");
			traceFiles.getArray("traceFiles").addObject(new JsonObject().putString("name", file));
		}
		renderView(request, traceFiles);
	}

	@SecuredAction("history.authent")
	public void logs(final HttpServerRequest request) {
		try {
			String tracesFile = logPath + request.params().get("app") + ".trace";
			renderJson(request, tracesToJson(tracesFile));
		} catch (Exception ex) {
			// TODO : manage end-user error message (i18n , human compatible message ...)
			renderError(request);
		}
	}

	// TODO : Try to writer a logFormatter in Trace Module that avoid this bloat operations
	private JsonObject tracesToJson(String logFileName) throws Exception {
		Buffer b = vertx.fileSystem().readFileSync(logFileName);
		String traces = b.toString().trim();
		String tracesArray = "[" + traces.substring(0, traces.length() -1) + "]";
		JsonObject jo = new JsonObject().putArray("records", new JsonArray(tracesArray));
		return jo;
	}
}