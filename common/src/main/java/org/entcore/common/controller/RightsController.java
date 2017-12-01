/*
 * Copyright © WebServices pour l'Éducation, 2014
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
					a = new JsonArray();
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
