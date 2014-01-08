package edu.one.core.common.notification;

import edu.one.core.infra.http.Renders;
import edu.one.core.common.user.UserInfos;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.file.FileProps;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.vertx.java.platform.Container;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TimelineHelper {

	private static final String TIMELINE_ADDRESS = "wse.timeline";
	private final static String messagesDir = "./i18n/timeline";
	private final EventBus eb;
	private final Renders render;
	private final Vertx vertx;
	private static final Logger log = LoggerFactory.getLogger(TimelineHelper.class);

	public TimelineHelper(Vertx vertx, EventBus eb, Container container) {
		this.eb = eb;
		this.render = new Renders(vertx, container);
		this.vertx = vertx;
		loadTimelineI18n();
	}

	public void notifyTimeline(HttpServerRequest request, UserInfos sender, String type, String eventType,
							   List<String> recipients, String resource, String template, JsonObject params) {
		notifyTimeline(request, sender, type, eventType, recipients, resource, null, template, params);
	}

	public void notifyTimeline(HttpServerRequest request, UserInfos sender, String type, final String eventType,
			List<String> recipients, String resource, String subResource, String template, JsonObject params) {
		JsonArray r = new JsonArray();
		for (String userId: recipients) {
			r.addObject(new JsonObject().putString("userId", userId).putNumber("unread", 1));
		}
		final JsonObject event = new JsonObject()
				.putString("action", "add")
				.putString("resource", resource)
				.putString("type", type)
				.putString("event-type", eventType)
				.putString("sender", sender.getUserId())
				.putArray("recipients", r);
		if (subResource != null && !subResource.trim().isEmpty()) {
			event.putString("sub-resource", subResource);
		}
		render.processTemplate(request, template, params, new Handler<String>() {
			@Override
			public void handle(String message) {
				if (message != null) {
					event.putString("message", message);
					eb.send(TIMELINE_ADDRESS, event);
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
					final Map<String, JsonObject> messages = new HashMap<>();
					vertx.fileSystem().readDir(messagesDir, new Handler<AsyncResult<String[]>>() {
						@Override
						public void handle(AsyncResult<String[]> ar) {
							if (ar.succeeded()) {
								final AtomicInteger count = new AtomicInteger(ar.result().length);
								for(final String path : ar.result()) {
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
												if (count.decrementAndGet() == 0) {
													appendTimelineEventsI18n(messages);
												}
											}
										}
									});
								}
							}
						}
					});
				} else {
					log.warn("I18n directory " + messagesDir + " doesn't exist.");
				}
			}
		});
	}

	private void appendTimelineEventsI18n(Map<String, JsonObject> i18ns) {
		ConcurrentMap<String, String> eventsI18n = vertx.sharedData().getMap("timelineEventsI18n");
		for (Map.Entry<String, JsonObject> e: i18ns.entrySet()) {
			String json = e.getValue().encode();
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
