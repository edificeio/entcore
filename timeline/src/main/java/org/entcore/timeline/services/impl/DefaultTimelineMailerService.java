/*
 * Copyright © "Open Digital Education", 2017
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

package org.entcore.timeline.services.impl;

import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.AsyncResult;
import io.vertx.core.shareddata.LocalMap;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.notification.NotificationUtils;
import org.entcore.common.notification.TimelineNotificationsLoader;
import org.entcore.timeline.controllers.TimelineLambda;
import org.entcore.timeline.services.TimelineConfigService;
import org.entcore.timeline.services.TimelineMailerService;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.StringReader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static fr.wseduc.webutils.Utils.getOrElse;

public class DefaultTimelineMailerService extends Renders implements TimelineMailerService {

	private static final Logger log = LoggerFactory.getLogger(DefaultTimelineMailerService.class);
	private static final String USERBOOK_ADDRESS = "userbook.preferences";
	private final EventBus eb;
	private Map<String, String> registeredNotifications;
	private TimelineConfigService configService;
	private LocalMap<String, String> eventsI18n;
	private HashMap<String, JsonObject> lazyEventsI18n;
	private final EmailSender emailSender;
	private final int USERS_LIMIT;
	private final MongoDb mongo = MongoDb.getInstance();
	private final Neo4j neo4j = Neo4j.getInstance();

	public DefaultTimelineMailerService(Vertx vertx, JsonObject config) {
		super(vertx, config);
		eb = Server.getEventBus(vertx);
		EmailFactory emailFactory = new EmailFactory(this.vertx, config);
		emailSender = emailFactory.getSenderWithPriority(EmailFactory.PRIORITY_VERY_LOW);
		USERS_LIMIT = config.getInteger("users-loop-limit", 25);
		super.init(vertx, config);
	}

	/* Override i18n to use additional timeline translations and nested templates */
	@Override
	protected void setLambdaTemplateRequest(final HttpServerRequest request) {
		super.setLambdaTemplateRequest(request);
		TimelineLambda.setLambdaTemplateRequest(request, this.templateProcessor, eventsI18n, lazyEventsI18n);
	}

	@Override
	public void sendImmediateMails(HttpServerRequest request, String notificationName, JsonObject notification, JsonObject templateParameters, JsonArray userList, JsonObject notificationProperties) {

		final Map<String, Map<String, Map<String,String>>> processedTemplates = new HashMap<>();
		templateParameters.put("innerTemplate", notification.getString("template", ""));

		final Map<String, Map<String, Map<String,String>>> toByDomainLang = new HashMap<>();

		final AtomicInteger userCount = new AtomicInteger(userList.size());
		final Handler<Void> templatesHandler = new Handler<Void>(){
			public void handle(Void v) {
				if(userCount.decrementAndGet() == 0){
					if(toByDomainLang.size() > 0){
						//On completion : log
						final Handler<AsyncResult<Message<JsonObject>>> completionHandler = event -> {
							if(event.failed() || "error".equals(event.result().body().getString("status", "error"))){
								log.error("[Timeline immediate emails] Error while sending mails : ", event.cause());
							} else {
								log.debug("[Timeline immediate emails] Immediate mails sent.");
							}
						};

						JsonArray keys = new fr.wseduc.webutils.collections.JsonArray()
								.add("timeline.immediate.mail.subject.header")
								.add(notificationName.toLowerCase());

						for(final String domain : toByDomainLang.keySet()){
							for(final String lang : toByDomainLang.get(domain).keySet()){
								translateTimeline(keys, domain, lang, new Handler<JsonArray>() {
									public void handle(JsonArray translations) {
										for(final String userId : toByDomainLang.get(domain).get(lang).keySet()) {
											//Send mail containing the "immediate" notification
											emailSender.sendEmail(request,
													Arrays.asList(toByDomainLang.get(domain).get(lang).get(userId)),
													null,
													null,
													translations.getString(0) + translations.getString(1),
													processedTemplates.get(domain).get(lang).get(userId),
													null,
													false,
													completionHandler);
										}
									}
								});
							}
						}
					}
				}
			}
		};

		for(Object userObj : userList){
			final JsonObject userPref = ((JsonObject) userObj);
			final String userDomain = userPref.getString("lastDomain", I18n.DEFAULT_DOMAIN) == null ? I18n.DEFAULT_DOMAIN : userPref.getString("lastDomain", I18n.DEFAULT_DOMAIN);
			final String userScheme = userPref.getString("lastScheme", "http");
			String mutableLanguage = "fr";
			try {
				mutableLanguage = getOrElse(new JsonObject(getOrElse(userPref.getString("language"), "{}", false)).getString("default-domain"), "fr", false);
			} catch(Exception e) {
				log.error("UserId [" + userPref.getString("userId", "") + "] - Bad language preferences format");
			}
			final String userLanguage = mutableLanguage;

			final String userDisplayName = getOrElse(userPref.getString("displayName"), "", true);
			templateParameters.put("displayName", userDisplayName);

			if(!processedTemplates.containsKey(userDomain))
				processedTemplates.put(userDomain, new HashMap<String, Map<String,String>>());
			JsonObject notificationPreference = userPref
					.getJsonObject("preferences", new JsonObject())
					.getJsonObject("config", new JsonObject())
					.getJsonObject(notificationName, new JsonObject());
			// If the frequency is IMMEDIATE
			// and the restriction is not INTERNAL (timeline only)
			// and if the user has provided an email
			if(TimelineNotificationsLoader.Frequencies.IMMEDIATE.name().equals(
					notificationPreference.getString("defaultFrequency", notificationProperties.getString("defaultFrequency"))) &&
					!TimelineNotificationsLoader.Restrictions.INTERNAL.name().equals(
							notificationPreference.getString("restriction", notificationProperties.getString("restriction"))) &&
					!TimelineNotificationsLoader.Restrictions.HIDDEN.name().equals(
							notificationPreference.getString("restriction", notificationProperties.getString("restriction"))) &&
					userPref.getString("userMail") != null && !userPref.getString("userMail").trim().isEmpty()){
				if(!toByDomainLang.containsKey(userDomain)){
					toByDomainLang.put(userDomain,new HashMap<String, Map<String, String>>());
				}
				if(!toByDomainLang.get(userDomain).containsKey(userLanguage)){
					toByDomainLang.get(userDomain).put(userLanguage, new HashMap<String, String>());
				}
				toByDomainLang.get(userDomain).get(userLanguage).put(userPref.getString("userId", ""),userPref.getString("userMail"));
			}
			if(!processedTemplates.get(userDomain).containsKey(userLanguage)) {
				processedTemplates.get(userDomain).put(userLanguage, new HashMap<>());
			}
			processTimelineTemplate(templateParameters, "", "notifications/immediate-mail.html",
					userDomain, userScheme, userLanguage, false, new Handler<String>(){
						public void handle(String processedTemplate) {
							processedTemplates.get(userDomain).get(userLanguage).put(userPref.getString("userId", ""), processedTemplate);
							templatesHandler.handle(null);
						}
					});
		}
	}

	@Override
	public void sendImmediateMails(final HttpServerRequest request, final String notificationName, final JsonObject notification,
								   final JsonObject templateParameters, final JsonArray recipientIds){
		//Get notification properties (mixin : admin console configuration which overrides default properties)
		configService.getNotificationProperties(notificationName, new Handler<Either<String, JsonObject>>() {
			public void handle(final Either<String, JsonObject> properties) {
				if(properties.isLeft() || properties.right().getValue() == null){
					log.error("[sendImmediateMails] Issue while retrieving notification (" + notificationName + ") properties.");
					return;
				}
				//Get users preferences (overrides notification properties)
				NotificationUtils.getUsersPreferences(eb, recipientIds, "language: uac.language, displayName: u.displayName",new Handler<JsonArray>() {
					public void handle(final JsonArray userList) {
						if(userList == null){
							log.error("[sendImmediateMails] Issue while retrieving users preferences.");
							return;
						}

						sendImmediateMails(request, notificationName, notification, templateParameters, userList, properties.right().getValue());
					}
				});
			}
		});
	}

	@Override
	public void translateTimeline(JsonArray i18nKeys, String domain, String language, Handler<JsonArray> handler) {
		String i18n = eventsI18n.get(language.split(",")[0].split("-")[0]);
		final JsonObject timelineI18n;
		if (i18n == null) {
			timelineI18n = new JsonObject();
		} else {
			timelineI18n = new JsonObject("{" + i18n.substring(0, i18n.length() - 1) + "}");
		}
		timelineI18n.mergeIn(I18n.getInstance().load(language, domain));
		JsonArray translations = new fr.wseduc.webutils.collections.JsonArray();
		for(Object keyObj : i18nKeys){
			String key = (String) keyObj;
			translations.add(timelineI18n.getString(key, key));
		}
		handler.handle(translations);
	}

	@Override
	public void processTimelineTemplate(JsonObject parameters, String resourceName,
			String template, String domain, String scheme, String language, boolean reader, final Handler<String> handler) {
		final HttpServerRequest request = new JsonHttpServerRequest(new JsonObject()
				.put("headers", new JsonObject()
						.put("Host", domain)
						.put("X-Forwarded-Proto", scheme)
						.put("Accept-Language", language)));
		if(reader){
			final StringReader templateReader = new StringReader(template);
			processTemplate(request, parameters, resourceName, templateReader, new Handler<Writer>() {
				public void handle(Writer writer) {
					handler.handle(writer.toString());
				}
			});

		} else {
			processTemplate(request, template, parameters, handler);
		}
	}


	@Override
	public void sendDailyMails(int dayDelta, final Handler<Either<String, JsonObject>> handler){

		final HttpServerRequest request = new JsonHttpServerRequest(new JsonObject());
		final AtomicInteger userPagination = new AtomicInteger(0);
		final AtomicInteger endPage = new AtomicInteger(0);
		final Calendar dayDate = Calendar.getInstance();
		dayDate.add(Calendar.DAY_OF_MONTH, dayDelta);
		dayDate.set(Calendar.HOUR_OF_DAY, 0);
		dayDate.set(Calendar.MINUTE, 0);
		dayDate.set(Calendar.SECOND, 0);
		dayDate.set(Calendar.MILLISECOND, 0);
		//
		final Calendar weekEndDate = Calendar.getInstance();
		weekEndDate.add(Calendar.DAY_OF_MONTH, dayDelta + 1);
		weekEndDate.set(Calendar.HOUR_OF_DAY, 0);
		weekEndDate.set(Calendar.MINUTE, 0);
		weekEndDate.set(Calendar.SECOND, 0);
		weekEndDate.set(Calendar.MILLISECOND, 0);

		final JsonObject results = new JsonObject()
				.put("mails.sent", 0)
				.put("users.ko", 0);
		final JsonObject notificationsDefaults = new JsonObject();
		final List<String> notifiedUsers = new ArrayList<>();

		final Handler<Boolean> userContinuationHandler = new Handler<Boolean>() {

			private final Handler<Boolean> continuation = this;
			private final Handler<JsonArray> usersHandler = new Handler<JsonArray>() {
				public void handle(final JsonArray users) {
					final int nbUsers = users.size();
					if(nbUsers == 0){
						log.info("[DailyMails] Page0 : " + userPagination.get() + "/" + endPage.get());
						continuation.handle(userPagination.get() != endPage.get());
						return;
					}
					final AtomicInteger usersCountdown = new AtomicInteger(nbUsers);

					final Handler<Void> usersEndHandler = new Handler<Void>() {
						public void handle(Void v) {
							if(usersCountdown.decrementAndGet() <= 0){
								log.info("[DailyMails] Page : " + userPagination.get() + "/" + endPage.get());
								continuation.handle(userPagination.get() != endPage.get());
							}
						}
					};

					final JsonArray userIds = new fr.wseduc.webutils.collections.JsonArray();
					for(Object userObj : users)
						userIds.add(((JsonObject) userObj).getString("id", ""));
					NotificationUtils.getUsersPreferences(eb, userIds, "language: uac.language, displayName: u.displayName", new Handler<JsonArray>(){
						public void handle(JsonArray preferences) {
							for(Object userObj : preferences){
								final JsonObject userPrefs = (JsonObject) userObj;
								final String userDomain = userPrefs.getString("lastDomain", I18n.DEFAULT_DOMAIN);
								final String userScheme = userPrefs.getString("lastScheme", "http");
								String mutableUserLanguage = "fr";
								try {
									mutableUserLanguage = getOrElse(new JsonObject(getOrElse(userPrefs.getString("language"), "{}", false)).getString("default-domain"), "fr", false);
								} catch(Exception e) {
									log.error("UserId [" + userPrefs.getString("userId", "") + "] - Bad language preferences format");
								}
								final String userLanguage = mutableUserLanguage;
								final String userDisplayName = getOrElse(userPrefs.getString("displayName"), "", true);

								getUserNotifications(userPrefs.getString("userId", ""), dayDate.getTime(), weekEndDate.getTime(), new Handler<JsonArray>(){
									public void handle(JsonArray notifications) {
										if(notifications.size() == 0){
											usersEndHandler.handle(null);
											return;
										}

										SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss", Locale.forLanguageTag(userLanguage));
										final JsonArray dates = new fr.wseduc.webutils.collections.JsonArray();
										final JsonArray templates = new fr.wseduc.webutils.collections.JsonArray();

										for(Object notificationObj : notifications){
											JsonObject notification = (JsonObject) notificationObj;
											final String notificationName =
													notification.getString("type","").toLowerCase() + "." +
															notification.getString("event-type", "").toLowerCase();
											if(notificationsDefaults.getJsonObject(notificationName) == null)
												continue;

											JsonObject notificationPreference = userPrefs
													.getJsonObject("preferences", new JsonObject())
													.getJsonObject("config", new JsonObject())
													.getJsonObject(notificationName, new JsonObject());
											if(TimelineNotificationsLoader.Frequencies.DAILY.name().equals(
													notificationPrefsMixin("defaultFrequency", notificationPreference, notificationsDefaults.getJsonObject(notificationName))) &&
													!TimelineNotificationsLoader.Restrictions.INTERNAL.name().equals(
															notificationPrefsMixin("restriction", notificationPreference, notificationsDefaults.getJsonObject(notificationName))) &&
													!TimelineNotificationsLoader.Restrictions.HIDDEN.name().equals(
															notificationPrefsMixin("restriction", notificationPreference, notificationsDefaults.getJsonObject(notificationName)))){
												templates.add(new JsonObject()
														.put("template", notificationsDefaults.getJsonObject(notificationName, new JsonObject()).getString("template", ""))
														.put("params", notification.getJsonObject("params", new JsonObject())));
												dates.add(formatter.format(MongoDb.parseIsoDate(notification.getJsonObject("date"))));
											}
										}
										if(templates.size() > 0){

											JsonObject templateParams = new JsonObject()
													.put("nestedTemplatesArray", templates)
													.put("notificationDates", dates)
													.put("displayName", userDisplayName);

											processTimelineTemplate(templateParams, "", "notifications/daily-mail.html",
													userDomain, userScheme, userLanguage, false, new Handler<String>() {
														public void handle(final String processedTemplate) {
															//On completion : log
															final Handler<AsyncResult<Message<JsonObject>>> completionHandler = event -> {
																if(event.failed() || "error".equals(event.result().body().getString("status", "error"))){
																	log.error("[Timeline daily emails] Error while sending mail : ", event.cause());
																	results.put("users.ko", results.getInteger("users.ko") + 1);
																} else {
																	results.put("mails.sent", results.getInteger("mails.sent") + 1);
																}
																usersEndHandler.handle(null);
															};

															//Translate mail title
															JsonArray keys = new fr.wseduc.webutils.collections.JsonArray()
																	.add("timeline.daily.mail.subject.header");
															translateTimeline(keys, userDomain, userLanguage, new Handler<JsonArray>() {
																public void handle(JsonArray translations) {
																	//Send mail containing the "daily" notifications
																	emailSender.sendEmail(request,
																			userPrefs.getString("userMail", ""),
																			null,
																			null,
																			translations.getString(0),
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
					getImpactedUsers(notifiedUsers, userPagination.getAndIncrement(), new Handler<Either<String,JsonArray>>() {
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

		getRecipientsUsers(dayDate.getTime(), weekEndDate.getTime(), new Handler<JsonArray>() {
			@Override
			public void handle(JsonArray event) {
				if (event != null && event.size() > 0) {
					notifiedUsers.addAll(event.getList());
					endPage.set((event.size() / USERS_LIMIT) + (event.size() % USERS_LIMIT != 0 ? 1 : 0));
				} else {
					handler.handle(new Either.Right<String, JsonObject>(results));
					return;
				}
				getNotificationsDefaults(new Handler<JsonArray>() {
					public void handle ( final JsonArray notifications){
						if (notifications == null) {
							log.error("[sendDailyMails] Error while retrieving notifications defaults.");
						} else {
							for (Object notifObj : notifications) {
								final JsonObject notif = (JsonObject) notifObj;
								notificationsDefaults.put(notif.getString("key", ""), notif);
							}
							userContinuationHandler.handle(true);
						}
					}
				});
			}
		});
	}

	public void sendWeeklyMails(int dayDelta, final Handler<Either<String, JsonObject>> handler) {

		final HttpServerRequest request = new JsonHttpServerRequest(new JsonObject());
		final AtomicInteger userPagination = new AtomicInteger(0);
		final AtomicInteger endPage = new AtomicInteger(0);
		final Calendar weekDate = Calendar.getInstance();
		weekDate.add(Calendar.DAY_OF_MONTH, dayDelta - 6);
		weekDate.set(Calendar.HOUR_OF_DAY, 0);
		weekDate.set(Calendar.MINUTE, 0);
		weekDate.set(Calendar.SECOND, 0);
		weekDate.set(Calendar.MILLISECOND, 0);
		//
		final Calendar weekEndDate = Calendar.getInstance();
		weekEndDate.add(Calendar.DAY_OF_MONTH, dayDelta + 1);
		weekEndDate.set(Calendar.HOUR_OF_DAY, 0);
		weekEndDate.set(Calendar.MINUTE, 0);
		weekEndDate.set(Calendar.SECOND, 0);
		weekEndDate.set(Calendar.MILLISECOND, 0);

		final JsonObject results = new JsonObject()
				.put("mails.sent", 0)
				.put("users.ko", 0);
		final JsonObject notificationsDefaults = new JsonObject();
		final List<String> notifiedUsers = new ArrayList<>();

		final Handler<Boolean> userContinuationHandler = new Handler<Boolean>() {

			private final Handler<Boolean> continuation = this;
			private final Handler<JsonArray> usersHandler = new Handler<JsonArray>() {
				public void handle(final JsonArray users) {
					final int nbUsers = users.size();
					if (nbUsers == 0) {
						log.info("[WeeklyMails] Page0 : " + userPagination.get() + "/" + endPage.get());
						continuation.handle(userPagination.get() != endPage.get());
						return;
					}
					final AtomicInteger usersCountdown = new AtomicInteger(nbUsers);

					final Handler<Void> usersEndHandler = new Handler<Void>() {
						public void handle(Void v) {
							if (usersCountdown.decrementAndGet() <= 0) {
								log.info("[WeeklyMails] Page : " + userPagination.get() + "/" + endPage.get());
								continuation.handle(userPagination.get() != endPage.get());
							}
						}
					};

					final JsonArray userIds = new fr.wseduc.webutils.collections.JsonArray();
					for (Object userObj : users)
						userIds.add(((JsonObject) userObj).getString("id", ""));
					NotificationUtils.getUsersPreferences(eb, userIds, "language: uac.language, displayName: u.displayName", new Handler<JsonArray>() {
						public void handle(JsonArray preferences) {
							for (Object userObj : preferences) {
								final JsonObject userPrefs = (JsonObject) userObj;
								final String userDomain = userPrefs.getString("lastDomain", I18n.DEFAULT_DOMAIN);
								final String userScheme = userPrefs.getString("lastScheme", "http");
								String mutableUserLanguage = "fr";
								try {
									mutableUserLanguage = getOrElse(new JsonObject(getOrElse(userPrefs.getString("language"), "{}", false)).getString("default-domain"), "fr", false);
								} catch (Exception e) {
									log.error("UserId [" + userPrefs.getString("userId", "") + "] - Bad language preferences format");
								}
								final String userLanguage = mutableUserLanguage;
								final String userDisplayName = getOrElse(userPrefs.getString("displayName"), "", true);

								getAggregatedUserNotifications(userPrefs.getString("userId", ""), weekDate.getTime(), weekEndDate.getTime(), new Handler<JsonArray>() {
									public void handle(JsonArray notifications) {
										if (notifications.size() == 0) {
											usersEndHandler.handle(null);
											return;
										}

										final JsonArray weeklyNotifications = new fr.wseduc.webutils.collections.JsonArray();

										for (Object notificationObj : notifications) {
											JsonObject notification = (JsonObject) notificationObj;
											final String notificationName =
													notification.getString("type", "").toLowerCase() + "." +
															notification.getString("event-type", "").toLowerCase();
											if (notificationsDefaults.getJsonObject(notificationName) == null)
												continue;

											JsonObject notificationPreference = userPrefs
													.getJsonObject("preferences", new JsonObject())
													.getJsonObject("config", new JsonObject())
													.getJsonObject(notificationName, new JsonObject());
											if (TimelineNotificationsLoader.Frequencies.WEEKLY.name().equals(
													notificationPrefsMixin("defaultFrequency", notificationPreference, notificationsDefaults.getJsonObject(notificationName))) &&
													!TimelineNotificationsLoader.Restrictions.INTERNAL.name().equals(
															notificationPrefsMixin("restriction", notificationPreference, notificationsDefaults.getJsonObject(notificationName))) &&
													!TimelineNotificationsLoader.Restrictions.HIDDEN.name().equals(
															notificationPrefsMixin("restriction", notificationPreference, notificationsDefaults.getJsonObject(notificationName)))) {
												notification.put("notificationName", notificationName);
												weeklyNotifications.add(notification);
											}
										}

										final JsonObject weeklyNotificationsObj = new JsonObject();
										final JsonArray weeklyNotificationsGroupedArray = new fr.wseduc.webutils.collections.JsonArray();
										for (Object notif : weeklyNotifications) {
											JsonObject notification = (JsonObject) notif;
											if (!weeklyNotificationsObj.containsKey(notification.getString("type").toLowerCase()))
												weeklyNotificationsObj.put(notification.getString("type").toLowerCase(), new JsonObject()
														.put("link", notificationsDefaults
																.getJsonObject(notification.getString("notificationName")).getString("app-address", ""))
														.put("event-types", new fr.wseduc.webutils.collections.JsonArray()));
											weeklyNotificationsObj
													.getJsonObject(notification.getString("type").toLowerCase())
													.getJsonArray(("event-types"), new fr.wseduc.webutils.collections.JsonArray())
													.add(notification);
										}

										for (String key : weeklyNotificationsObj.getMap().keySet()) {
											weeklyNotificationsGroupedArray.add(new JsonObject()
													.put("type", key)
													.put("link", weeklyNotificationsObj.getJsonObject(key).getString("link", ""))
													.put("event-types", weeklyNotificationsObj.getJsonObject(key).getJsonArray("event-types")));
										}

										if (weeklyNotifications.size() > 0) {

											JsonObject templateParams = new JsonObject().put("notifications", weeklyNotificationsGroupedArray);
											templateParams.put("displayName", userDisplayName);

											processTimelineTemplate(templateParams, "", "notifications/weekly-mail.html",
													userDomain, userScheme, userLanguage, false, new Handler<String>() {
														public void handle(final String processedTemplate) {
															//On completion : log
															final Handler<AsyncResult<Message<JsonObject>>> completionHandler = event -> {
																if (event.failed() || "error".equals(event.result().body().getString("status", "error"))) {
																	log.error("[Timeline weekly emails] Error while sending mail : ", event.cause());
																	results.put("users.ko", results.getInteger("users.ko") + 1);
																} else {
																	results.put("mails.sent", results.getInteger("mails.sent") + 1);
																}
																usersEndHandler.handle(null);
															};

															//Translate mail title
															JsonArray keys = new fr.wseduc.webutils.collections.JsonArray()
																	.add("timeline.weekly.mail.subject.header");
															translateTimeline(keys, userDomain, userLanguage, new Handler<JsonArray>() {
																public void handle(JsonArray translations) {
																	//Send mail containing the "weekly" notifications
																	emailSender.sendEmail(request,
																			userPrefs.getString("userMail", ""),
																			null,
																			null,
																			translations.getString(0),
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
				if (continuation) {
					getImpactedUsers(notifiedUsers, userPagination.getAndIncrement(), new Handler<Either<String, JsonArray>>() {
						public void handle(Either<String, JsonArray> event) {
							if (event.isLeft()) {
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
		getRecipientsUsers(weekDate.getTime(), weekEndDate.getTime(), new Handler<JsonArray>() {
			@Override
			public void handle(JsonArray event) {
				if (event != null && event.size() > 0) {
					notifiedUsers.addAll(event.getList());
					endPage.set((event.size() / USERS_LIMIT) + (event.size() % USERS_LIMIT != 0 ? 1 : 0));
				} else {
					handler.handle(new Either.Right<String, JsonObject>(results));
					return;
				}
				getNotificationsDefaults(new Handler<JsonArray>() {
					public void handle(final JsonArray notifications) {
						if (notifications == null) {
							log.error("[sendWeeklyMails] Error while retrieving notifications defaults.");
						} else {
							for (Object notifObj : notifications) {
								final JsonObject notif = (JsonObject) notifObj;
								notificationsDefaults.put(notif.getString("key", ""), notif);
							}
							userContinuationHandler.handle(true);
						}
					}
				});

			}
		});
	}

		@Override
	public void getNotificationsDefaults(final Handler<JsonArray> handler) {
		configService.list(new Handler<Either<String, JsonArray>>() {
			public void handle(Either<String, JsonArray> event) {
				if (event.isLeft()) {
					handler.handle(null);
				} else {
					JsonArray config = event.right().getValue();

					JsonArray notificationsList = new fr.wseduc.webutils.collections.JsonArray();
					for (String key : registeredNotifications.keySet()) {
						JsonObject notif = new JsonObject(registeredNotifications.get(key));
						notif.put("key", key);
						for (Object notifConfigObj : config) {
							JsonObject notifConfig = (JsonObject) notifConfigObj;
							if (notifConfig.getString("key", "").equals(key)) {
								notif.put("defaultFrequency",
										notifConfig.getString("defaultFrequency", notif.getString("defaultFrequency")));
								notif.put("restriction",
										notifConfig.getString("restriction", notif.getString("restriction")));
								break;
							}
						}
						notificationsList.add(notif);
					}
					handler.handle(notificationsList);
				}
			}
		});
	}

	/**
	 * Retrieves all timeline notifications from mongodb for a single user, from a specific date in the past.
	 *
	 * @param userId : Userid
	 * @param from : The starting date
	 * @param handler : Handles the notifications
	 */
	private void getUserNotifications(String userId, Date from, Date to, final Handler<JsonArray> handler){
		JsonObject matcher = MongoQueryBuilder.build(
				QueryBuilder
						.start("recipients").elemMatch(QueryBuilder.start("userId").is(userId).get())
						.and("date").greaterThanEquals(from).lessThan(to));

		JsonObject keys = new JsonObject()
				.put("_id", 0)
				.put("type", 1)
				.put("event-type", 1)
				.put("params", 1)
				.put("date", 1);
		mongo.find("timeline", matcher, null, keys, new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> event) {
				if("error".equals(event.body().getString("status", "error"))){
					handler.handle(new fr.wseduc.webutils.collections.JsonArray());
				} else {
					handler.handle(event.body().getJsonArray("results"));
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

	private void getRecipientsUsers(Date from, Date to, final Handler<JsonArray> handler) {
		final JsonObject aggregation = new JsonObject();
		JsonArray pipeline = new fr.wseduc.webutils.collections.JsonArray();
		aggregation
				.put("aggregate", "timeline")
				.put("allowDiskUse", true)
				.put("pipeline", pipeline)
				.put("cursor", new JsonObject().put("batchSize", Integer.MAX_VALUE));

		JsonObject matcher = MongoQueryBuilder.build(QueryBuilder.start("date").greaterThanEquals(from).lessThan(to));
		JsonObject grouper = new JsonObject("{ \"_id\" : \"notifiedUsers\", \"recipients\" : {\"$addToSet\" : \"$recipients.userId\"}}");

		pipeline.add(new JsonObject().put("$match", matcher));
		pipeline.add(new JsonObject().put("$unwind", "$recipients"));
		pipeline.add(new JsonObject().put("$group", grouper));

		mongo.command(aggregation.toString(), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("error".equals(event.body().getString("status", "error"))) {
					handler.handle(new fr.wseduc.webutils.collections.JsonArray());
				} else {
					JsonArray r = event.body().getJsonObject("result", new JsonObject())
							.getJsonObject("cursor", new JsonObject()).getJsonArray("firstBatch");
					if (r != null && r.size() > 0) {
						handler.handle(r.getJsonObject(0).getJsonArray("recipients", new fr.wseduc.webutils.collections.JsonArray()));
					} else {
						handler.handle(new fr.wseduc.webutils.collections.JsonArray());
					}
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
	private void getImpactedUsers(List<String> recipients, int page, final Handler<Either<String, JsonArray>> handler){
		int fromIdx = page * USERS_LIMIT;
		int toIdx = page * USERS_LIMIT + USERS_LIMIT;
		if (fromIdx >= recipients.size()) {
			handler.handle(new Either.Right<String, JsonArray>(new fr.wseduc.webutils.collections.JsonArray()));
			return;
		}
		if (toIdx > recipients.size()) {
			toIdx = recipients.size();
		}
		final String query =
				"MATCH (u:User)-[:IN]->(g:Group)-[:AUTHORIZED]->(r:Role)-[:AUTHORIZE]->(act:WorkflowAction) " +
				"WHERE u.id IN {notifiedUsers} AND u.activationCode IS NULL AND u.email IS NOT NULL AND length(u.email) > 0 " +
				"AND act.name = \"org.entcore.timeline.controllers.TimelineController|mixinConfig\"" +
				"RETURN DISTINCT u.email as mail, u.id as id ";
		JsonObject params = new JsonObject()
				.put("notifiedUsers", new fr.wseduc.webutils.collections.JsonArray(recipients.subList(fromIdx, toIdx)));
		neo4j.execute(query, params, Neo4jResult.validResultHandler(handler));
	}

	/**
	 * Retrieves an aggregated list of notifications from mongodb for a single user.
	 *
	 *  Notifications are grouped by type & event-type.
	 * @param userId : Userid
	 * @param from : Starting date in the past
	 * @param to : End date used to filter notifications
	 * @param handler: Handles the notifications
	 */
	private void getAggregatedUserNotifications(String userId, Date from, Date to, final Handler<JsonArray> handler){
		final JsonObject aggregation = new JsonObject();
		JsonArray pipeline = new fr.wseduc.webutils.collections.JsonArray();
		aggregation
				.put("aggregate", "timeline")
				.put("allowDiskUse", true)
				.put("pipeline", pipeline)
				.put("cursor", new JsonObject().put("batchSize", Integer.MAX_VALUE));

		JsonObject matcher = MongoQueryBuilder.build(
				QueryBuilder
						.start("recipients").elemMatch(QueryBuilder.start("userId").is(userId).get())
						.and("date").greaterThanEquals(from).lessThan(to));
		JsonObject grouper = new JsonObject("{ \"_id\" : { \"type\": \"$type\", \"event-type\": \"$event-type\"}, \"count\": { \"$sum\": 1 } }");
		JsonObject transformer = new JsonObject("{ \"type\": \"$_id.type\", \"event-type\": \"$_id.event-type\", \"count\": 1, \"_id\": 0 }");

		pipeline.add(new JsonObject().put("$match", matcher));
		pipeline.add(new JsonObject().put("$group", grouper));
		pipeline.add(new JsonObject().put("$project", transformer));

		mongo.command(aggregation.toString(), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if("error".equals(event.body().getString("status", "error"))){
					handler.handle(new fr.wseduc.webutils.collections.JsonArray());
				} else {
					handler.handle(
							event.body().getJsonObject("result", new JsonObject())
									.getJsonObject("cursor", new JsonObject())
									.getJsonArray("firstBatch", new fr.wseduc.webutils.collections.JsonArray()));
				}
			}

		});
	}

	public void setConfigService(TimelineConfigService configService) {
		this.configService = configService;
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
