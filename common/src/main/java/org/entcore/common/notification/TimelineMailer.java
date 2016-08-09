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

package org.entcore.common.notification;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.vertx.java.platform.Container;

import com.mongodb.QueryBuilder;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.email.EmailSender;

public class TimelineMailer {

	private static final String TIMELINE_ADDRESS = "wse.timeline";
	private static final String USERBOOK_ADDRESS = "userbook.preferences";
	private final EventBus eb;
	private final Vertx vertx;
	private final EmailSender emailSender;
	private final Neo4j neo4j = Neo4j.getInstance();
	private final MongoDb mongo = MongoDb.getInstance();
	private static final Logger log = LoggerFactory.getLogger(TimelineMailer.class);

	private final int USERS_LIMIT;

	public TimelineMailer(Vertx vertx, EventBus eb, Container container){
		this(vertx, eb, container, 10);
	}
	public TimelineMailer(Vertx vertx, EventBus eb, Container container, int usersLimit){
		this.vertx = vertx;
		this.eb = eb;
		this.USERS_LIMIT = usersLimit;
		EmailFactory emailFactory = new EmailFactory(this.vertx, container, container.config());
		emailSender = emailFactory.getSender();
	}

	/**
	 * Translates a key using the usual i18n keys + the timeline folders keys (from all apps).
	 *
	 * @param keys : Keys to translate
	 * @param domain : Domain of the i18n files
	 * @param language : Language
	 * @param handler : Handles a JsonArray containing translated Strings.
	 */
	private void translateTimeline(JsonArray keys, String domain,
			String language, final Handler<JsonArray> handler){
		eb.send(TIMELINE_ADDRESS, new JsonObject()
				.putString("action", "translate-timeline")
				.putString("language", language)
				.putString("domain", domain)
				.putArray("i18nKeys", keys),
				new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> event) {
				handler.handle(event.body().getArray("translations", new JsonArray()));
			}
		});
	}

	/**
	 * Processes a mustache template using lambdas defined in the Timeline module.
	 *
	 * @param parameters : Template parameters
	 * @param notificationName : Name of the notification (mostly useless)
	 * @param template : Template contents
	 * @param domain : Domain of the i18n files
	 * @param handler : Handles the processed template
	 */
	private void processTimelineTemplate(JsonObject parameters, String notificationName,
			String template, String domain, String scheme, final Handler<String> handler){
		eb.send(TIMELINE_ADDRESS, new JsonObject()
				.putString("action", "process-timeline-template")
				.putObject("request", new JsonObject()
					.putObject("headers", new JsonObject()
							.putString("Host", domain)
							.putString("X-Forwarded-Proto", scheme)))
				.putObject("parameters", parameters)
				.putString("resourceName", notificationName)
				.putString("template", template), new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> event) {
				handler.handle(event.body().getString("processedTemplate", ""));
			}
		});
	}

	/**
	 * Retrieves stored properties for a single notification.
	 *
	 * @param notificationName : Name of the notification
	 * @param handler : Handles the properties
	 */
	private void getNotificationProperties(String notificationName, final Handler<JsonObject> handler){
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

	/**
	 * Retrieves default properties for all notifications.
	 * @param handler : Handles the properties
	 */
	private void getNotificationsDefaults(final Handler<JsonArray> handler){
		eb.send(TIMELINE_ADDRESS, new JsonObject()
				.putString("action", "list-notifications-defaults"), new Handler<Message<JsonObject>>() {
				public void handle(Message<JsonObject> event) {
					if(!"error".equals(event.body().getString("status", "error"))){
						handler.handle(event.body().getArray("results"));
					} else {
						handler.handle(null);
					}
				}
			}
		);
	}

	/**
	 * Retrieves stored timeline user preferences.
	 *
	 * @param userIds : Ids of the users
	 * @param handler : Handles the preferences
	 */
	private void getUsersPreferences(JsonArray userIds, final Handler<JsonArray> handler){
		eb.send(USERBOOK_ADDRESS, new JsonObject()
			.putString("action", "get.userlist")
			.putString("application", "timeline")
			.putString("additionalMatch", ", u-[:IN]->(g:Group)-[:AUTHORIZED]-(r:Role)-[:AUTHORIZE]->(act:WorkflowAction) ")
			.putString("additionalWhere", "AND act.name = \"org.entcore.timeline.controllers.TimelineController|mixinConfig\" ")
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

	/**
	 * Retrieves users having an email address, paginated.
	 *
	 * @param page : Page number
	 * @param handler : Handles the users
	 */
	private void getImpactedUsers(int page, final Handler<Either<String, JsonArray>> handler){
		String query =
			"MATCH (u:User), u-[:IN]->(g:Group)-[:AUTHORIZED]-(r:Role)-[:AUTHORIZE]->(act:WorkflowAction) " +
			"WHERE u.activationCode IS NULL AND u.email IS NOT NULL AND length(u.email) > 0 " +
			"AND act.name = \"org.entcore.timeline.controllers.TimelineController|mixinConfig\"" +
			"RETURN DISTINCT u.email as mail, u.id as id " +
			"SKIP {skip} LIMIT {limit}";
		JsonObject params = new JsonObject()
			.putNumber("skip", page * USERS_LIMIT)
			.putNumber("limit", USERS_LIMIT);
		neo4j.execute(query, params, Neo4jResult.validResultHandler(handler));
	}

	/**
	 * Retrieves all timeline notifications from mongodb for a single user, from a specific date in the past.
	 *
	 * @param userId : Userid
	 * @param from : The starting date
	 * @param handler : Handles the notifications
	 */
	private void getUserNotifications(String userId, Date from, final Handler<JsonArray> handler){
		JsonObject matcher = MongoQueryBuilder.build(
				QueryBuilder
					.start("recipients").elemMatch(QueryBuilder.start("userId").is(userId).get())
					.and("date").greaterThanEquals(from));

		mongo.find("timeline", matcher, new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> event) {
				if("error".equals(event.body().getString("status", "error"))){
					handler.handle(new JsonArray());
				} else {
					handler.handle(event.body().getArray("results"));
				}
			}
		});
	}

	/**
	 * Retrieves an aggregated list of notifications from mongodb for a single user.
	 *
	 *  Notifications are grouped by type & event-type.
	 * @param userId : Userid
	 * @param from : Starting date in the past
	 * @param handler: Handles the notifications
	 */
	private void getAggregatedUserNotifications(String userId, Date from, final Handler<JsonArray> handler){
		final JsonObject aggregation = new JsonObject();
		JsonArray pipeline = new JsonArray();
		aggregation
			.putString("aggregate", "timeline")
			.putBoolean("allowDiskUse", true)
			.putArray("pipeline", pipeline);

		JsonObject matcher = MongoQueryBuilder.build(
			QueryBuilder
				.start("recipients").elemMatch(QueryBuilder.start("userId").is(userId).get())
				.and("date").greaterThanEquals(from));
		JsonObject grouper = new JsonObject("{ \"_id\" : { \"type\": \"$type\", \"event-type\": \"$event-type\"}, \"count\": { \"$sum\": 1 } }");
		JsonObject transformer = new JsonObject("{ \"type\": \"$_id.type\", \"event-type\": \"$_id.event-type\", \"count\": 1, \"_id\": 0 }");

		pipeline.addObject(new JsonObject().putObject("$match", matcher));
		pipeline.add(new JsonObject().putObject("$group", grouper));
		pipeline.add(new JsonObject().putObject("$project", transformer));

		mongo.command(aggregation.toString(), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if("error".equals(event.body().getString("status", "error"))){
					handler.handle(new JsonArray());
				} else {
					handler.handle(
						event.body().getObject("result", new JsonObject())
							.getArray("result", new JsonArray()));
				}
			}

		});
	}

	/**
	 * Returns either user preferences or defaults when the user has not chosen specific values.
	 *
	 * @param field : Which preference
	 * @param userPrefs : User preferences
	 * @param defaultPrefs : Default preferences
	 * @return The prevailing preference
	 */
	private String notificationPrefsMixin(String field, JsonObject userPrefs, JsonObject defaultPrefs){
		return userPrefs.getString(field, defaultPrefs.getString(field, ""));
	}

	/**
	 * Sends immediate notification emails for users that are concerned.
	 *
	 * @param request : Request initiating the notification.
	 * @param notificationName : Name of the notification
	 * @param notification : Notification properties
	 * @param templateParameters : Notification parameters
	 * @param recipientIds : Recipients of the notification
	 */
	public void sendImmediateMails(final HttpServerRequest request, final String notificationName, final JsonObject notification,
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
						//Process template once by domain
						final Map<String, String> processedTemplates = new HashMap<String, String>();
						templateParameters.putString("innerTemplate", notification.getString("template", ""));

						final Map<String, List<Object>> toDomainMap = new HashMap<>();

						final AtomicInteger userCount = new AtomicInteger(userList.size());
						final VoidHandler templatesHandler = new VoidHandler(){
							protected void handle() {
								if(userCount.decrementAndGet() == 0){
									if(toDomainMap.size() > 0){
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

										JsonArray keys = new JsonArray()
											.add("timeline.immediate.mail.subject.header")
											.add(notificationName.toLowerCase());

										for(final String domain : toDomainMap.keySet()){
											translateTimeline(keys, domain, "fr", new Handler<JsonArray>() {
												public void handle(JsonArray translations) {
													//Send mail containing the "immediate" notification
													emailSender.sendEmail(request,
														toDomainMap.get(domain),
														null,
														null,
														translations.get(0).toString() + translations.get(1).toString(),
														processedTemplates.get(domain),
														null,
														false,
														completionHandler);
												}
											});
										}
									}
								}
							}
						};

						for(Object userObj : userList){
							final JsonObject userPref = ((JsonObject) userObj);
							final String userDomain = userPref.getString("lastDomain", I18n.DEFAULT_DOMAIN);
							final String userScheme = userPref.getString("lastScheme", "http");
							if(!processedTemplates.containsKey(userDomain)){
								processTimelineTemplate(templateParameters, "", "notifications/immediate-mail.html",
									userDomain, userScheme, new Handler<String>(){
										public void handle(String processedTemplate) {
											processedTemplates.put(userDomain, processedTemplate);
											templatesHandler.handle(null);
										}
								});
							} else {
								templatesHandler.handle(null);
							}
							JsonObject notificationPreference = userPref
									.getObject("preferences", new JsonObject())
										.getObject("config", new JsonObject())
											.getObject(notificationName, new JsonObject());
								// If the frequency is IMMEDIATE
								// and the restriction is not INTERNAL (timeline only)
								// and if the user has provided an email
								if(TimelineNotificationsLoader.Frequencies.IMMEDIATE.name().equals(
										notificationPreference.getString("defaultFrequency", properties.getString("defaultFrequency"))) &&
									!TimelineNotificationsLoader.Restrictions.INTERNAL.name().equals(
										notificationPreference.getString("restriction", properties.getString("restriction"))) &&
									!TimelineNotificationsLoader.Restrictions.HIDDEN.name().equals(
											notificationPreference.getString("restriction", properties.getString("restriction"))) &&
									userPref.getString("userMail") != null && !userPref.getString("userMail").trim().isEmpty()){
									if(!toDomainMap.containsKey(userDomain)){
										toDomainMap.put(userDomain, new ArrayList<Object>());
									}
									toDomainMap.get(userDomain).add(userPref.getString("userMail"));
								}
						}
					}
				});
			}
		});
	}

	/**
	 * Send daily notification emails for all users.
	 *
	 * @param dayDelta : When to aggregate, delta from now.
	 * @param handler : Handles the results, emails sent / users KO
	 */
	public void sendDailyMails(int dayDelta, final Handler<Either<String, JsonObject>> handler){

		final HttpServerRequest request = new JsonHttpServerRequest(new JsonObject());
		final AtomicInteger userPagination = new AtomicInteger(0);
		final Calendar dayDate = Calendar.getInstance();
		dayDate.add(Calendar.DAY_OF_MONTH, dayDelta);
		dayDate.set(Calendar.HOUR_OF_DAY, 0);
		dayDate.set(Calendar.MINUTE, 0);
		dayDate.set(Calendar.SECOND, 0);
		dayDate.set(Calendar.MILLISECOND, 0);

		final JsonObject results = new JsonObject()
			.putNumber("mails.sent", 0)
			.putNumber("users.ko", 0);
		final JsonObject notificationsDefaults = new JsonObject();

		final Handler<Boolean> userContinuationHandler = new Handler<Boolean>() {

			private final Handler<Boolean> continuation = this;
			private final Handler<JsonArray> usersHandler = new Handler<JsonArray>() {
				public void handle(final JsonArray users) {
					final int nbUsers = users.size();
					if(nbUsers == 0){
						continuation.handle(false);
						return;
					}
					final AtomicInteger usersCountdown = new AtomicInteger(nbUsers);

					final VoidHandler usersEndHandler = new VoidHandler() {
						protected void handle() {
							if(usersCountdown.decrementAndGet() <= 0){
								continuation.handle(nbUsers == USERS_LIMIT);
							}
						}
					};

					final JsonArray userIds = new JsonArray();
					for(Object userObj : users)
						userIds.add(((JsonObject) userObj).getString("id", ""));
					getUsersPreferences(userIds, new Handler<JsonArray>(){
						public void handle(JsonArray preferences) {
							for(Object userObj : preferences){
								final JsonObject userPrefs = (JsonObject) userObj;
								final String userDomain = userPrefs.getString("lastDomain", I18n.DEFAULT_DOMAIN);
								final String userScheme = userPrefs.getString("lastScheme", "http");

								getUserNotifications(userPrefs.getString("userId", ""), dayDate.getTime(), new Handler<JsonArray>(){
									public void handle(JsonArray notifications) {
										if(notifications.size() == 0){
											usersEndHandler.handle(null);
											return;
										}

										/* TODO : Locale */
										SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss", Locale.FRANCE);
										final JsonArray dates = new JsonArray();
										final JsonArray templates = new JsonArray();

										for(Object notificationObj : notifications){
											JsonObject notification = (JsonObject) notificationObj;
											final String notificationName =
												notification.getString("type","").toLowerCase() + "." +
												notification.getString("event-type", "").toLowerCase();
											if(notificationsDefaults.getObject(notificationName) == null)
												continue;

											JsonObject notificationPreference = userPrefs
													.getObject("preferences", new JsonObject())
														.getObject("config", new JsonObject())
															.getObject(notificationName, new JsonObject());
											if(TimelineNotificationsLoader.Frequencies.DAILY.name().equals(
													notificationPrefsMixin("defaultFrequency", notificationPreference, notificationsDefaults.getObject(notificationName))) &&
												!TimelineNotificationsLoader.Restrictions.INTERNAL.name().equals(
													notificationPrefsMixin("restriction", notificationPreference, notificationsDefaults.getObject(notificationName))) &&
												!TimelineNotificationsLoader.Restrictions.HIDDEN.name().equals(
													notificationPrefsMixin("restriction", notificationPreference, notificationsDefaults.getObject(notificationName)))){
												templates.add(new JsonObject()
													.putString("template", notificationsDefaults.getObject(notificationName, new JsonObject()).getString("template", ""))
													.putObject("params", notification.getObject("params", new JsonObject())));
												dates.add(formatter.format(MongoDb.parseIsoDate(notification.getObject("date"))));
											}
										}
										if(templates.size() > 0){
											JsonObject templateParams = new JsonObject()
												.putArray("nestedTemplatesArray", templates)
												.putArray("notificationDates", dates);
											processTimelineTemplate(templateParams, "", "notifications/daily-mail.html",
													userDomain, userScheme, new Handler<String>() {
												public void handle(final String processedTemplate) {
													//On completion : log
													final Handler<Message<JsonObject>> completionHandler = new Handler<Message<JsonObject>>(){
														public void handle(Message<JsonObject> event) {
															if("error".equals(event.body().getString("status", "error"))){
																log.error("[Timeline daily emails] Error while sending mail : " + event.body());
																results.putNumber("users.ko", results.getInteger("users.ko") + 1);
															} else {
																results.putNumber("mails.sent", results.getInteger("mails.sent") + 1);
															}
															usersEndHandler.handle(null);
														}

													};

													//Translate mail title
													//TODO : Server side default language
													JsonArray keys = new JsonArray()
														.add("timeline.daily.mail.subject.header");
													translateTimeline(keys, userDomain, "fr", new Handler<JsonArray>() {
														public void handle(JsonArray translations) {
															//Send mail containing the "daily" notifications
															emailSender.sendEmail(request,
																userPrefs.getString("userMail", ""),
																null,
																null,
																translations.get(0).toString(),
																processedTemplate,
																null,
																false,
																completionHandler);
														}
													});
												}
											});
										} else {
											usersEndHandler.handle(null);
										}

									}
								});
							}
						}
					});
				}
			};

			public void handle(Boolean continuation) {
				if(continuation){
					getImpactedUsers(userPagination.getAndIncrement(), new Handler<Either<String,JsonArray>>() {
						public void handle(Either<String, JsonArray> event) {
							if(event.isLeft()){
								log.error("[sendDailyMails] Error while retrieving impacted users : " + event.left().getValue());
								handler.handle(new Either.Left<String, JsonObject>(event.left().getValue()));
							} else {
								JsonArray users = event.right().getValue();
								usersHandler.handle(users);
							}
						}
					});
				} else {
					handler.handle(new Either.Right<String, JsonObject>(results));
				}
			}
		};

		getNotificationsDefaults(new Handler<JsonArray>() {
			public void handle(final JsonArray notifications) {
				if(notifications == null){
					log.error("[sendDailyMails] Error while retrieving notifications defaults.");
					return;
				} else {
					for(Object notifObj: notifications){
						final JsonObject notif = (JsonObject) notifObj;
						notificationsDefaults.putObject(notif.getString("key", ""), notif);
					}
					userContinuationHandler.handle(true);
				}
			}
		});
	}

	/**
	 * Sends weekly notification emails for all users.
	 *
	 * @param dayDelta : Day from which to aggregate, delta from now.
	 * @param handler : Handles the results, emails sent / users KO
	 */
	public void sendWeeklyMails(int dayDelta, final Handler<Either<String, JsonObject>> handler){

		final HttpServerRequest request = new JsonHttpServerRequest(new JsonObject());
		final AtomicInteger userPagination = new AtomicInteger(0);
		final Calendar weekDate = Calendar.getInstance();
		weekDate.add(Calendar.DAY_OF_MONTH, dayDelta - 6);
		weekDate.set(Calendar.HOUR_OF_DAY, 0);
		weekDate.set(Calendar.MINUTE, 0);
		weekDate.set(Calendar.SECOND, 0);
		weekDate.set(Calendar.MILLISECOND, 0);

		final JsonObject results = new JsonObject()
			.putNumber("mails.sent", 0)
			.putNumber("users.ko", 0);
		final JsonObject notificationsDefaults = new JsonObject();

		final Handler<Boolean> userContinuationHandler = new Handler<Boolean>() {

			private final Handler<Boolean> continuation = this;
			private final Handler<JsonArray> usersHandler = new Handler<JsonArray>() {
				public void handle(final JsonArray users) {
					final int nbUsers = users.size();
					if(nbUsers == 0){
						continuation.handle(false);
						return;
					}
					final AtomicInteger usersCountdown = new AtomicInteger(nbUsers);

					final VoidHandler usersEndHandler = new VoidHandler() {
						protected void handle() {
							if(usersCountdown.decrementAndGet() <= 0){
								continuation.handle(nbUsers == USERS_LIMIT);
							}
						}
					};

					final JsonArray userIds = new JsonArray();
					for(Object userObj : users)
						userIds.add(((JsonObject) userObj).getString("id", ""));
					getUsersPreferences(userIds, new Handler<JsonArray>(){
						public void handle(JsonArray preferences) {
							for(Object userObj : preferences){
								final JsonObject userPrefs = (JsonObject) userObj;
								final String userDomain = userPrefs.getString("lastDomain", I18n.DEFAULT_DOMAIN);
								final String userScheme = userPrefs.getString("lastScheme", "http");

								getAggregatedUserNotifications(userPrefs.getString("userId", ""), weekDate.getTime(), new Handler<JsonArray>(){
									public void handle(JsonArray notifications) {
										if(notifications.size() == 0){
											usersEndHandler.handle(null);
											return;
										}

										final JsonArray weeklyNotifications = new JsonArray();

										for(Object notificationObj : notifications){
											JsonObject notification = (JsonObject) notificationObj;
											final String notificationName =
												notification.getString("type","").toLowerCase() + "." +
												notification.getString("event-type", "").toLowerCase();
											if(notificationsDefaults.getObject(notificationName) == null)
												continue;

											JsonObject notificationPreference = userPrefs
													.getObject("preferences", new JsonObject())
														.getObject("config", new JsonObject())
																.getObject(notificationName, new JsonObject());
											if(TimelineNotificationsLoader.Frequencies.WEEKLY.name().equals(
													notificationPrefsMixin("defaultFrequency", notificationPreference, notificationsDefaults.getObject(notificationName))) &&
												!TimelineNotificationsLoader.Restrictions.INTERNAL.name().equals(
													notificationPrefsMixin("restriction", notificationPreference, notificationsDefaults.getObject(notificationName))) &&
												!TimelineNotificationsLoader.Restrictions.HIDDEN.name().equals(
														notificationPrefsMixin("restriction", notificationPreference, notificationsDefaults.getObject(notificationName)))){
												notification.putString("notificationName", notificationName);
												weeklyNotifications.add(notification);
											}
										}

										final JsonObject weeklyNotificationsObj = new JsonObject();
										final JsonArray weeklyNotificationsGroupedArray = new JsonArray();
										for(Object notif : weeklyNotifications){
											JsonObject notification = (JsonObject) notif;
											if(!weeklyNotificationsObj.containsField(notification.getString("type").toLowerCase()))
												weeklyNotificationsObj.putObject(notification.getString("type").toLowerCase(), new JsonObject()
													.putString("link", notificationsDefaults
															.getObject(notification.getString("notificationName")).getString("app-address", ""))
													.putArray("event-types", new JsonArray()));
											weeklyNotificationsObj
												.getObject(notification.getString("type").toLowerCase())
													.getArray(("event-types"), new JsonArray())
														.add(notification);
										}

										for(String key : weeklyNotificationsObj.toMap().keySet()){
											weeklyNotificationsGroupedArray.add(new JsonObject()
												.putString("type", key)
												.putString("link", weeklyNotificationsObj.getObject(key).getString("link", ""))
												.putArray("event-types", weeklyNotificationsObj.getObject(key).getArray("event-types")));
										}

										if(weeklyNotifications.size() > 0){
											JsonObject templateParams = new JsonObject().putArray("notifications", weeklyNotificationsGroupedArray);
											processTimelineTemplate(templateParams, "", "notifications/weekly-mail.html",
													userDomain, userScheme, new Handler<String>() {
												public void handle(final String processedTemplate) {
													//On completion : log
													final Handler<Message<JsonObject>> completionHandler = new Handler<Message<JsonObject>>(){
														public void handle(Message<JsonObject> event) {
															if("error".equals(event.body().getString("status", "error"))){
																log.error("[Timeline weekly emails] Error while sending mail : " + event.body());
																results.putNumber("users.ko", results.getInteger("users.ko") + 1);
															} else {
																results.putNumber("mails.sent", results.getInteger("mails.sent") + 1);
															}
															usersEndHandler.handle(null);
														}

													};

													//Translate mail title
													//TODO : Server side default language
													JsonArray keys = new JsonArray()
														.add("timeline.weekly.mail.subject.header");
													translateTimeline(keys, userDomain, "fr", new Handler<JsonArray>() {
														public void handle(JsonArray translations) {
															//Send mail containing the "weekly" notifications
															emailSender.sendEmail(request,
																userPrefs.getString("userMail", ""),
																null,
																null,
																translations.get(0).toString(),
																processedTemplate,
																null,
																false,
																completionHandler);
														}
													});
												}
											});
										} else {
											usersEndHandler.handle(null);
										}

									}
								});
							}
						}
					});
				}
			};

			public void handle(Boolean continuation) {
				if(continuation){
					getImpactedUsers(userPagination.getAndIncrement(), new Handler<Either<String,JsonArray>>() {
						public void handle(Either<String, JsonArray> event) {
							if(event.isLeft()){
								log.error("[sendWeeklyMails] Error while retrieving impacted users : " + event.left().getValue());
								handler.handle(new Either.Left<String, JsonObject>(event.left().getValue()));
							} else {
								JsonArray users = event.right().getValue();
								usersHandler.handle(users);
							}
						}
					});
				} else {
					handler.handle(new Either.Right<String, JsonObject>(results));
				}
			}
		};

		getNotificationsDefaults(new Handler<JsonArray>() {
			public void handle(final JsonArray notifications) {
				if(notifications == null){
					log.error("[sendWeeklyMails] Error while retrieving notifications defaults.");
					return;
				} else {
					for(Object notifObj: notifications){
						final JsonObject notif = (JsonObject) notifObj;
						notificationsDefaults.putObject(notif.getString("key", ""), notif);
					}
					userContinuationHandler.handle(true);
				}
			}
		});
	}

}
