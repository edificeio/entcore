/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.common.notification;

import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.http.Renders;

import org.entcore.common.email.EmailFactory;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.file.FileProps;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.vertx.java.platform.Container;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TimelineHelper {

	private static final String TIMELINE_ADDRESS = "wse.timeline";
	private static final String USERBOOK_ADDRESS = "userbook.preferences";
	private final static String messagesDir = "./i18n/timeline";
	private EmailSender emailSender;
	private final EventBus eb;
	private final Renders render;
	private final Vertx vertx;
	private final TimelineNotificationsLoader notificationsLoader;
	private static final Logger log = LoggerFactory.getLogger(TimelineHelper.class);

	public TimelineHelper(Vertx vertx, EventBus eb, Container container) {
		this.eb = eb;
		this.render = new Renders(vertx, container);
		this.vertx = vertx;
		this.notificationsLoader = TimelineNotificationsLoader.getInstance(vertx);
		loadTimelineI18n();
		EmailFactory emailFactory = new EmailFactory(vertx, container, container.config());
		emailSender = emailFactory.getSender();
	}

	public void notifyTimeline(HttpServerRequest request,  String notificationName,
			UserInfos sender, List<String> recipients, JsonObject params){
		notifyTimeline(request, notificationName, sender, recipients, null, null, params);
	}
	public void notifyTimeline(HttpServerRequest request,  String notificationName,
			UserInfos sender, List<String> recipients, String resource, JsonObject params){
		notifyTimeline(request, notificationName, sender, recipients, resource, null, params);
	}
	public void notifyTimeline(final HttpServerRequest request, final String notificationName,
			UserInfos sender, final List<String> recipients, String resource, String subResource, final JsonObject params){
		final JsonObject notification = notificationsLoader.getNotification(notificationName);

		JsonArray r = new JsonArray();
		for (String userId: recipients) {
			r.addObject(new JsonObject().putString("userId", userId).putNumber("unread", 1));
		}
		final JsonObject event = new JsonObject()
				.putString("action", "add")
				.putString("type", notification.getString("type"))
				.putString("event-type", notification.getString("event-type"))
				.putArray("recipients", r);
		if (resource != null) {
			event.putString("resource", resource);
		}
		if (sender != null) {
			event.putString("sender", sender.getUserId());
		}
		if (subResource != null && !subResource.trim().isEmpty()) {
			event.putString("sub-resource", subResource);
		}
		Long date = params.getLong("timeline-publish-date");
		if (date != null) {
			event.putObject("date", new JsonObject().putNumber("$date", date));
			params.removeField("timeline-publish-date");
		}
		event.putObject("params", params);
		eb.send(TIMELINE_ADDRESS, event, new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> event) {
				JsonObject result = event.body();
				if(!"error".equals(result.getString("status", "error"))){
					sendImmediateMails(request, notificationName, notification, params, recipients);
				}
			}
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
		JsonArray r = new JsonArray();
		for (String userId: recipients) {
			r.addObject(new JsonObject().putString("userId", userId).putNumber("unread", 1));
		}
		final JsonObject event = new JsonObject()
				.putString("action", "add")
				.putString("type", type)
				.putString("event-type", eventType)
				.putArray("recipients", r);
		if (resource != null) {
			event.putString("resource", resource);
		}
		if (sender != null) {
			event.putString("sender", sender.getUserId());
		}
		if (subResource != null && !subResource.trim().isEmpty()) {
			event.putString("sub-resource", subResource);
		}
		Long date = params.getLong("timeline-publish-date");
		if (date != null) {
			event.putObject("date", new JsonObject().putNumber("$date", date));
			params.removeField("timeline-publish-date");
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

	public void translateTimeline(JsonArray keys, String language, final Handler<JsonArray> handler){
		eb.send(TIMELINE_ADDRESS, new JsonObject()
				.putString("action", "translate-timeline")
				.putString("language", language)
				.putArray("i18nKeys", keys),
				new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> event) {
				handler.handle(event.body().getArray("translations", new JsonArray()));
			}
		});
	}

	public void processTimelineTemplate(JsonObject parameters, String notificationName, String template, final Handler<String> handler){
		eb.send(TIMELINE_ADDRESS, new JsonObject()
				.putString("action", "process-timeline-template")
				.putObject("request", new JsonObject())
				.putObject("parameters", parameters)
				.putString("resourceName", notificationName)
				.putString("template", template), new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> event) {
				handler.handle(event.body().getString("processedTemplate", ""));
			}
		});
	}

	public void getNotificationProperties(String notificationName, final Handler<JsonObject> handler){
		eb.send(TIMELINE_ADDRESS, new JsonObject()
				.putString("action", "get-notification-properties")
				.putString("key", notificationName), new Handler<Message<JsonObject>>() {
				public void handle(Message<JsonObject> event) {
					if(!"error".equals(event.body().getString("status", "error"))){
						handler.handle(event.body().getObject("result"));
					} else {
						handler.handle(null);
					}
				}
			}
		);
	}

	private void getUsersPreferences(JsonArray userIds, final Handler<JsonArray> handler){
		eb.send(USERBOOK_ADDRESS, new JsonObject()
			.putString("action", "get.userlist")
			.putString("application", "timeline")
			.putArray("userIds", userIds), new Handler<Message<JsonObject>>() {
				public void handle(Message<JsonObject> event) {
					if(!"error".equals(event.body().getString("status"))){
						handler.handle(event.body().getArray("results"));
					} else {
						handler.handle(null);
					}
				}
		});
	}

	private void sendImmediateMails(final HttpServerRequest request, final String notificationName, final JsonObject notification,
			final JsonObject templateParameters, final List<String> recipientIds){
		//Get notification properties (mixin : admin console configuration which overrides default properties)
		getNotificationProperties(notificationName, new Handler<JsonObject>() {
			public void handle(final JsonObject properties) {
				if(properties == null){
					log.error("[sendImmediateMails] Issue while retrieving notification (" + notificationName + ") properties.");
					return;
				}
				//Get users preferences (overrides notification properties)
				getUsersPreferences(new JsonArray(recipientIds.toArray()), new Handler<JsonArray>() {
					public void handle(final JsonArray userList) {
						if(userList == null){
							log.error("[sendImmediateMails] Issue while retrieving users preferences.");
						}
						//Process template once
						templateParameters.putString("innerTemplate", notification.getString("template", ""));
						processTimelineTemplate(templateParameters, "", "notifications/immediate-mail.html", new Handler<String>(){
							public void handle(final String processedTemplate) {
								final List<Object> to = new ArrayList<Object>();
								// For each user preference
								for(Object userObj : userList){
									JsonObject userPref = ((JsonObject) userObj);
									JsonObject notificationPreference = userPref
										.getObject("preferences", new JsonObject())
											.getObject("config", new JsonObject()
													.getObject(notificationName, new JsonObject()));
									// If the frequency is IMMEDIATE
									// and the restriction is not INTERNAL (timeline only)
									// and if the user has provided an email
									if(TimelineNotificationsLoader.Frequencies.IMMEDIATE.name().equals(
											notificationPreference.getString("defaultFrequency", properties.getString("defaultFrequency"))) &&
										!TimelineNotificationsLoader.Restrictions.INTERNAL.name().equals(
											notificationPreference.getString("restriction", properties.getString("restriction"))) &&
										userPref.getString("userMail") != null){
										to.add(userPref.getString("userMail"));
									}
								}

								// If there are receivers (users with the 'immediate mail' setting)
								if(to.size() > 0){
									//On completion : log
									final Handler<Message<JsonObject>> completionHandler = new Handler<Message<JsonObject>>(){
										public void handle(Message<JsonObject> event) {
											if("error".equals(event.body().getString("status", "error"))){
												log.error("[Timeline immediate emails] Error while sending mails : " + event.body());
											} else {
												log.debug("[Timeline immediate emails] Immediate mails sent.");
											}
										}

									};

									//Translate mail title
									//TODO : Server side default language
									JsonArray keys = new JsonArray()
										.add("timeline.immediate.mail.subject.header")
										.add(notificationName.toLowerCase());
									translateTimeline(keys, "fr", new Handler<JsonArray>() {
										public void handle(JsonArray translations) {
											//Send mail containing the "immediate" notification
											emailSender.sendEmail(request,
												to,
												null,
												null,
												translations.get(0).toString() + translations.get(1).toString(),
												processedTemplate,
												null,
												false,
												completionHandler);
										}
									});
								}

							}
						});
					}
				});
			}
		});
	}

}
