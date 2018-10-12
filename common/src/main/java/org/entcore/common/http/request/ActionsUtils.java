/* Copyright © "Open Digital Education", 2014
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
							JsonArray b = new fr.wseduc.webutils.collections.JsonArray();
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
