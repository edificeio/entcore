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

package org.entcore.common.notification;

import fr.wseduc.webutils.Utils;
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.http.Renders;

import io.vertx.core.shareddata.LocalMap;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.StringUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.FileProps;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class TimelineHelper {

	private static final String TIMELINE_ADDRESS = "wse.timeline";
	private final static String messagesDir = FileResolver.absolutePath("i18n/timeline");
	private final EventBus eb;
	private final Renders render;
	private final Vertx vertx;
	private final JsonObject config;
	private final TimelineNotificationsLoader notificationsLoader;
	private static final Logger log = LoggerFactory.getLogger(TimelineHelper.class);

	public TimelineHelper(Vertx vertx, EventBus eb, JsonObject config) {
		this.eb = eb;
		this.render = new Renders(vertx, config);
		this.vertx = vertx;
		this.config = config;
		this.notificationsLoader = TimelineNotificationsLoader.getInstance(vertx);
		loadTimelineI18n();
		loadAssetsTimelineDirectory();
	}

	public void notifyTimeline(HttpServerRequest request,  String notificationName,
			UserInfos sender, List<String> recipients, JsonObject params){
		notifyTimeline(request, notificationName, sender, recipients, null, null, params);
	}
	public void notifyTimeline(HttpServerRequest request,  String notificationName,
			UserInfos sender, List<String> recipients, String resource, JsonObject params){
		notifyTimeline(request, notificationName, sender, recipients, resource, null, params);
	}
	public void notifyTimeline(HttpServerRequest request, final String notificationName,
							   UserInfos sender, final List<String> recipients, String resource, String subResource, final JsonObject params) {
		notifyTimeline(request, notificationName, sender, recipients, resource, subResource, params, false);
	}
	public void notifyTimeline(final HttpServerRequest req, final String notificationName, UserInfos sender,
			final List<String> recipients, String resource, String subResource, final JsonObject params, final boolean disableAntiFlood){
		notifyTimeline(req, notificationName, sender, recipients, resource, subResource, params, disableAntiFlood, null);
	}
	public void notifyTimeline(final HttpServerRequest req, final String notificationName,
			UserInfos sender, final List<String> recipients, String resource, String subResource, final JsonObject params, final boolean disableAntiFlood, JsonObject preview){
		notificationsLoader.getNotification(notificationName, notification -> {
			JsonArray r = new fr.wseduc.webutils.collections.JsonArray();
			for (String userId: recipients) {
				r.add(new JsonObject().put("userId", userId).put("unread", 1));
			}
			final JsonObject event = new JsonObject()
					.put("action", "add")
					.put("type", notification.getString("type"))
					.put("event-type", notification.getString("event-type"))
					.put("recipients", r)
					.put("recipientsIds", new fr.wseduc.webutils.collections.JsonArray(recipients));
			if (resource != null) {
				event.put("resource", resource);
			}
			if (sender != null) {
				event.put("sender", sender.getUserId());
			}
			if (subResource != null && !subResource.trim().isEmpty()) {
				event.put("sub-resource", subResource);
			}
			if (disableAntiFlood || params.getBoolean("disableAntiFlood", false)) {
				event.put("disableAntiFlood", true);
				if (params.containsKey("disableAntiFlood")) {
					params.remove("disableAntiFlood");
				}
			}
			if (preview != null) {
				event.put("preview", preview);
			}
			Long date = params.getLong("timeline-publish-date");
			if (date != null) {
				event.put("date", new JsonObject().put("$date", date));
				params.remove("timeline-publish-date");
			}

			event.put("pushNotif", params.remove("pushNotif"));
			event.put("disableMailNotification", params.remove("disableMailNotification"));

			HttpServerRequest request;
			if (req == null) {
				request = new JsonHttpServerRequest(new JsonObject());
			} else {
				request = req;
			}
			event.put("params", params)
				.put("notificationName", notificationName)
				.put("notification", notification)
				.put("request", new JsonObject().put("headers",
						new JsonObject()
								.put("Host", Renders.getHost(request))
								.put("X-Forwarded-Proto", Renders.getScheme(request))
								.put("Accept-Language", request.headers().get("Accept-Language"))
				));
			eb.request(TIMELINE_ADDRESS, event, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
				public void handle(Message<JsonObject> event) {
					JsonObject result = event.body();
					if("error".equals(result.getString("status", "error"))){
						log.error("Error in timeline notification : " + result.getString("message"));
					}
				}
			}));
		});
	}

	/**
	 * @deprecated
	 * Notification system was refactored in version 1.16.1
	 */
	@Deprecated
	public void notifyTimeline(HttpServerRequest request, UserInfos sender, String type, String eventType,
							   List<String> recipients, String resource, String template, JsonObject params) {
		notifyTimeline(request, sender, type, eventType, recipients, resource, null, template, params);
	}
	/**
	 * @deprecated
	 * Notification system was refactored in version 1.16.1
	 */
	@Deprecated
	public void notifyTimeline(HttpServerRequest request, UserInfos sender, String type, final String eventType,
			List<String> recipients, String resource, String subResource, String template, JsonObject params) {
		JsonArray r = new fr.wseduc.webutils.collections.JsonArray();
		for (String userId: recipients) {
			r.add(new JsonObject().put("userId", userId).put("unread", 1));
		}
		final JsonObject event = new JsonObject()
				.put("action", "add")
				.put("type", type)
				.put("event-type", eventType)
				.put("recipients", r);
		if (resource != null) {
			event.put("resource", resource);
		}
		if (sender != null) {
			event.put("sender", sender.getUserId());
		}
		if (subResource != null && !subResource.trim().isEmpty()) {
			event.put("sub-resource", subResource);
		}
		Long date = params.getLong("timeline-publish-date");
		if (date != null) {
			event.put("date", new JsonObject().put("$date", date));
			params.remove("timeline-publish-date");
		}
		render.processTemplate(request, template, params, new Handler<String>() {
			@Override
			public void handle(String message) {
				if (message != null) {
					event.put("message", message);
					eb.request(TIMELINE_ADDRESS, event);
				} else {
					log.error("Unable to send timeline " + eventType + " notification.");
				}
			}
		});
	}


	private void loadTimelineI18n() {
		vertx.fileSystem().exists(messagesDir, new Handler<AsyncResult<Boolean>>() {
			@Override
			public void handle(AsyncResult<Boolean> ar) {
				if (ar.succeeded() && ar.result()) {
					vertx.fileSystem().readDir(messagesDir, new Handler<AsyncResult<List<String>>>() {
						@Override
						public void handle(AsyncResult<List<String>> ar) {
							if (ar.succeeded()) {
								readI18nTimeline(ar);
							}
						}
					});
				} else {
					log.warn("I18n directory " + messagesDir + " doesn't exist.");
				}
			}
		});
	}

	private void readI18nTimeline(AsyncResult<List<String>> ar) {
		final Map<String, JsonObject> messages = new HashMap<>();
		final AtomicInteger count = new AtomicInteger(ar.result().size());
		for(final String path : ar.result()) {
			log.info("... load i18n from " + path);
			vertx.fileSystem().props(path, new Handler<AsyncResult<FileProps>>() {
				@Override
				public void handle(AsyncResult<FileProps> ar) {
					if (ar.succeeded() && ar.result().isRegularFile()) {
						final String k = new File(path).getName().split("\\.")[0];
						vertx.fileSystem().readFile(path, new Handler<AsyncResult<Buffer>>() {
							@Override
							public void handle(AsyncResult<Buffer> ar) {
								if (ar.succeeded()) {
									JsonObject jo = new JsonObject(
											ar.result().toString("UTF-8"));
									messages.put(k, jo);
								}
								if (count.decrementAndGet() == 0) {
									appendTimelineEventsI18n(messages);
								}
							}
						});
					} else {
						log.info("... Unable to load i18n from " + path);
						if (count.decrementAndGet() == 0) {
							appendTimelineEventsI18n(messages);
						}
					}
				}
			});
		}
	}

	private void loadAssetsTimelineDirectory() {
		final String[] app = Utils.loadFromResource("mod.json").getString("main").split("\\.");
		final String assetsDirectory = config.getString("assets-path", "../..") + File.separator + "assets";
		final String i18nDirectory = assetsDirectory + File.separator + "i18n" + File.separator + app[app.length - 1] +
				File.separator + "timeline";
		final String i18nDirectory2 = assetsDirectory + File.separator + "i18n" + File.separator + "Timeline";
		loadAssetsTimelineDirectory(i18nDirectory);
		loadAssetsTimelineDirectory(i18nDirectory2);
	}

	private void loadAssetsTimelineDirectory(String i18nDirectory) {
		vertx.fileSystem().exists(i18nDirectory, new Handler<AsyncResult<Boolean>>() {
			@Override
			public void handle(AsyncResult<Boolean> ar) {
				if (ar.succeeded() && ar.result()) {
					vertx.fileSystem().readDir(i18nDirectory, new Handler<AsyncResult<List<String>>>() {
						@Override
						public void handle(AsyncResult<List<String>> asyncResult) {
							if (asyncResult.succeeded()) {
								readI18nTimeline(asyncResult);
							} else {
								log.error("Error loading assets i18n timeline.", asyncResult.cause());
							}
						}
					});
				} else if (ar.failed()) {
					log.error("Error loading assets i18n timeline.", ar.cause());
				}
			}
		});
	}

	private void appendTimelineEventsI18n(Map<String, JsonObject> i18ns) {
		LocalMap<String, String> eventsI18n = vertx.sharedData().getLocalMap("timelineEventsI18n");
		for (Map.Entry<String, JsonObject> e: i18ns.entrySet()) {
			String json = e.getValue().encode();
			if (StringUtils.isEmpty(json) || "{}".equals(StringUtils.stripSpaces(json))) continue;
			String j = json.substring(1, json.length() - 1) + ",";
			String resJson = j;
			String oldJson = eventsI18n.putIfAbsent(e.getKey(), j);
			if (oldJson != null && !oldJson.equals(j)) {
				resJson += oldJson;
				boolean appended = eventsI18n.replace(e.getKey(), oldJson, resJson);
				while (!appended) {
					oldJson = eventsI18n.get(e.getKey());
					resJson = j;
					resJson += oldJson;
					appended = eventsI18n.replace(e.getKey(), oldJson, resJson);
				}
			}
		}
	}

}
