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

package org.entcore.auth.services.impl;


import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.security.SecuredAction;
import org.entcore.auth.services.FederationService;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.vertx.java.core.http.RouteMatcher;

import java.util.Map;

public class FederationServiceImpl extends BaseController implements FederationService {

	private MongoDb mongo = MongoDb.getInstance();
	private static final String SESSIONS_COLLECTION = "sessions";

	public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
					 Map<String, SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);
		mongo = MongoDb.getInstance();
	}

	@Override
	public void getMongoDbSession(final String sessionId, Handler<Message<JsonObject>> handler) {
		final JsonObject query = new JsonObject().put("_id", sessionId);
		mongo.findOne(SESSIONS_COLLECTION, query, handler);
	}
}
