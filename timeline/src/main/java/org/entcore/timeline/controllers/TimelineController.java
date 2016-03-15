package org.entcore.timeline.controllers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Utils;
import fr.wseduc.security.ActionType;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.impl.VertxInternal;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.spi.cluster.ClusterManager;
import org.vertx.java.platform.Container;

import org.entcore.common.user.UserUtils;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.notification.TimelineNotificationHelper;
import org.entcore.common.user.UserInfos;
import fr.wseduc.security.SecuredAction;
import org.entcore.timeline.events.DefaultTimelineEventStore;
import org.entcore.timeline.events.TimelineEventStore;
import org.entcore.timeline.services.TimelineConfigService;
import org.entcore.timeline.services.impl.DefaultTimelineConfigService;

import static org.entcore.common.http.response.DefaultResponseHandler.*;

public class TimelineController extends BaseController {

	private TimelineEventStore store;
	private TimelineConfigService configService;
	private ConcurrentMap<String, String> eventsI18n;
	private Map<String, String> registeredNotifications;

	private final String TIMELINE_CONFIG_COLLECTION = "timeline.config";

	public void init(Vertx vertx, Container container,
			RouteMatcher rm, Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, container, rm, securedActions);
		store = new DefaultTimelineEventStore();
		configService = new DefaultTimelineConfigService(TIMELINE_CONFIG_COLLECTION);
		eventsI18n = vertx.sharedData().getMap("timelineEventsI18n");
		Boolean cluster = (Boolean) vertx.sharedData().getMap("server").get("cluster");
		if (Boolean.TRUE.equals(cluster)) {
			ClusterManager cm = ((VertxInternal) vertx).clusterManager();
			registeredNotifications = cm.getSyncMap("notificationsMap");
		} else {
			registeredNotifications = vertx.sharedData().getMap("notificationsMap");
		}
	}

	@Get("/timeline")
	@SecuredAction(value = "timeline.view", type = ActionType.AUTHENTICATED)
	public void view(HttpServerRequest request) {
		renderView(request);
	}

	@Get("/i18nNotifications")
	@SecuredAction(value = "timeline.i18n", type = ActionType.AUTHENTICATED)
	public void i18n(HttpServerRequest request) {
		String language = Utils.getOrElse(request.headers().get("Accept-Language"), "fr", false);
		String i18n = eventsI18n.get(language.split(",")[0].split("-")[0]);
		if (i18n == null) {
			i18n = eventsI18n.get("fr");
		}
		renderJson(request, new JsonObject("{" + i18n.substring(0, i18n.length() - 1) + "}"));
	}

	@Get("/registeredNotifications")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void registeredNotifications(HttpServerRequest request){
		JsonArray reply = new JsonArray();
		for(String key : registeredNotifications.keySet()){
			reply.add(new JsonObject(registeredNotifications.get(key)).putString("key", key));
		}
		renderJson(request, reply);
	}

	@Get("/calendar")
	@SecuredAction(value = "timeline.calendar", type = ActionType.AUTHENTICATED)
	public void calendar(HttpServerRequest request) {
		renderView(request);
	}

	@Get("/lastNotifications")
	@SecuredAction(value = "timeline.events", type = ActionType.AUTHENTICATED)
	public void lastEvents(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					getExternalNotifications(new Handler<Either<String, JsonObject>>() {
						public void handle(Either<String, JsonObject> notifs) {
							if(notifs.isLeft()){
								badRequest(request, notifs.left().getValue());
								return;
							}

							String page = request.params().get("page");
							List<String> types = request.params().getAll("type");
							int offset = 0;
							try {
								offset = 25 * Integer.parseInt(page);
							} catch (NumberFormatException e) {}

							store.get(user, types, offset, 25, notifs.right().getValue(), new Handler<JsonObject>() {
								public void handle(JsonObject res) {
									if (res != null && "ok".equals(res.getString("status"))) {
										renderJson(request, res);
									} else {
										renderError(request, res);
									}
								}
							});
						}
					});

				} else {
					unauthorized(request);
				}
			}
		});
	}

	private void getExternalNotifications(final Handler<Either<String, JsonObject>> handler){
		configService.list(new Handler<Either<String,JsonArray>>() {
			public void handle(Either<String, JsonArray> event) {
				if(event.isLeft()){
					handler.handle(new Either.Left<String, JsonObject>(event.left().getValue()));
					return;
				}
				final JsonObject restricted = new JsonObject();
				for(String key : registeredNotifications.keySet()){
					JsonObject notif = new JsonObject(registeredNotifications.get(key));
					String restriction = notif.getString("restriction", TimelineNotificationHelper.Restrictions.NONE.name());
					for(Object notifConfigObj : event.right().getValue()){
						JsonObject notifConfig = (JsonObject) notifConfigObj;
						if(notifConfig.getString("key", "").equals(key)){
							restriction = notifConfig.getString("restriction", restriction);
							break;
						}
					}
					if(restriction.equals(TimelineNotificationHelper.Restrictions.EXTERNAL.name())){
						if(!restricted.containsField(notif.getString("type"))){
							restricted.putArray("type", new JsonArray());
						}
						restricted.getArray("type").add(notif.getString("event-type"));
					}
				}
				handler.handle(new Either.Right<String, JsonObject>(restricted));
			}
		});

	}

	@Get("/types")
	@SecuredAction(value = "timeline.auth", type = ActionType.AUTHENTICATED)
	public void listTypes(final HttpServerRequest request) {
		store.listTypes(new Handler<JsonArray>() {

			@Override
			public void handle(JsonArray res) {
				renderJson(request, res);
			}
		});
	}

	@Post("/publish")
	@SecuredAction("timeline.publish")
	public void publish(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, pathPrefix + "publish", new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject json) {
				store.add(json, new Handler<JsonObject>() {
					@Override
					public void handle(JsonObject res) {
						if ("ok".equals(res.getString("status"))) {
							created(request);
						} else {
							badRequest(request, res.getString("message"));
						}
					}
				});
			}
		});
	}

	@Get("/admin-console")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void adminPage(final HttpServerRequest request){
		renderView(request);
	}

	@Get("/config")
	@SecuredAction(type = ActionType.AUTHENTICATED, value = "")
	public void getConfig(final HttpServerRequest request) {
		configService.list(arrayResponseHandler(request));
	}

	@Put("/config")
	@SecuredAction(type = ActionType.RESOURCE, value = "")
	@ResourceFilter(SuperAdminFilter.class)
	public void updateConfig(final HttpServerRequest request){
		RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
			public void handle(JsonObject data) {
				configService.upsert(data, defaultResponseHandler(request));
			}
		});
	}

	@BusAddress("wse.timeline")
	public void busApi(final Message<JsonObject> message) {
		if (message == null) {
			return;
		}
		JsonObject json = message.body();
		if (json == null) {
			message.reply(new JsonObject().putString("status", "error")
					.putString("message", "Invalid body."));
			return;
		}

		Handler<JsonObject> handler = new Handler<JsonObject>() {

			@Override
			public void handle(JsonObject event) {
				message.reply(event);
			}
		};

		String action = json.getString("action");
		if (action == null) {
			log.warn("Invalid action.");
			message.reply(new JsonObject().putString("status", "error")
					.putString("message", "Invalid action."));
			return;
		}

		switch (action) {
		case "add":
			store.add(json, handler);
			break;
		case "get":
			UserInfos u = new UserInfos();
			u.setUserId(json.getString("recipient"));
			u.setExternalId(json.getString("externalId"));
			store.get(u,
					null,
					json.getInteger("offset", 0),
					json.getInteger("limit", 25),
					null,
					handler);
			break;
		case "delete":
			store.delete(json.getString("resource"), handler);
			break;
		case "deleteSubResource":
			store.deleteSubResource(json.getString("sub-resource"), handler);
		case "list-types":
			store.listTypes(new Handler<JsonArray>() {
				@Override
				public void handle(JsonArray types) {
					message.reply(new JsonObject().putString("status", "ok").putArray("types", types));
				}
			});
			break;
		default:
			message.reply(new JsonObject().putString("status", "error")
					.putString("message", "Invalid action."));
		}
	}

}
