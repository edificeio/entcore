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

package org.entcore.auth.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import org.entcore.auth.services.ConfigurationService;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class ConfigurationController extends BaseController {

	private ConfigurationService configurationService;

	@Put("/configure/welcome")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void editWelcomeMessage(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				if (body != null) {
					configurationService.editWelcomeMessage(getHost(request), body, defaultResponseHandler(request));
				} else {
					badRequest(request, "invalid.body");
				}
			}
		});
	}

	@Get("/configure/welcome")
	public void getWelcomeMessage(final HttpServerRequest request) {
		final String host = getHost(request);
		final String language = request.params().get("allLanguages") != null ? null : I18n.acceptLanguage(request).split(",")[0].split("\\-")[0];
		configurationService.getWelcomeMessage(host, language, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if (event.isRight()) {
					if (event.right().getValue() != null && event.right().getValue().size() > 0) {
						JsonObject res = event.right().getValue();
						if (res.getJsonObject(host) != null && language != null && res.getJsonObject(host).getString(language) != null) {
							renderJson(request, new JsonObject()
									.put("welcomeMessage", res.getJsonObject(host).getString(language))
									.put("enabled", res.getJsonObject(host).getBoolean("enabled", false))
							);
						} else if (res.getJsonObject(host) != null) {
							if (!res.getJsonObject(host).containsKey("enabled")) {
								res.getJsonObject(host).put("enabled", false);
							}
							renderJson(request, res.getJsonObject(host));
						} else {
							renderJson(request, res);
						}
					} else {
						notFound(request);
					}
				} else {
					badRequest(request, event.left().getValue());
				}
			}
		});
	}

	public void setConfigurationService(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

}
