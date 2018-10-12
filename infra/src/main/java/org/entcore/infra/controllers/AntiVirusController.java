/*
 * Copyright © "Open Digital Education", 2017
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

import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.infra.services.AntivirusService;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class AntiVirusController extends BaseController {

	private AntivirusService antivirusService;

	@Post("/antivirus/check")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void checkScanReport(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject event) {
				if (event != null && isNotEmpty(event.getString("scanReport"))) {
					antivirusService.replaceInfectedFiles(event.getString("scanReport"), defaultResponseHandler(request));
				}
			}
		});
	}

	@Post("/antivirus/scan")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void scan(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject event) {
				if (event != null && isNotEmpty(event.getString("file"))) {
					antivirusService.scan(event.getString("file"));
					ok(request);
				} else {
					badRequest(request);
				}
			}
		});
	}

	public void setAntivirusService(AntivirusService antivirusService) {
		this.antivirusService = antivirusService;
	}

}
