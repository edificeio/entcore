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
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

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
								b.addObject(new JsonObject()
										.putString("verb", binding.getMethod().name())
										.putString("path", binding.getUriPattern().pattern())
										.putString("type", binding.getActionType().name())
								);
							}
							actions.putArray(action.getName(), b);
						}
					}
				}
				Renders.renderJson(request, actions);
			}
		});
	}

}
