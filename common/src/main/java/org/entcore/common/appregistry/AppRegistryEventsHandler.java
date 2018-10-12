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

package org.entcore.common.appregistry;

import static org.entcore.common.appregistry.AppRegistryEvents.*;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public final class AppRegistryEventsHandler implements Handler<Message<JsonObject>> {

	private final AppRegistryEventsService appRegistryEventsService;

	public AppRegistryEventsHandler(Vertx vertx, AppRegistryEventsService service) {
		appRegistryEventsService = service;
		vertx.eventBus().localConsumer(APP_REGISTRY_PUBLISH_ADDRESS, this);
	}

	@Override
	public void handle(Message<JsonObject> event) {
		String type = event.body().getString("type");
		if (type != null) {
			switch (type) {
				case PROFILE_GROUP_ACTIONS_UPDATED:
					appRegistryEventsService.authorizedActionsUpdated(event.body().getJsonArray("groups"));
					break;
				case USER_GROUP_UPDATED:
					appRegistryEventsService.userGroupUpdated(event.body().getJsonArray("users"), event);
					break;
				case IMPORT_SUCCEEDED:
					appRegistryEventsService.importSucceeded();
					break;
			}
		}
	}

}
