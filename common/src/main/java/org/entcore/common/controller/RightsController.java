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

package org.entcore.common.controller;

import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.security.ActionType;
import fr.wseduc.webutils.security.SecuredAction;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.vertx.java.core.http.RouteMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RightsController extends BaseController {

	public static final List<String> allowedSharingRights = Arrays
			.asList("read", "contrib", "manager", "publish", "comment");
	private JsonObject rights;

	@Override
	public void init(Vertx vertx, JsonObject config, RouteMatcher rm, Map<String, SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);
		initRights(securedActions);
		get("/rights/sharing", "getRights");
	}

	private void initRights(Map<String, SecuredAction> securedActions) {
		rights = new JsonObject();
		for (SecuredAction action: securedActions.values()) {
			if (isSharingRight(action)) {
				JsonArray a = rights.getJsonArray(action.getDisplayName());
				if (a == null) {
					a = new fr.wseduc.webutils.collections.JsonArray();
					rights.put(action.getDisplayName(), a);
				}
				a.add(action.getName().replaceAll("\\.", "-"));
			}
		}
	}

	private boolean isSharingRight(SecuredAction action) {
		if (action == null || action.getDisplayName() == null || !ActionType.RESOURCE.name().equals(action.getType())) {
			return false;
		}
		String sharingType = action.getDisplayName().substring(action.getDisplayName().lastIndexOf('.') + 1);
		return allowedSharingRights.contains(sharingType);
	}

	public void getRights(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					renderJson(request, rights);
				} else {
					unauthorized(request);
				}
			}
		});
	}

}
