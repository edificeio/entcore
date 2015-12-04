package org.entcore.registry.controllers;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.registry.filters.AnyAdmin;
import org.entcore.registry.filters.SuperAdminFilter;
import org.entcore.registry.filters.WidgetLinkFilter;
import org.entcore.registry.services.WidgetService;
import org.entcore.registry.services.impl.DefaultWidgetService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.BaseController;

public class WidgetController extends BaseController {
	private final WidgetService service = new DefaultWidgetService();

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

	@Post("/widget")
	public void recordWidget(final HttpServerRequest request) {
		if (("localhost:"+ container.config().getInteger("port", 8012))
				.equalsIgnoreCase(request.headers().get("Host"))) {
			bodyToJson(request, new Handler<JsonObject>() {
				@Override
				public void handle(JsonObject jo) {
					eb.send("wse.app.registry.widgets", jo, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> reply) {
							renderJson(request, reply.body());
						}
					});
				}
			});
		} else {
			forbidden(request, "invalid.host");
		}
	}

	@BusAddress("wse.app.registry.widgets")
	public void collectWidgets(final Message<JsonObject> message) {
		final JsonArray widgets = message.body().getArray("widgets", new JsonArray());
		if(widgets.size() == 0 && message.body().containsField("widget")){
			widgets.add(message.body().getObject("widget"));
		} else if(widgets.size() == 0){
			message.reply(new JsonObject().putString("status", "error").putString("message", "invalid.parameters"));
			return;
		}

		final AtomicInteger countdown = new AtomicInteger(widgets.size());
		final JsonObject reply = new JsonObject()
				.putString("status", "ok")
				.putArray("errors", new JsonArray());

		final Handler<JsonObject> replyHandler = new Handler<JsonObject>(){
			public void handle(JsonObject res) {
				if("error".equals(res.getString("status"))){
					reply.putString("status", "error");
					reply.getArray("errors").addString(reply.getString("message"));
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
		widget.removeField("applicationName");
		if (widgetName != null && !widgetName.trim().isEmpty()) {
			service.createWidget(applicationName, widget, new Handler<Either<String, JsonObject>>() {
				@Override
				public void handle(Either<String, JsonObject> event) {
					JsonObject j = new JsonObject();
					if (event.isRight()) {
						j.putString("status", "ok");
					} else {
						j.putString("status", "error").putString("message", event.left().getValue());
					}
					handler.handle(j);
				}
			});
		} else {
			handler.handle(new JsonObject().putString("status", "error").putString("message", "invalid.parameters"));
		}
	}
}
