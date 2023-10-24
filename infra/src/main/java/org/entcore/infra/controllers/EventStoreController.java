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
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.StringUtils;
import org.entcore.infra.services.EventStoreService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.entcore.common.http.response.DefaultResponseHandler.asyncArrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.asyncDefaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.voidResponseHandler;

import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;

import static org.entcore.common.aggregation.MongoConstants.*;

public class EventStoreController extends BaseController {

	private EventStoreService eventStoreService;
	private JsonArray userBlackList;
	private final JsonArray eventWhiteList;
	private final JsonObject eventModuleReference;

	public EventStoreController (JsonObject eventConfig) {
		this.userBlackList = eventConfig.getJsonArray("user-blacklist", new JsonArray());
		this.eventWhiteList = eventConfig.getJsonArray("event-whitelist", new JsonArray());
		this.eventModuleReference = eventConfig.getJsonObject("module-reference", new JsonObject());

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

	@Post("/event/web/store")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void storeWeb(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null && !userBlackList.contains(user.getUserId())) {
				RequestUtils.bodyToJson(request, event -> {
					final String eventType = event.getString("event-type", "*");
					if (eventWhiteList.contains(eventType.toUpperCase()) || eventWhiteList.contains(eventType.toLowerCase())) {
						
						if(!event.containsKey("date")) {
							event.put("date", System.currentTimeMillis());
						}

						if(!event.containsKey("module") && request.headers().get("Referer") != null) {
							String module = parseModuleName(request.headers().get("Referer"));
							event.put("module",module);	
						}

						if(!event.containsKey("userId")) {
							event.put("userId", user.getUserId());
						}

						if(!event.containsKey("ua") && request.headers().get("User-Agent") != null) {
							event.put("ua", request.headers().get("User-Agent"));
						}
						
						if(!event.containsKey("referer") && request.headers().get("Referer") != null) {
							event.put("referer", request.headers().get("Referer"));
						}
						
						if(!event.containsKey("ip") && Renders.getIp(request) != null) {
							event.put("ip", Renders.getIp(request));
						}

						eventStoreService.store(event, voidResponseHandler(request));
					} else {
						Renders.badRequest(request, "bad event:"+eventType);
					}
				});
			} else {
				Renders.unauthorized(request);
			}
		});
	}

	@Post("/event/mobile/store")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void storeMobile(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null && !userBlackList.contains(user.getUserId())) {
				RequestUtils.bodyToJson(request, body -> {
					final String module = body.getString("module");
					if (!StringUtils.isEmpty(module)) {
						eventStoreService.generateMobileEvent(TRACE_TYPE_MOBILE, user,
								request, module, voidResponseHandler(request));
					} else {
						Renders.badRequest(request);
					}
				});
			} else {
				Renders.unauthorized(request);
			}
		});
	}

	@Get("/event/list/:type/:epoch")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void listEvents(final HttpServerRequest request) {
		final String type = request.params().get("type");
		if (!EventStoreService.EVENT_STORE_TYPES.contains(type)) {
			badRequest(request, "invalid.type");
			return;
		}
		try {
			final long epoch = Long.parseLong(request.params().get("epoch"));
			final long duration = request.params().contains("duration") ?
					Long.parseLong(request.params().get("duration")) : EventStoreService.ONE_DAY_DURATION;
			final boolean skipSynced =
					("true".equals(request.params().get("skip-synced")) || !request.params().contains("skip-synced"));
			eventStoreService.listEvents(type, epoch, duration, skipSynced, asyncArrayResponseHandler(request));
		} catch (RuntimeException e) {
			badRequest(request, "invalid.input.format");
		}
	}

	@Put("/event/mark/:type/:epoch")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void markSyncedEvents(final HttpServerRequest request) {
		final String type = request.params().get("type");
		if (!EventStoreService.EVENT_STORE_TYPES.contains(type)) {
			badRequest(request, "invalid.type");
			return;
		}
		try {
			final long epoch = Long.parseLong(request.params().get("epoch"));
			final long duration = request.params().contains("duration") ?
					Long.parseLong(request.params().get("duration")) : EventStoreService.ONE_DAY_DURATION;
			eventStoreService.markSyncedEvents(type, epoch, duration, asyncDefaultResponseHandler(request));
		} catch (RuntimeException e) {
			badRequest(request, "invalid.input.format");
		}
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

	@BusAddress("event.store.custom")
	public void eventStoreCustom(final Message<JsonObject> message) {
		eventStoreService.storeCustomEvent(message.body().getString("base-event-type"), message.body().getJsonObject("payload"));
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

	private String parseModuleName(String urlPath) {

		final String[] pathSegments = urlPath.split("/");
		
		if (pathSegments.length > 3 && !pathSegments[3].isEmpty()) {
			String module = pathSegments[3];

			if(module.contains("?")) {
				final String[] moduleSegments = module.split("\\?");

				if(moduleSegments.length > 0 && !moduleSegments[0].isEmpty()) {
					module = moduleSegments[0];
				}
			}

			if(!eventModuleReference.isEmpty() && eventModuleReference.containsKey(module)) {
				module = eventModuleReference.getString(module);
			} else {
				module = module.substring(0, 1).toUpperCase() + module.substring(1);
			}

			return module;
		} else {
			return "Portal";
		}
	}

}
