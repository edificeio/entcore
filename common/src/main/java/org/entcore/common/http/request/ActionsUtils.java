/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
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
