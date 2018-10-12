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

package org.entcore.infra.controllers;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Post;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.infra.services.EventStoreService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.entcore.common.http.response.DefaultResponseHandler.voidResponseHandler;

public class EventStoreController extends BaseController {

	private EventStoreService eventStoreService;
	private JsonArray userBlackList;

	public EventStoreController (JsonObject eventConfig) {
        this.userBlackList = eventConfig.getJsonArray("user-blacklist", new fr.wseduc.webutils.collections.JsonArray());
	}

	@Post("/event/store")
	@SecuredAction("event.store")
	public void store(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject event) {
			    if (!authorizedUser(event)) {
                    Renders.ok(request);
                } else {
                    eventStoreService.store(event, voidResponseHandler(request));
                }
			}
		});
	}

	@Post("/event/localhost/store")
	public void storeLocalhost(final HttpServerRequest request) {
		if (("localhost:"+ config.getInteger("port", 8001))
				.equalsIgnoreCase(request.headers().get("Host"))) {
			store(request);
		} else {
			forbidden(request, "invalid.host");
		}
	}

	@BusAddress("event.store")
	public void eventStore(final Message<JsonObject> message) {
		if (!authorizedUser(message.body())) {
			message.reply(new JsonObject().put("status", "ok"));
		} else {
			eventStoreService.store(message.body(), new Handler<Either<String, Void>>() {
				@Override
				public void handle(Either<String, Void> event) {
					if (event.isRight()) {
						message.reply(new JsonObject().put("status", "ok"));
					} else {
						message.reply(new JsonObject().put("status", "error")
								.put("message", event.left().getValue()));
					}
				}
			});
		}
	}

	@BusAddress("event.blacklist")
	public void getBlacklist(final Message<Void> message) {
		message.reply(this.userBlackList);
	}

	public void setEventStoreService(EventStoreService eventStoreService) {
		this.eventStoreService = eventStoreService;
	}

	private boolean authorizedUser (JsonObject event) {
		return !this.userBlackList.contains(event.getString("userId"));
	}

}
