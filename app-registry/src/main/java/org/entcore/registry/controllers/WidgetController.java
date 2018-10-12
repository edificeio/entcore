/*
 * Copyright © "Open Digital Education", 2016
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

package org.entcore.registry.controllers;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.registry.filters.AnyAdmin;
import org.entcore.registry.filters.SuperAdminFilter;
import org.entcore.registry.filters.WidgetLinkFilter;
import org.entcore.registry.services.WidgetService;
import org.entcore.registry.services.impl.DefaultWidgetService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.BaseController;

public class WidgetController extends BaseController {
	private final WidgetService service = new DefaultWidgetService();

	@Get("/widget-preview")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void widgetPreview(final HttpServerRequest request) {
		renderView(request);
	}

	@Get("/widgets")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AnyAdmin.class)
	public void listWidgets(final HttpServerRequest request){
		service.listWidgets(defaultResponseHandler(request));
	}

	@Get("/widget/:id")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void getWidgetInfos(final HttpServerRequest request){
		String widgetId = request.params().get("id");
		String structureId = request.params().get("structureId");
		service.getWidgetInfos(widgetId, structureId, defaultResponseHandler(request));
	}

	@Delete("/widget/:id")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void deleteWidget(final HttpServerRequest request){
		final String widgetId = request.params().get("id");
		service.deleteWidget(widgetId, defaultResponseHandler(request, 201));
	}

	@Put("/widget/:id/lock")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void toggleLock(final HttpServerRequest request){
		final String widgetId = request.params().get("id");
		service.toggleLock(widgetId, defaultResponseHandler(request));
	}

	@Post("/widget/:id/link/:groupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(WidgetLinkFilter.class)
	public void linkWidget(final HttpServerRequest request){
		final String widgetId = request.params().get("id");
		final List<String> groupIds = new ArrayList<String>();
		groupIds.add(request.params().get("groupId"));
		service.linkWidget(widgetId, groupIds, defaultResponseHandler(request));
	}

	@Delete("/widget/:id/link/:groupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(WidgetLinkFilter.class)
	public void unlinkWidget(final HttpServerRequest request){
		final String widgetId = request.params().get("id");
		final List<String> groupIds = new ArrayList<String>();
		groupIds.add(request.params().get("groupId"));
		service.unlinkWidget(widgetId, groupIds, defaultResponseHandler(request));

	}

	@Put("/widget/:id/mandatory/:groupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(WidgetLinkFilter.class)
	public void setWidgetMandatory(final HttpServerRequest request){
		final String widgetId = request.params().get("id");
		final List<String> groupIds = new ArrayList<String>();
		groupIds.add(request.params().get("groupId"));
		service.setMandatory(widgetId, groupIds, defaultResponseHandler(request));
	}

	@Delete("/widget/:id/mandatory/:groupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(WidgetLinkFilter.class)
	public void removeWidgetMandatory(final HttpServerRequest request){
		final String widgetId = request.params().get("id");
		final List<String> groupIds = new ArrayList<String>();
		groupIds.add(request.params().get("groupId"));
		service.removeMandatory(widgetId, groupIds, defaultResponseHandler(request));
	}

	@Put("/widget/:id/authorize/:structureId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void authorizeProfiles(final HttpServerRequest request) {
		final String widgetId = request.params().get("id");
		final String structureId = request.params().get("structureId");
		List<String> profiles = request.params().getAll("profile");

		service.massAuthorize(widgetId, structureId, profiles, defaultResponseHandler(request));
	}

	@Delete("/widget/:id/authorize/:structureId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void unauthorizeProfiles(final HttpServerRequest request) {
		final String widgetId = request.params().get("id");
		final String structureId = request.params().get("structureId");
		List<String> profiles = request.params().getAll("profile");

		service.massUnauthorize(widgetId, structureId, profiles, defaultResponseHandler(request));
	}

	@Put("/widget/:id/mandatory/:structureId/mass")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void setMandatoryProfiles(final HttpServerRequest request) {
		final String widgetId = request.params().get("id");
		final String structureId = request.params().get("structureId");
		List<String> profiles = request.params().getAll("profile");

		service.massSetMandatory(widgetId, structureId, profiles, defaultResponseHandler(request));
	}

	@Delete("/widget/:id/mandatory/:structureId/mass")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void removeMandatoryProfiles(final HttpServerRequest request) {
		final String widgetId = request.params().get("id");
		final String structureId = request.params().get("structureId");
		List<String> profiles = request.params().getAll("profile");

		service.massRemoveMandatory(widgetId, structureId, profiles, defaultResponseHandler(request));
	}

	@Post("/widget")
	public void recordWidget(final HttpServerRequest request) {
		if (("localhost:"+ config.getInteger("port", 8012))
				.equalsIgnoreCase(request.headers().get("Host"))) {
			bodyToJson(request, new Handler<JsonObject>() {
				@Override
				public void handle(JsonObject jo) {
					eb.send("wse.app.registry.widgets", jo, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> reply) {
							renderJson(request, reply.body());
						}
					}));
				}
			});
		} else {
			forbidden(request, "invalid.host");
		}
	}

	@BusAddress("wse.app.registry.widgets")
	public void collectWidgets(final Message<JsonObject> message) {
		final JsonArray widgets = message.body().getJsonArray("widgets", new fr.wseduc.webutils.collections.JsonArray());
		if(widgets.size() == 0 && message.body().containsKey("widget")){
			widgets.add(message.body().getJsonObject("widget"));
		} else if(widgets.size() == 0){
			message.reply(new JsonObject().put("status", "error").put("message", "invalid.parameters"));
			return;
		}

		final AtomicInteger countdown = new AtomicInteger(widgets.size());
		final JsonObject reply = new JsonObject()
				.put("status", "ok")
				.put("errors", new fr.wseduc.webutils.collections.JsonArray());

		final Handler<JsonObject> replyHandler = new Handler<JsonObject>(){
			public void handle(JsonObject res) {
				if("error".equals(res.getString("status"))){
					reply.put("status", "error");
					reply.getJsonArray("errors").add(reply.getString("message"));
				}
				if(countdown.decrementAndGet() == 0){
					message.reply(reply);
				}
			}
		};

		for(Object widgetObj : widgets){
			registerWidget((JsonObject) widgetObj, replyHandler);
		}
	}

	private void registerWidget(final JsonObject widget, final Handler<JsonObject> handler){
		final String widgetName = widget.getString("name");
		final String applicationName = widget.getString("applicationName");
		widget.remove("applicationName");
		if (widgetName != null && !widgetName.trim().isEmpty()) {
			service.createWidget(applicationName, widget, new Handler<Either<String, JsonObject>>() {
				@Override
				public void handle(Either<String, JsonObject> event) {
					JsonObject j = new JsonObject();
					if (event.isRight()) {
						j.put("status", "ok");
					} else {
						j.put("status", "error").put("message", event.left().getValue());
					}
					handler.handle(j);
				}
			});
		} else {
			handler.handle(new JsonObject().put("status", "error").put("message", "invalid.parameters"));
		}
	}
}
