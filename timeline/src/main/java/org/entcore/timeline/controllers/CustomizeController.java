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

package org.entcore.timeline.controllers;

import java.util.Map;

import org.entcore.common.events.EventHelper;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.timeline.Timeline;
import org.vertx.java.core.http.RouteMatcher;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public class CustomizeController extends BaseController {

	protected I18n i18n = I18n.getInstance();
	private final EventHelper eventHelper;

	public CustomizeController() {
		final EventStore eventStore = EventStoreFactory.getFactory().getEventStore(Timeline.class.getSimpleName());
		this.eventHelper = new EventHelper(eventStore);
	}

	public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);
	}

	@Get("/customize")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void customize(HttpServerRequest request) {
		eventHelper.onAccess(request);
		renderView(request, new JsonObject(), "homepage.html", null);
	}
}
