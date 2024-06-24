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

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.MfaProtected;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;

import java.util.Arrays;
import java.util.List;

import static org.entcore.common.utils.Config.defaultDeleteUserDelay;
import static org.entcore.common.utils.Config.defaultPreDeleteUserDelay;

public class PlatformInfoController extends BaseController {

	private boolean smsActivated;
	private static final List<String> PROFILES = Arrays.asList("Personnel", "Teacher", "Student", "Relative", "Guest");

	@Get("api/platform/module/sms")
	@SecuredAction(type = ActionType.RESOURCE, value = "")
	@ResourceFilter(AdminFilter.class)
	@MfaProtected()
	public void moduleSms(HttpServerRequest request) {
		renderJson(request, new JsonObject().put("activated", this.smsActivated), 200);
	}

	public boolean isSmsModule() {
		return smsActivated;
	}

	public void setSmsModule(boolean smsModule) {
		this.smsActivated = smsModule;
	}

	@Get("api/platform/config")
	@SecuredAction(type = ActionType.RESOURCE, value = "")
	@ResourceFilter(AdminFilter.class)
	@MfaProtected()
	public void readConfig(HttpServerRequest request) {
		LocalMap<Object, Object> serverMap = vertx.sharedData().getLocalMap("server");

		final JsonObject preDelete = config.getJsonObject("pre-delete");
		final JsonObject configuration =  new JsonObject()
				.put("delete-user-delay", config.getLong("delete-user-delay", defaultDeleteUserDelay))
				.put("reset-code-delay", config.getLong("resetCodeDelay", 0L))
				.put("distributions", config.getJsonArray("distributions", new JsonArray()))
				.put("hide-adminv1-link", config.getBoolean("hide-adminv1-link", false))
				.put("hide-personal-data", serverMap.get("hidePersonalData"))
				.put("mass-messaging-enabled", config.getBoolean("mass-messaging-enabled"));

		if (preDelete != null && preDelete.size() == PROFILES.size() &&
				PROFILES.containsAll(preDelete.fieldNames())) {
			for (String profile : preDelete.fieldNames()) {
				final JsonObject profilePreDelete = preDelete.getJsonObject(profile);
				if (profilePreDelete == null ||
						profilePreDelete.getLong("delay") == null) {
					configuration.put(profile.toLowerCase() + "-pre-delete-delay", defaultPreDeleteUserDelay);
				} else {
					configuration.put(profile.toLowerCase() + "-pre-delete-delay", profilePreDelete.getLong("delay"));
				}
			}
		} else {
			for (String profile : PROFILES) {
				configuration.put(profile.toLowerCase() + "-pre-delete-delay", defaultPreDeleteUserDelay);
			}
		}

		renderJson(request, configuration);
	}
}
