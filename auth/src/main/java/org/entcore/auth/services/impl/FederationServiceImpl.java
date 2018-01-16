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

package org.entcore.auth.services.impl;


import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.security.SecuredAction;
import org.entcore.auth.services.FederationService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.Map;

public class FederationServiceImpl extends BaseController implements FederationService {

	private MongoDb mongo = MongoDb.getInstance();
	private static final String SESSIONS_COLLECTION = "sessions";

	public void init(Vertx vertx, Container container, RouteMatcher rm,
					 Map<String, SecuredAction> securedActions) {
		super.init(vertx, container, rm, securedActions);
		mongo = MongoDb.getInstance();
	}

	@Override
	public void getMongoDbSession(final String sessionId, Handler<Message<JsonObject>> handler) {
		final JsonObject query = new JsonObject().putString("_id", sessionId);
		mongo.findOne(SESSIONS_COLLECTION, query, handler);
	}
}
