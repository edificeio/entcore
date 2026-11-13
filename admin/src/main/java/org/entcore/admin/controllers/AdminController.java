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
 */

package org.entcore.admin.controllers;

import org.entcore.admin.Admin;
import org.entcore.common.events.EventHelper;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import static org.entcore.common.http.response.DefaultResponseHandler.*;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.security.MfaProtected;
import fr.wseduc.webutils.http.BaseController;

public class AdminController extends BaseController {
	/** Workflow right granting access to the moderation (reports) React app. */
	private static final String REPORTS_TREAT_RIGHT = "admin.reports.treat";

	private final EventHelper eventHelper;
	private final AdminFilter adminFilter = new AdminFilter();

	public AdminController(){
		final EventStore store = EventStoreFactory.getFactory().getEventStore(Admin.class.getSimpleName());
		eventHelper = new EventHelper(store);
	}

	@Get("")
	@SecuredAction(type = ActionType.RESOURCE, value = "")
	@ResourceFilter(AdminFilter.class)
	@MfaProtected()
	public void serveHome(HttpServerRequest request) {
		renderView(request, new JsonObject(), "admin.html", null);
		eventHelper.onAccess(request);
	}

	/** Catch-all for the Angular console routes ; /reports is served by the moderation React app. */
	@Get(value = "(?!api|reports(/|$)).*", regex = true)
	@SecuredAction(type = ActionType.RESOURCE, value = "")
	@ResourceFilter(AdminFilter.class)
	@MfaProtected()
	public void serveHomeAlias(HttpServerRequest request) {
		serveHome(request);
	}

	/* Signalements (moderation) */

	/**
	 * Serves the moderation React app (embedded in the Angular console through an iframe),
	 * for /admin/reports and all its sub-routes handled by the React router.
	 * Requires both the workflow right and the ADMC/ADML functions : a WORKFLOW action does not
	 * evaluate @ResourceFilter, so the AdminFilter check is done explicitly.
	 */
	@Get(value = "/reports(/.*)?", regex = true)
	@SecuredAction(REPORTS_TREAT_RIGHT)
	@MfaProtected()
	public void serveReports(HttpServerRequest request) {
		// getAuthenticatedUserInfos already answers 401 when there is no user
		UserUtils.getAuthenticatedUserInfos(eb, request)
			.onSuccess(user -> adminFilter.authorize(request, null, user, isAdmin -> {
				if (Boolean.TRUE.equals(isAdmin)) {
					renderView(request, new JsonObject(), "moderation.html", null);
				} else {
					unauthorized(request);
				}
			}));
	}
}
