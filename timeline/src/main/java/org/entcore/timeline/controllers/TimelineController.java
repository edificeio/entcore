/*
 * Copyright © WebServices pour l'Éducation, 2016
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
 */

package org.entcore.timeline.controllers;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Utils;
import fr.wseduc.webutils.collections.TTLSet;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.shareddata.LocalMap;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.AdmlOfStructures;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.notification.TimelineNotificationsLoader;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.timeline.events.DefaultTimelineEventStore;
import org.entcore.timeline.events.TimelineEventStore;
import org.entcore.timeline.events.TimelineEventStore.AdminAction;
import org.entcore.timeline.services.TimelineConfigService;
import org.entcore.timeline.services.TimelineMailerService;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.vertx.java.core.http.RouteMatcher;

import java.io.StringReader;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class TimelineController extends BaseController {

	private TimelineEventStore store;
	private TimelineConfigService configService;
	private TimelineMailerService mailerService;
	private Map<String, String> registeredNotifications;
	private LocalMap<String, String> eventsI18n;
	private HashMap<String, JsonObject> lazyEventsI18n;
	private Set<String> antiFlood;

	//Declaring a TimelineHelper ensures the loading of the i18n/timeline folder.
	private TimelineHelper timelineHelper;
	private JsonArray eventTypes; // cache to improve perfs
	private boolean refreshTypesCache;

	public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);
		store = new DefaultTimelineEventStore();
		timelineHelper = new TimelineHelper(vertx, eb, config);
		antiFlood = new TTLSet<>(config.getLong("antiFloodDelay", 3000l),
				vertx, config.getLong("antiFloodClear", 3600 * 1000l));
		refreshTypesCache = config.getBoolean("refreshTypesCache", false);
	}

	/* Override i18n to use additional timeline translations and nested templates */
	@Override
	protected void setLambdaTemplateRequest(final HttpServerRequest request, final Map<String, Object> ctx) {
		super.setLambdaTemplateRequest(request, ctx);
		TimelineLambda.setLambdaTemplateRequest(request, ctx, eventsI18n, lazyEventsI18n);
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

	@Get("/historyView")
	@SecuredAction(value = "timeline.historyView")
	public void historyView(HttpServerRequest request) {
		renderView(request);
	}

	@Get("/i18nNotifications")
	@SecuredAction(value = "timeline.i18n", type = ActionType.AUTHENTICATED)
	public void i18n(HttpServerRequest request) {
		String language = Utils.getOrElse(
				I18n.acceptLanguage(request), "fr", false);
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
					.put("key", key);
			notif.remove("template");
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
		final boolean mine = request.params().contains("mine");

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

							store.get(user, types, offset, 25, notifs.right().getValue(), mine, new Handler<JsonObject>() {
								public void handle(final JsonObject res) {
									if (res != null && "ok".equals(res.getString("status"))) {
										JsonArray results = res.getJsonArray("results", new JsonArray());
										final JsonArray compiledResults = new JsonArray();

										final AtomicInteger countdown = new AtomicInteger(results.size());
										final Handler<Void> endHandler = new Handler<Void>() {
											public void handle(Void v) {
												if (countdown.decrementAndGet() <= 0) {
													res.put("results", compiledResults);
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
											processTemplate(request,notif.getJsonObject("params",new JsonObject()),key, reader, new Handler<Writer>() {
												public void handle(Writer writer) {
													notif.put("message", writer.toString());
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
		if (eventTypes != null) {
			renderJson(request, eventTypes);
		} else {
			store.listTypes(new Handler<JsonArray>() {

				@Override
				public void handle(JsonArray res) {
					renderJson(request, res);
					eventTypes = res;
				}
			});
		}
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

	@Get("/admin-history")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void adminHistory(final HttpServerRequest request) {
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
					JsonObject notif = new JsonObject(registeredNotifications.get(key)).put("key", key);
					notif.remove("template");
					for(Object admcDefaultObj : admcDefaults){
						JsonObject admcDefault = (JsonObject) admcDefaultObj;
						if(admcDefault.getString("key", "").equals(key)){
							notif.mergeIn(admcDefault);
							notif.remove("_id");
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
		mailerService.sendDailyMails(0, defaultResponseHandler(request));
	}

	@Get("/performWeeklyMailing")
	@SecuredAction(type = ActionType.RESOURCE, value = "")
	@ResourceFilter(SuperAdminFilter.class)
	public void performWeeklyMailing(final HttpServerRequest request) {
		mailerService.sendWeeklyMails(0, defaultResponseHandler(request));
	}

	@Get("/allowLanguages")
	@SecuredAction("timeline.allowLanguages")
	public void allowLanguages(final HttpServerRequest request) {
		// This route is used to create allowLanguages Workflow right, nothing to do
		return;
	}

	@Delete("/:id")
	@SecuredAction("timeline.delete.own.notification")
	public void deleteNotification(final HttpServerRequest request) {
		final String id = request.params().get("id");
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			public void handle(final UserInfos user) {
				if(user == null){
					unauthorized(request);
					return;
				}
				store.delete(id, user.getUserId(), defaultResponseHandler(request));
			}
		});
	}

	@Put("/:id")
	@SecuredAction("timeline.discard.notification")
	public void discardNotification(final HttpServerRequest request) {
		final String id = request.params().get("id");
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			public void handle(final UserInfos user) {
				if(user == null){
					unauthorized(request);
					return;
				}
				store.discard(id, user.getUserId(), defaultResponseHandler(request));
			}
		});
	}

	@Put("/:id/report")
	@SecuredAction("timeline.report.notification")
	public void reportNotification(final HttpServerRequest request) {
		final String id = request.params().get("id");
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			public void handle(final UserInfos user) {
				if(user == null){
					unauthorized(request);
					return;
				}
				store.report(id, user, new Handler<Either<String,JsonObject>>() {
					public void handle(Either<String, JsonObject> event) {
						defaultResponseHandler(request).handle(event);

						if(event.isLeft() || event.right().getValue().getInteger("number", 0) == 0) {
							return;
						}

						final List<String> structureIds = user.getStructures();
						final JsonObject params = new JsonObject()
							.put("username", user.getUsername())
							.put("uri", "/userbook/annuaire#" + user.getUserId() + "#" + user.getType());

						final AtomicInteger countdown = new AtomicInteger(structureIds.size());
						final Set<String> recipientsSet = new HashSet<>();
						final Handler<Void> finalHandler = new Handler<Void>() {
							public void handle(Void v) {
								if(countdown.decrementAndGet() == 0){
									ArrayList<String> recipients = new ArrayList<>();
									recipients.addAll(recipientsSet);
									timelineHelper.notifyTimeline(
											request,
											"timeline.notify-report",
											null,
											recipients,
											id,
											params);
								}
							}
						};

						for(final String structureId : structureIds){
							JsonObject message = new JsonObject()
								.put("action", "list-adml")
								.put("structureId", structureId);

							eb.send("directory", message, result -> {
								if (result.succeeded()) {
									JsonArray users = (JsonArray) result.result().body();
									for (Object userObj : users) {
										JsonObject user = (JsonObject) userObj;
										recipientsSet.add(user.getString("id"));
									}
								} else {
									log.error("Error list adml", result.cause());
								}
								finalHandler.handle(null);
							});
						}
					}
				});
			}
		});
	}

	final int PAGELIMIT = 25;

	@Get("/reported")
	@SecuredAction(type = ActionType.RESOURCE, value = "")
	@ResourceFilter(AdmlOfStructures.class)
	public void listReportedNotifications(final HttpServerRequest request) {
		final String structure = request.params().get("structure");
		final boolean pending = Boolean.parseBoolean(request.params().get("pending"));
		int page = 0;
		if(request.params().contains("page")){
			try {
				page = Integer.parseInt(request.params().get("page"));
			} catch(NumberFormatException e) {
				//silent
			}
		}
		store.listReported(structure, pending, PAGELIMIT*page, PAGELIMIT, new Handler<Either<String,JsonArray>>() {
			public void handle(Either<String, JsonArray> event) {
				if(event.isLeft()){
					renderError(request);
					return;
				}

				final JsonArray results = event.right().getValue();
				final JsonArray compiledResults = new JsonArray();

				final AtomicInteger countdown = new AtomicInteger(results.size());
				final Handler<Void> endHandler = new Handler<Void>() {
					public void handle(Void v) {
						if (countdown.decrementAndGet() <= 0) {
							renderJson(request, compiledResults);
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
					processTemplate(request,notif.getJsonObject("params",new JsonObject()),key, reader, new Handler<Writer>() {
						public void handle(Writer writer) {
							notif.put("message", writer.toString());
							compiledResults.add(notif);
							endHandler.handle(null);
						}
					});
				}
			}
		});
	}

	@Put("/:id/action/keep")
	@SecuredAction(type = ActionType.RESOURCE, value = "")
	@ResourceFilter(AdmlOfStructures.class)
	public void adminKeepAction(final HttpServerRequest request) {
		final String id = request.params().get("id");
		final String structureId = request.params().get("structure");
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			public void handle(UserInfos user) {
				if(user == null){
					unauthorized(request);
					return;
				}
				store.performAdminAction(id, structureId, user, AdminAction.KEEP, new Handler<Either<String,JsonObject>>() {
					public void handle(Either<String, JsonObject> event) {
						if(event.isRight()){
							store.deleteReportNotification(id, new Handler<Either<String,JsonObject>>() {
								public void handle(Either<String, JsonObject> event) {
									if(event.isLeft()){
										log.error(event.left().getValue());
									}
								}
							});
						}
						defaultResponseHandler(request).handle(event);
					}
				});
			}
		});
	}

	@Put("/:id/action/delete")
	@SecuredAction(type = ActionType.RESOURCE, value = "")
	@ResourceFilter(AdmlOfStructures.class)
	public void adminDeleteAction(final HttpServerRequest request) {
		final String id = request.params().get("id");
		final String structureId = request.params().get("structure");
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			public void handle(UserInfos user) {
				if(user == null){
					unauthorized(request);
					return;
				}
				store.performAdminAction(id, structureId, user, AdminAction.DELETE, new Handler<Either<String,JsonObject>>() {
					public void handle(Either<String, JsonObject> event) {
						if(event.isRight()){
							store.deleteReportNotification(id, new Handler<Either<String,JsonObject>>() {
								public void handle(Either<String, JsonObject> event) {
									if(event.isLeft()){
										log.error(event.left().getValue());
									}
								}
							});
						}
						defaultResponseHandler(request).handle(event);
					}
				});
			}
		});
	}

	@BusAddress("wse.timeline")
	public void busApi(final Message<JsonObject> message) {
		if (message == null) {
			return;
		}
		final JsonObject json = message.body();
		if (json == null) {
			message.reply(new JsonObject().put("status", "error")
					.put("message", "Invalid body."));
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
			message.reply(new JsonObject().put("status", "error")
					.put("message", "Invalid action."));
			return;
		}

		switch (action) {
		case "add":
			final String sender = json.getString("sender");
			if (sender == null || sender.startsWith("no-reply") || json.getBoolean("disableAntiFlood", false) || antiFlood.add(sender)) {
				store.add(json, new Handler<JsonObject>() {
					public void handle(JsonObject result) {
						mailerService.sendImmediateMails(
								new JsonHttpServerRequest(json.getJsonObject("request")),
								json.getString("notificationName"), json.getJsonObject("notification"), json.getJsonObject("params"),
								json.getJsonArray("recipientsIds")
						);
						handler.handle(result);
					}
				});
				if (refreshTypesCache && eventTypes != null && !eventTypes.contains(json.getString("type"))) {
					eventTypes = null;
				}
			} else {
				message.reply(new JsonObject().put("status", "error")
						.put("message", "flood"));
			}
			break;
		case "get":
			UserInfos u = new UserInfos();
			u.setUserId(json.getString("recipient"));
			u.setExternalId(json.getString("externalId"));
			store.get(u, null, json.getInteger("offset", 0),
					json.getInteger("limit", 25), null, false, handler);
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
					message.reply(new JsonObject().put("status", "ok")
							.put("types", types));
				}
			});
			break;
		default:
			message.reply(new JsonObject().put("status", "error")
					.put("message", "Invalid action."));
		}
	}

	private void getExternalNotifications(final Handler<Either<String, JsonObject>> handler) {
		configService.list(new Handler<Either<String, JsonArray>>() {
			public void handle(Either<String, JsonArray> event) {
				if (event.isLeft()) {
					handler.handle(new Either.Left<String, JsonObject>(event.left().getValue()));
					return;
				}
				final JsonObject restricted = new JsonObject();
				for (String key : registeredNotifications.keySet()) {
					JsonObject notif = new JsonObject(registeredNotifications.get(key));
					String restriction = notif.getString("restriction",TimelineNotificationsLoader.Restrictions.NONE.name());
					for (Object notifConfigObj : event.right().getValue()) {
						JsonObject notifConfig = (JsonObject) notifConfigObj;
						if (notifConfig.getString("key", "").equals(key)) {
							restriction = notifConfig.getString("restriction", restriction);
							break;
						}
					}
					if (restriction.equals(TimelineNotificationsLoader.Restrictions.EXTERNAL.name()) ||
							restriction.equals(TimelineNotificationsLoader.Restrictions.HIDDEN.name())) {
						String notifType = notif.getString("type");
						if (!restricted.containsKey(notifType)) {
							restricted.put(notifType, new JsonArray());
						}
						restricted.getJsonArray(notifType).add(notif.getString("event-type"));
					}
				}
				handler.handle(new Either.Right<String, JsonObject>(restricted));
			}
		});
	}

	public void setConfigService(TimelineConfigService configService) {
		this.configService = configService;
	}

	public void setMailerService(TimelineMailerService mailerService) {
		this.mailerService = mailerService;
	}

	public void setRegisteredNotifications(Map<String, String> registeredNotifications) {
		this.registeredNotifications = registeredNotifications;
	}

	public void setEventsI18n(LocalMap<String, String> eventsI18n) {
		this.eventsI18n = eventsI18n;
	}

	public void setLazyEventsI18n(HashMap<String, JsonObject> lazyEventsI18n) {
		this.lazyEventsI18n = lazyEventsI18n;
	}
}
