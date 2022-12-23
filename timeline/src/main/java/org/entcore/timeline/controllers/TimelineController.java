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

package org.entcore.timeline.controllers;

import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.MfaProtected;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Utils;
import fr.wseduc.webutils.collections.TTLSet;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.shareddata.LocalMap;
import org.apache.commons.lang3.function.FailableDoubleUnaryOperator;
import org.entcore.common.cache.CacheService;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.AdmlOfStructures;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.mute.MuteHelper;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.notification.TimelineNotificationsLoader;
import org.entcore.common.notification.NotificationUtils;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.StringUtils;
import org.entcore.timeline.controllers.helper.NotificationHelper;
import org.entcore.timeline.events.CachedTimelineEventStore;
import org.entcore.timeline.events.DefaultTimelineEventStore;
import org.entcore.timeline.events.MobileTimelineEventStore;
import org.entcore.timeline.events.SplitTimelineEventStore;
import org.entcore.timeline.events.TimelineEventStore;
import org.entcore.timeline.events.TimelineEventStore.AdminAction;
import org.entcore.timeline.services.TimelineConfigService;
import org.entcore.timeline.services.TimelineMailerService;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.vertx.java.core.http.RouteMatcher;

import java.io.StringReader;
import java.io.Writer;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static fr.wseduc.webutils.Utils.isEmpty;
import static fr.wseduc.webutils.Utils.isNotEmpty;
import static java.util.Collections.emptySet;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class TimelineController extends BaseController {
    public static Logger log = LoggerFactory.getLogger(TimelineController.class);

	private TimelineEventStore store;
	private TimelineConfigService configService;
	private TimelineMailerService mailerService;
	private Map<String, String> registeredNotifications;
	private LocalMap<String, String> eventsI18n;
	private HashMap<String, JsonObject> lazyEventsI18n;
	private Set<String> antiFlood;

	//Declaring a TimelineHelper ensures the loading of the i18n/timeline folder.
	private TimelineHelper timelineHelper;
	private MuteHelper muteHelper;
	private JsonArray eventTypes; // cache to improve perfs
	private boolean refreshTypesCache;
	private NotificationHelper notificationHelper;
	protected I18n i18n = I18n.getInstance();

	// TEMPORARY to handle both timeline and timeline2 view
	private String defaultSkin;
	private Map<String, String> hostSkin;
	private JsonObject skinLevels;

	public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);
		store = new DefaultTimelineEventStore();
		final Integer maxRecipientLength = config.getInteger("maxRecipientLength");
		if(config.getBoolean("isolate-mobile", false)){
			store = new MobileTimelineEventStore(store);
		}
		if(maxRecipientLength != null){
			store = new SplitTimelineEventStore(store, maxRecipientLength);
		}
		timelineHelper = new TimelineHelper(vertx, eb, config);
		muteHelper = new MuteHelper(vertx);
		antiFlood = new TTLSet<>(config.getLong("antiFloodDelay", 3000l),
				vertx, config.getLong("antiFloodClear", 3600 * 1000l));
		refreshTypesCache = config.getBoolean("refreshTypesCache", false);
		if(config.getBoolean("cache", false)){
			final CacheService cacheService = CacheService.create(vertx, config);
			final Integer cacheLen = config.getInteger("cache-size", PAGELIMIT);
			store = new CachedTimelineEventStore(store, cacheService, cacheLen, configService, registeredNotifications);
		}
		if(config.getBoolean("isolate-mobile", false)){
			store = new MobileTimelineEventStore(store);
		}

		// TEMPORARY to handle both timeline and timeline2 view
		this.defaultSkin = config.getString("skin", "raw");
		this.hostSkin = new HashMap<>();
		JsonObject skins = new JsonObject(vertx.sharedData().getLocalMap("skins"));
		for (final String domain: skins.fieldNames()) {
			this.hostSkin.put(domain, skins.getString(domain));
		}
		this.skinLevels = new JsonObject(vertx.sharedData().getLocalMap("skin-levels"));
	}

	/* Override i18n to use additional timeline translations and nested templates */
	@Override
	protected void setLambdaTemplateRequest(final HttpServerRequest request) {
		super.setLambdaTemplateRequest(request);
		TimelineLambda.setLambdaTemplateRequest(request, this.templateProcessor, eventsI18n, lazyEventsI18n);
	}

	private boolean isLightmode(){
		final JsonObject publicConf =  config.getJsonObject("publicConf", new JsonObject());
		return publicConf.getBoolean("lightmode", false);
	}

	private JsonObject lightModeResult(HttpServerRequest request){
		final JsonObject publicConf =  config.getJsonObject("publicConf", new JsonObject());
		final String messageKey = publicConf.getString("lightmodeI18Key", "lightmode.timeline.notifications.html");
		final JsonArray list = new JsonArray();
		final JsonObject res = new JsonObject();
		final JsonObject first = new JsonObject();
		final String msg = I18n.getInstance().translate(messageKey, getHost(request), I18n.acceptLanguage(request));
		first.put("date", new JsonObject().put("$date", System.currentTimeMillis()));
		first.put("event-type", "");
		first.put("message", msg);
		first.put("preview", new JsonObject().put("text", msg).put("images", new JsonArray()));
		first.put("params", new JsonObject());
		first.put("recipients", new JsonArray());
		first.put("type", "");
		first.put("_id", System.currentTimeMillis()+"");
		list.add(first);
		res.put("results", list );
		res.put("number", 1);
		return res;
	}

	@Get("/timeline")
	@SecuredAction(value = "timeline.view", type = ActionType.AUTHENTICATED)
	public void view(HttpServerRequest request) {
		// =================
		// /!\ TEMPORARY /!\
		// =================
		// handle both new and old timeline app depending on user theme
		// if 2D then new timeline else default timeline
		if (this.skinLevels == null) {
			renderView(request, new JsonObject().put("lightMode", isLightmode()).put("cache", config.getBoolean("cache", false)));
			return;
		}

		UserUtils.getTheme(eb, request, this.hostSkin, userTheme -> {
			if (isEmpty(userTheme)) {
				renderView(request, new JsonObject().put("lightMode", isLightmode()).put("cache", config.getBoolean("cache", false)));
				return;
			}

			JsonArray userSkinLevels = this.skinLevels.getJsonArray(userTheme);
			if (userSkinLevels != null && userSkinLevels.contains("2d")) {
				renderView(request, new JsonObject().put("lightMode", isLightmode()).put("cache", config.getBoolean("cache", false)), "timeline2.html", null);
			} else {
				renderView(request, new JsonObject().put("lightMode", isLightmode()).put("cache", config.getBoolean("cache", false)));
			}
		});

	}

	@Get("/timeline2")
	@SecuredAction(value = "timeline.view", type = ActionType.AUTHENTICATED)
	public void view2(HttpServerRequest request) {
		final boolean cache = config.getBoolean("cache", false);
		renderView(request, new JsonObject().put("lightMode",isLightmode()).put("cache", cache));
	}

	@Get("/preferencesView")
	@SecuredAction(value = "timeline.preferencesView", type = ActionType.AUTHENTICATED)
	public void preferencesView(HttpServerRequest request) {
		renderView(request);
	}

	@Get("/historyView")
	@SecuredAction(value = "timeline.historyView")
	public void historyView(HttpServerRequest request) {
		final JsonObject publicConf =  config.getJsonObject("publicConf", new JsonObject());
		renderView(request, new JsonObject().put("lightMode",isLightmode()));
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
		final JsonObject i18Notif = new JsonObject(
			"{" + i18n.substring(0, i18n.length() - 1) + "}");
		if("true".equals(request.params().get("mergeall"))){
			final JsonObject original = this.i18n.load(request);
			renderJson(request, i18Notif.mergeIn(original));
		}else{
			renderJson(request, i18Notif);
		}
	}

	@Get("/registeredNotifications")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void registeredNotifications(HttpServerRequest request) {
		JsonArray reply = new fr.wseduc.webutils.collections.JsonArray();
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
		if(isLightmode()){
			renderJson(request, lightModeResult(request));
			return;
		}
		final boolean mine = request.params().contains("mine");
		final boolean both = request.params().contains("both");
		final String version = RequestUtils.acceptVersion(request);

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
								offset = PAGELIMIT * Integer.parseInt(page);
							} catch (NumberFormatException e) {}

							store.get(user, types, offset, PAGELIMIT, notifs.right().getValue(), mine, both, version, new Handler<JsonObject>() {
								public void handle(final JsonObject res) {
									if (res != null && "ok".equals(res.getString("status"))) {
										if ("2.0".equals(version)) {
											renderJson(request, res);
											return;
										}
										JsonArray results = res.getJsonArray("results", new fr.wseduc.webutils.collections.JsonArray());
										final JsonArray compiledResults = new fr.wseduc.webutils.collections.JsonArray();

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
											if (!(notifObj instanceof JsonObject)) continue;
											final JsonObject notif = (JsonObject) notifObj;
											if (!Utils.getOrElse(notif.getString("message"), "").isEmpty()) {
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
		if (eventTypes != null && !eventTypes.isEmpty()) {
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
	@MfaProtected()
	public void adminPage(final HttpServerRequest request) {
		renderView(request);
	}

	@Get("/admin-history")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	@MfaProtected()
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
	@MfaProtected()
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
				JsonArray reply = new fr.wseduc.webutils.collections.JsonArray();

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
	@MfaProtected()
	public void performDailyMailing(final HttpServerRequest request) {
		if(request.params().contains("forday")){
			try{
				final LocalDate ldate = LocalDate.parse(request.params().get("forday"));
				final Date date = Date.from(ldate.atStartOfDay(ZoneOffset.UTC).toInstant());
				mailerService.sendDailyMails(date, 0, defaultResponseHandler(request));
			}catch(Exception e){
				renderError(request, new JsonObject().put("message", e.getMessage()));
			}
		}else{
			mailerService.sendDailyMails(0, defaultResponseHandler(request));
		}
	}

	@Get("/performWeeklyMailing")
	@SecuredAction(type = ActionType.RESOURCE, value = "")
	@ResourceFilter(SuperAdminFilter.class)
	@MfaProtected()
	public void performWeeklyMailing(final HttpServerRequest request) {
		if(request.params().contains("forday")){
			try{
				final LocalDate ldate = LocalDate.parse(request.params().get("forday"));
				final Date date = Date.from(ldate.atStartOfDay(ZoneOffset.UTC).toInstant());
				mailerService.sendWeeklyMails(date, 0, defaultResponseHandler(request));
			}catch(Exception e){
				renderError(request, new JsonObject().put("message", e.getMessage()));
			}
		}else{
			mailerService.sendWeeklyMails(0, defaultResponseHandler(request));
		}
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
	@MfaProtected()
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
				final JsonArray compiledResults = new fr.wseduc.webutils.collections.JsonArray();

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
	@MfaProtected()
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
	@MfaProtected()
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


	@Get("/pushNotif/fcmTokens")
	@SecuredAction(value = "timeline.api", type = ActionType.AUTHENTICATED)
	public void getFcmTokens(final HttpServerRequest request){
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					NotificationUtils.getFcmTokensByUser(user.getUserId(), new Handler<Either<String,JsonArray>>() {
						@Override
						public void handle(Either<String, JsonArray> result) {
							if(result.isRight()){
								renderJson(request, result.right().getValue());
							} else {
								JsonObject error = new JsonObject()
										.put("error", result.left().getValue());
								renderJson(request, error, 400);
							}
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Put("/pushNotif/fcmToken")
	@SecuredAction(value = "timeline.api", type = ActionType.AUTHENTICATED)
	public void putFcmToken(final HttpServerRequest request){
		final String token= request.params().get("fcmToken");
		if(token != null && !token.trim().isEmpty()) {
			UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
				@Override
				public void handle(final UserInfos user) {
					if (user != null) {
						NotificationUtils.putFcmToken(user.getUserId(), token, new Handler<Either<String, JsonObject>>() {
							@Override
							public void handle(Either<String, JsonObject> result) {
								if (result.isRight()) {
									renderJson(request, result.right().getValue());
								} else {
									JsonObject error = new JsonObject()
											.put("error", result.left().getValue());
									renderJson(request, error, 400);
								}
							}
						});
					} else {
						unauthorized(request);
					}
				}
			});
		}else{
			badRequest(request);
		}
	}

	@Delete("/pushNotif/fcmToken")
	@SecuredAction(value = "timeline.api", type = ActionType.AUTHENTICATED)
	public void deleteFcmToken(final HttpServerRequest request){
		final String token= request.params().get("fcmToken");
		if(token != null && !token.trim().isEmpty()) {
			UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
				@Override
				public void handle(final UserInfos user) {
					if (user != null) {
						NotificationUtils.deleteFcmToken(user.getUserId(), token, new Handler<Either<String, JsonObject>>() {
							@Override
							public void handle(Either<String, JsonObject> result) {
								if (result.isRight()) {
									renderJson(request, result.right().getValue());
								} else {
									JsonObject error = new JsonObject()
											.put("error", result.left().getValue());
									renderJson(request, error, 400);
								}
							}
						});
					} else {
						unauthorized(request);
					}
				}
			});
		}else{
			badRequest(request);
		}
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
				this.removeMutersFromRecipientList(json)
				.onComplete(notificationResult -> {
					final JsonObject notification = notificationResult.succeeded() ? notificationResult.result() : json;
					store.add(notification, new Handler<JsonObject>() {
						public void handle(JsonObject result) {
							notificationHelper.sendImmediateNotifications(new JsonHttpServerRequest(notification.getJsonObject("request")), notification);
							handler.handle(result);
						}
					});
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
					json.getInteger("limit", PAGELIMIT), null, false, false, "", handler);
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

	/**
	 * <p>
	 * Retrieve users who muted this resource from the list by calling mute service and remove them from the notification.
	 * </p>
	 * <p>
	 *     <strong>NB : </strong> The parameter is modified.
	 * </p>
	 * @param originalNotification Original notification received from the notifier service whose recipients should be
	 *                             stripped from muters
	 * @return The same notification but whose recipients have been cleaned from users who didn't want to receive it.
	 */
	private Future<JsonObject> removeMutersFromRecipientList(final JsonObject originalNotification) {
			return muteHelper.fetResourceMutesByEntId(originalNotification.getString("resource"))
		.otherwise(emptySet())
		.map(muters -> {
			final JsonArray recipientsIds = originalNotification.getJsonArray("recipientsIds");
			final List<String> recipientsIdsWithoutMuters = recipientsIds.stream()
					.filter(recipient -> !muters.contains(recipient))
					.map(e -> (String)e)
					.collect(Collectors.toList());

			final JsonArray recipients = originalNotification.getJsonArray("recipients");
			final List<JsonObject> recipientsWithoutMuters = recipients.stream()
					.map(recipient -> (JsonObject)recipient)
					.filter(recipient -> !muters.contains(recipient.getString("userId")))
					.collect(Collectors.toList());
			originalNotification.put("recipientsIds", new JsonArray(recipientsIdsWithoutMuters));
			originalNotification.put("recipients", new JsonArray(recipientsWithoutMuters));
			return originalNotification;
		});
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
							restricted.put(notifType, new fr.wseduc.webutils.collections.JsonArray());
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

	public void setNotificationHelper(NotificationHelper notificationHelper) {
		this.notificationHelper = notificationHelper;
	}

	public void setEventsI18n(LocalMap<String, String> eventsI18n) {
		this.eventsI18n = eventsI18n;
	}

	public void setLazyEventsI18n(HashMap<String, JsonObject> lazyEventsI18n) {
		this.lazyEventsI18n = lazyEventsI18n;
	}
}
