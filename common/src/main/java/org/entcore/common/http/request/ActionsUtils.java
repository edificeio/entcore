/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.common.http.request;

import org.entcore.common.user.UserUtils;
import fr.wseduc.webutils.Controller;
import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.http.Renders;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Map;
import java.util.Set;

public class ActionsUtils {

	public static void findWorkflowSecureActions(EventBus eb, final HttpServerRequest request,
			Controller controller) {
		final Map<String, Set<Binding>> bindings = controller.getSecuredUriBinding();
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				JsonObject actions = new JsonObject();
				if (user != null) {
					for (UserInfos.Action action : user.getAuthorizedActions()) {
						if (bindings.containsKey(action.getName())) {
							JsonArray b = new JsonArray();
							for (Binding binding: bindings.get(action.getName())) {
								b.add(new JsonObject()
										.put("verb", binding.getMethod().name())
										.put("path", binding.getUriPattern().pattern())
										.put("type", binding.getActionType().name())
								);
							}
							actions.put(action.getName(), b);
						}
					}
				}
				Renders.renderJson(request, actions);
			}
		});
	}

}
