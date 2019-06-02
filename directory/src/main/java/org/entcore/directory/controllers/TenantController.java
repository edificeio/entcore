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

package org.entcore.directory.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.directory.services.TenantService;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;

public class TenantController extends BaseController {

	private TenantService tenantService;

	@Post("/tenant")
	@SecuredAction("tenant.create")
	public void create(final HttpServerRequest request) {
		bodyToJson(request, pathPrefix + "createTenant", new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject event) {
				tenantService.create(event, notEmptyResponseHandler(request, 201));
			}
		});
	}

	@Get("/tenant/:id")
	@SecuredAction("tenant.get")
	@ResourceFilter(SuperAdminFilter.class)
	public void get(HttpServerRequest request) {
		final String id = request.params().get("id");
		if ("all".equals(id)) {
			tenantService.list(arrayResponseHandler(request));
		} else {
			tenantService.get(id, defaultResponseHandler(request));
		}
	}

	public void setTenantService(TenantService tenantService) {
		this.tenantService = tenantService;
	}

}
