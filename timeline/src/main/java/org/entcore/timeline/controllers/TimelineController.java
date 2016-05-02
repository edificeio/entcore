package org.entcore.timeline.controllers;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Utils;
import fr.wseduc.security.ActionType;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.impl.VertxInternal;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.spi.cluster.ClusterManager;
import org.vertx.java.platform.Container;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import com.samskivert.mustache.Template.Fragment;

import org.entcore.common.user.UserUtils;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.notification.TimelineMailer;
import org.entcore.common.notification.TimelineNotificationsLoader;
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
	private HashMap<String, JsonObject> lazyEventsI18n = new HashMap<>();
	private Map<String, String> registeredNotifications;

	//Declaring a TimelineHelper ensures the loading of the i18n/timeline folder.
	@SuppressWarnings("unused")
	private TimelineHelper timelineHelper;

	private final String TIMELINE_CONFIG_COLLECTION = "timeline.config";

	private TimelineMailer mailer;

	public void init(Vertx vertx, Container container, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, container, rm, securedActions);
		store = new DefaultTimelineEventStore();
		configService = new DefaultTimelineConfigService(
				TIMELINE_CONFIG_COLLECTION);
		eventsI18n = vertx.sharedData().getMap("timelineEventsI18n");
		timelineHelper = new TimelineHelper(vertx, eb, container);
		Boolean cluster = (Boolean) vertx.sharedData().getMap("server")
				.get("cluster");
		if (Boolean.TRUE.equals(cluster)) {
			ClusterManager cm = ((VertxInternal) vertx).clusterManager();
			registeredNotifications = cm.getSyncMap("notificationsMap");
		} else {
			registeredNotifications = vertx.sharedData()
					.getMap("notificationsMap");
		}
		mailer = new TimelineMailer(vertx, eb, container);
	}

	/* Override i18n to use additional timeline translations and nested templates */
	@Override
	protected void setLambdaTemplateRequest(final HttpServerRequest request, final Map<String, Object> ctx) {
		super.setLambdaTemplateRequest(request, ctx);

		ctx.put("i18n", new Mustache.Lambda() {
			@Override
			public void execute(Template.Fragment frag, Writer out)
					throws IOException {
				String key = frag.execute();
				String language = Utils.getOrElse(
						request.headers().get("Accept-Language"), "fr", false);

				JsonObject timelineI18n;
				if (!lazyEventsI18n.containsKey(language)) {
					String i18n = eventsI18n.get(language.split(",")[0].split("-")[0]);
					timelineI18n = new JsonObject("{" + i18n.substring(0, i18n.length() - 1) + "}");
					lazyEventsI18n.put(language, timelineI18n);
				} else {
					timelineI18n = lazyEventsI18n.get(language);
				}

				if (timelineI18n.getString(key, key).equals(key)) {
					out.write(I18n.getInstance().translate(key, language));
				} else {
					out.write(timelineI18n.getString(key, key));
				}
			}
		});

		ctx.put("nested", new Mustache.Lambda() {
			public void execute(Fragment frag, Writer out) throws IOException {
				String nestedTemplateName = frag.execute();
				String nestedTemplate = (String) ctx.get(nestedTemplateName);
				if(nestedTemplate != null)
					Mustache.compiler().compile(nestedTemplate).execute(ctx, out);
			}
		});

		ctx.put("nestedArray", new Mustache.Lambda() {
			public void execute(Fragment frag, Writer out) throws IOException {
				String nestedTemplatePos = frag.execute();
				JsonArray nestedArray = new JsonArray((List<Object>) ctx.get("nestedTemplatesArray"));
				try {
					JsonObject nestedTemplate = (JsonObject) nestedArray.get(Integer.parseInt(nestedTemplatePos) - 1);
					ctx.putAll(nestedTemplate.getObject("params", new JsonObject()).toMap());
					Mustache.compiler()
						.compile(nestedTemplate.getString("template", ""))
						.execute(ctx, out);
				} catch(NumberFormatException e) {
					log.error("Mustache compiler error while parsing a nested template array lambda.");
				}
			}
		});
	}

	@Get("/timeline")
	@SecuredAction(value = "timeline.view", type = ActionType.AUTHENTICATED)
	public void view(HttpServerRequest request) {
		renderView(request);
	}

	@Get("/preferencesView")
	@SecuredAction(value = "timeline.preferencesView", type = ActionType.AUTHENTICATED)
	public void preferencesView(HttpServerRequest request) {
		renderView(request);
	}

	@Get("/i18nNotifications")
	@SecuredAction(value = "timeline.i18n", type = ActionType.AUTHENTICATED)
	public void i18n(HttpServerRequest request) {
		String language = Utils.getOrElse(
				request.headers().get("Accept-Language"), "fr", false);
		String i18n = eventsI18n.get(language.split(",")[0].split("-")[0]);
		if (i18n == null) {
			i18n = eventsI18n.get("fr");
		}
		renderJson(request, new JsonObject(
				"{" + i18n.substring(0, i18n.length() - 1) + "}"));
	}

	@Get("/registeredNotifications")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void registeredNotifications(HttpServerRequest request) {
		JsonArray reply = new JsonArray();
		for (String key : registeredNotifications.keySet()) {
			JsonObject notif = new JsonObject(registeredNotifications.get(key))
					.putString("key", key);
			notif.removeField("template");
			reply.add(notif);
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
							if (notifs.isLeft()) {
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
								public void handle(final JsonObject res) {
									if (res != null && "ok".equals(res.getString("status"))) {
										JsonArray results = res.getArray("results", new JsonArray());
										final JsonArray compiledResults = new JsonArray();

										final AtomicInteger countdown = new AtomicInteger(results.size());
										final VoidHandler endHandler = new VoidHandler() {
											protected void handle() {
												if (countdown.decrementAndGet() <= 0) {
													res.putArray("results", compiledResults);
													renderJson(request, res);
												}
											}
										};
										if (results.size() == 0)
											endHandler.handle(null);

										for (Object notifObj : results) {
											final JsonObject notif = (JsonObject) notifObj;
											if (!notif.getString("message", "").isEmpty()) {
												compiledResults.add(notif);
												endHandler.handle(null);
												continue;
											}

											String key = notif.getString("type", "").toLowerCase()
												+ "."
												+ notif.getString("event-type", "").toLowerCase();

											String stringifiedRegisteredNotif = registeredNotifications.get(key);
											if (stringifiedRegisteredNotif == null) {
												log.error("Failed to retrieve registered from the shared map notification with key : " + key);
												endHandler.handle(null);
												continue;
											}
											JsonObject registeredNotif = new JsonObject(stringifiedRegisteredNotif);

											StringReader reader = new StringReader(registeredNotif.getString("template", ""));
											processTemplate(request,notif.getObject("params",new JsonObject()),key, reader, new Handler<Writer>() {
												public void handle(Writer writer) {
													notif.putString("message", writer.toString());
													compiledResults.add(notif);
													endHandler.handle(null);
												}
											});
										}
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
		RequestUtils.bodyToJson(request, pathPrefix + "publish",
				new Handler<JsonObject>() {
					@Override
					public void handle(JsonObject json) {
						store.add(json, new Handler<JsonObject>() {
							@Override
							public void handle(JsonObject res) {
								if ("ok".equals(res.getString("status"))) {
									created(request);
								} else {
									badRequest(request,
											res.getString("message"));
								}
							}
						});
					}
				});
	}

	@Get("/admin-console")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void adminPage(final HttpServerRequest request) {
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
	public void updateConfig(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
			public void handle(JsonObject data) {
				configService.upsert(data, defaultResponseHandler(request));
			}
		});
	}

	@Get("/notifications-defaults")
	@SecuredAction("timeline.external.notifications")
	public void mixinConfig(final HttpServerRequest request){
		configService.list(new Handler<Either<String,JsonArray>>() {
			public void handle(Either<String, JsonArray> event) {
				if(event.isLeft()){
					badRequest(request);
					return;
				}
				JsonArray admcDefaults = event.right().getValue();
				JsonArray reply = new JsonArray();

				for (String key : registeredNotifications.keySet()) {
					JsonObject notif = new JsonObject(registeredNotifications.get(key)).putString("key", key);
					notif.removeField("template");
					for(Object admcDefaultObj : admcDefaults){
						JsonObject admcDefault = (JsonObject) admcDefaultObj;
						if(admcDefault.getString("key", "").equals(key)){
							notif.mergeIn(admcDefault);
							notif.removeField("_id");
							break;
						}
					}
					reply.add(notif);
				}
				renderJson(request, reply);
			}
		});

	}

	@Get("/performDailyMailing")
	@SecuredAction(type = ActionType.RESOURCE, value = "")
	@ResourceFilter(SuperAdminFilter.class)
	public void performDailyMailing(final HttpServerRequest request) {
		mailer.sendDailyMails(0, defaultResponseHandler(request));
	}

	@Get("/performWeeklyMailing")
	@SecuredAction(type = ActionType.RESOURCE, value = "")
	@ResourceFilter(SuperAdminFilter.class)
	public void performWeeklyMailing(final HttpServerRequest request) {
		mailer.sendWeeklyMails(0, defaultResponseHandler(request));
	}

	@BusAddress("wse.timeline")
	public void busApi(final Message<JsonObject> message) {
		if (message == null) {
			return;
		}
		final JsonObject json = message.body();
		if (json == null) {
			message.reply(new JsonObject().putString("status", "error")
					.putString("message", "Invalid body."));
			return;
		}

		final Handler<JsonObject> handler = new Handler<JsonObject>() {
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
			store.add(json, new Handler<JsonObject>() {
				public void handle(JsonObject result) {
					handler.handle(result);
				}
			});
			break;
		case "get":
			UserInfos u = new UserInfos();
			u.setUserId(json.getString("recipient"));
			u.setExternalId(json.getString("externalId"));
			store.get(u, null, json.getInteger("offset", 0),
					json.getInteger("limit", 25), null, handler);
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
					message.reply(new JsonObject().putString("status", "ok")
							.putArray("types", types));
				}
			});
			break;
		case "get-notification-properties":
			getNotificationProperties(json.getString("key"),
					new Handler<Either<String, JsonObject>>() {
						public void handle(Either<String, JsonObject> event) {
							if (event.isLeft()) {
								message.reply(new JsonObject()
										.putString("status", "error")
										.putString("message",
												event.left().getValue()));
							} else {
								message.reply(new JsonObject()
										.putString("status", "ok")
										.putObject("result",
												event.right().getValue()));
							}
						}
					});
			break;
		case "list-notifications-defaults":
			configService.list(new Handler<Either<String,JsonArray>>() {
				public void handle(Either<String, JsonArray> event) {
					if (event.isLeft()) {
						message.reply(new JsonObject()
								.putString("status", "error")
								.putString("message",
										event.left().getValue()));
					} else {
						JsonArray config = event.right().getValue();

						JsonArray notificationsList = new JsonArray();
						for (String key : registeredNotifications.keySet()) {
							JsonObject notif = new JsonObject(registeredNotifications.get(key));
							notif.putString("key", key);
							for(Object notifConfigObj: config){
								JsonObject notifConfig = (JsonObject) notifConfigObj;
								if (notifConfig.getString("key", "").equals(key)) {
									notif.putString("defaultFrequency",
											notifConfig.getString("defaultFrequency", notif.getString("defaultFrequency")));
									notif.putString("restriction",
											notifConfig.getString("restriction", notif.getString("restriction")));
									break;
								}
							}
							notificationsList.add(notif);
						}
						message.reply(new JsonObject()
								.putString("status", "ok")
								.putArray("results", notificationsList));
					}
				}
			});
			break;
		case "process-timeline-template":
			final HttpServerRequest request = new JsonHttpServerRequest(
					message.body().getObject("request", new JsonObject()));
			final JsonObject parameters = message.body().getObject("parameters", new JsonObject());
			final String resourceName = message.body().getString("resourceName", "");
			if(message.body().getBoolean("reader", false)){
				final StringReader templateReader = new StringReader(message.body().getString("template"));
				processTemplate(request, parameters, resourceName, templateReader, new Handler<Writer>() {
					public void handle(Writer writer) {
						message.reply(
							new JsonObject()
								.putString("status", "ok")
								.putString("processedTemplate", writer.toString()));
					}
				});

			} else {
				processTemplate(request, message.body().getString("template", ""), parameters, new Handler<String>() {
					public void handle(String template) {
						message.reply(
							new JsonObject()
							 	.putString("status", "ok")
								.putString("processedTemplate", template));
					}
				});
			}

			break;
		case "translate-timeline":
			final JsonArray i18nKeys = message.body().getArray("i18nKeys", new JsonArray());
			final String language = message.body().getString("language", "fr");
			String i18n = eventsI18n
				.get(language.split(",")[0].split("-")[0]);
			JsonObject timelineI18n = new JsonObject(
					"{" + i18n.substring(0, i18n.length() - 1) + "}");
			timelineI18n.mergeIn(I18n.getInstance().load(language));
			JsonArray translations = new JsonArray();
			for(Object keyObj : i18nKeys){
				String key = (String) keyObj;
				translations.add(timelineI18n.getString(key, key));
			}
			message.reply(new JsonObject()
				.putString("status", "ok")
				.putArray("translations", translations));
			break;
		default:
			message.reply(new JsonObject().putString("status", "error")
					.putString("message", "Invalid action."));
		}
	}

	private void getExternalNotifications(
			final Handler<Either<String, JsonObject>> handler) {
		configService.list(new Handler<Either<String, JsonArray>>() {
			public void handle(Either<String, JsonArray> event) {
				if (event.isLeft()) {
					handler.handle(new Either.Left<String, JsonObject>(
							event.left().getValue()));
					return;
				}
				final JsonObject restricted = new JsonObject();
				for (String key : registeredNotifications.keySet()) {
					JsonObject notif = new JsonObject(
							registeredNotifications.get(key));
					String restriction = notif.getString("restriction",
							TimelineNotificationsLoader.Restrictions.NONE
									.name());
					for (Object notifConfigObj : event.right().getValue()) {
						JsonObject notifConfig = (JsonObject) notifConfigObj;
						if (notifConfig.getString("key", "").equals(key)) {
							restriction = notifConfig.getString("restriction",
									restriction);
							break;
						}
					}
					if (restriction
							.equals(TimelineNotificationsLoader.Restrictions.EXTERNAL
									.name())) {
						if (!restricted
								.containsField(notif.getString("type"))) {
							restricted.putArray("type", new JsonArray());
						}
						restricted.getArray("type")
								.add(notif.getString("event-type"));
					}
				}
				handler.handle(
						new Either.Right<String, JsonObject>(restricted));
			}
		});
	}

	private void getNotificationProperties(final String notificationKey,
			final Handler<Either<String, JsonObject>> handler) {
		configService.list(new Handler<Either<String, JsonArray>>() {
			public void handle(Either<String, JsonArray> event) {
				if (event.isLeft()) {
					handler.handle(new Either.Left<String, JsonObject>(
							event.left().getValue()));
					return;
				}
				final String notificationStr = registeredNotifications
						.get(notificationKey.toLowerCase());
				if (notificationStr == null) {
					handler.handle(new Either.Left<String, JsonObject>(
							"invalid.notification.key"));
					return;
				}
				final JsonObject notification = new JsonObject(notificationStr);
				for (Object notifConfigObj : event.right().getValue()) {
					JsonObject notifConfig = (JsonObject) notifConfigObj;
					if (notifConfig.getString("key", "")
							.equals(notificationKey.toLowerCase())) {
						notification.putString("defaultFrequency",
								notifConfig.getString("defaultFrequency", ""));
						notification.putString("restriction",
								notifConfig.getString("restriction", ""));
						break;
					}
				}
				handler.handle(
						new Either.Right<String, JsonObject>(notification));
			}
		});
	}

}
