/*
 * Copyright © "Open Digital Education", 2014
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

package org.entcore.auth.controllers;

import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import org.entcore.auth.services.SafeRedirectionService;
import org.vertx.java.core.http.RouteMatcher;
import java.util.Map;

public class RedirectController extends BaseController {

    protected final SafeRedirectionService redirectionService = SafeRedirectionService.getInstance();

	@Override
	public void init(Vertx vertx, JsonObject config, RouteMatcher rm, Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
        super.init(vertx, config, rm, securedActions);
    }

    @Get("/redirect")
    @ApiDoc("Redirect to the given URL after checking its safety")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void redirect(final HttpServerRequest request) {
        String targetUrl = request.getParam("url");
        if (targetUrl == null || targetUrl.isEmpty()) {
            log.error("[Auth@RedirectController::redirect] No URL where redirect to.");
            badRequest(request);
            return;
        }
        redirectionService.redirect(request, targetUrl, "");
    }

}
