/*
 * Copyright © WebServices pour l'Éducation, 2015
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

package org.entcore.directory.security;

import fr.wseduc.webutils.http.Binding;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;

public class AddFunctionFilter extends AdmlOfUser {

	@Override
	protected void additionnalsChecks(HttpServerRequest resourceRequest,
			Binding binding, UserInfos user, UserInfos.Function adminLocal, final Handler<Boolean> handler) {
		bodyToJson(resourceRequest, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject event) {
				String function = event.getString("functionCode", "").trim();
				handler.handle(!function.isEmpty() && !"SUPER_ADMIN".equals(function));
			}
		});
		resourceRequest.resume();
	}

}
