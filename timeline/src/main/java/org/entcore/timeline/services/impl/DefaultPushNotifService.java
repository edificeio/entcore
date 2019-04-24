/*
 * Copyright © "Open Digital Education", 2018
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

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import org.entcore.common.notification.NotificationUtils;
import org.entcore.common.notification.TimelineNotificationsLoader;
import org.entcore.timeline.services.TimelineConfigService;
import org.entcore.timeline.services.TimelinePushNotifService;
import org.entcore.common.notification.ws.OssFcm;
import org.entcore.common.utils.HtmlUtils;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;

import static fr.wseduc.webutils.Utils.getOrElse;


public class DefaultPushNotifService extends Renders implements TimelinePushNotifService {

    private static final Logger log = LoggerFactory.getLogger(DefaultPushNotifService.class);
    private static final String USERBOOK_ADDRESS = "userbook.preferences";
    private static final int MAX_BODY_LENGTH = 50;
    private TimelineConfigService configService;
    private final EventBus eb;
    private final OssFcm ossFcm;
    private LocalMap<String,String> eventsI18n;


    public DefaultPushNotifService(Vertx vertx, JsonObject config, OssFcm ossFcm) {
        super(vertx, config);
        eb = Server.getEventBus(vertx);
        this.ossFcm = ossFcm;
    }

    @Override
    public void sendImmediateNotifs(String notificationName, JsonObject notification, JsonArray userList, JsonObject notificationProperties) {
        sendUsers(notificationName, notification, userList, notificationProperties, true, true);
    }

    @Override
    public void sendNotificationMessageUsers(final String notificationName,final JsonObject notification,final JsonArray recipientIds, boolean addData) {
        configService.getNotificationProperties(notificationName, new Handler<Either<String, JsonObject>>() {
            public void handle(final Either<String, JsonObject> properties) {
                if(properties.isLeft() || properties.right().getValue() == null){
                    log.error("[sendPushNotification] Issue while retrieving notification (" + notificationName + ") properties.");
                    return;
                }
                //Get users preferences (overrides notification properties)
                NotificationUtils.getUsersPreferences(eb, recipientIds, "tokens: uac.fcmTokens", new Handler<JsonArray>() {
                    public void handle(final JsonArray userList) {
                        if(userList == null){
                            log.error("[sendPushNotification] Issue while retrieving users preferences.");
                            return;
                        }
                        sendUsers(notificationName, notification, userList, properties.right().getValue(), true, addData);

                    }
                });
            }
        });
    }

    @Override
    public void sendNotificationMessageTopic(String notificationName, JsonObject notification, JsonObject templateParameters, String topic, boolean addData) {
        this.sendTopic(notificationName, notification, topic,true, addData);
    }

    @Override
    public void sendNotificationMessageCondition(String notificationName, JsonObject notification, JsonObject templateParameters, String condition, boolean addData) {
        this.sendCondition(notificationName, notification, condition, true, addData);
    }

    @Override
    public void sendDataMessageUsers(String notificationName, JsonObject notification, JsonObject templateParameters, JsonArray recipientIds) {
        configService.getNotificationProperties(notificationName, new Handler<Either<String, JsonObject>>() {
            public void handle(final Either<String, JsonObject> properties) {
                if(properties.isLeft() || properties.right().getValue() == null){
                    log.error("[sendPushNotification] Issue while retrieving notification (" + notificationName + ") properties.");
                    return;
                }
                //Get users preferences (overrides notification properties)
                NotificationUtils.getUsersPreferences(eb, recipientIds, "tokens: uac.fcmTokens", new Handler<JsonArray>() {
                    public void handle(final JsonArray userList) {
                        if(userList == null){
                            log.error("[sendPushNotification] Issue while retrieving users preferences.");
                            return;
                        }
                        sendUsers(notificationName, notification, userList, properties.right().getValue() ,false, true);
                    }
                });
            }
        });
    }

    @Override
    public void sendDataMessageTopic(String notificationName, JsonObject notification, JsonObject templateParameters, String topic) {
        this.sendTopic(notificationName, notification, topic,false, true);
    }

    @Override
    public void sendDataMessageCondition(String notificationName, JsonObject notification, JsonObject templateParameters, String condition) {
        this.sendCondition(notificationName, notification, condition, false, true);
    }


    private void sendUsers(final String notificationName,final JsonObject notification, final JsonArray userList, final JsonObject notificationProperties, boolean typeNotification, boolean typeData){

        for(Object userObj : userList){
            final JsonObject userPref = ((JsonObject) userObj);

            JsonObject notificationPreference = userPref
                    .getJsonObject("preferences", new JsonObject())
                    .getJsonObject("config", new JsonObject())
                    .getJsonObject(notificationName, new JsonObject());

            if(notificationPreference.getBoolean("push-notif", notificationProperties.getBoolean("push-notif")) &&
                    !TimelineNotificationsLoader.Restrictions.INTERNAL.name().equals(
                            notificationPreference.getString("restriction", notificationProperties.getString("restriction"))) &&
                    !TimelineNotificationsLoader.Restrictions.HIDDEN.name().equals(
                            notificationPreference.getString("restriction", notificationProperties.getString("restriction"))) &&
                    userPref.getJsonArray("tokens") != null && userPref.getJsonArray("tokens").size() > 0){
                for(Object token : userPref.getJsonArray("tokens")){
                    processMessage(notification, this.getUserLanguage(userPref), typeNotification, typeData, new Handler<JsonObject>() {
                        @Override
                        public void handle(final JsonObject message) {
                            try {
                                ossFcm.sendNotifications(new JsonObject().put("message", message.put("token", (String) token)));
                            } catch (Exception e) {
                                log.error("[sendNotificationToUsers] Issue while sending notification (" + notificationName + ").", e);

                            }
                        }
                    });
                }

            }
        }
    }

    private void sendTopic(final String notificationName,final JsonObject notification,final String topic, boolean typeNotification, boolean typeData){

        this.processMessage(notification, "fr", typeNotification, typeData, new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject message) {
                try {
                    ossFcm.sendNotifications(new JsonObject().put("message", message.put("topic", topic)));
                } catch (Exception e) {
                    log.error("[sendNotificationToTopic] Issue while sending notification (" + notificationName + ").", e);

                }

            }
        });
    }

    private void sendCondition(final String notificationName,final JsonObject notification,final String condition,  boolean typeNotification, boolean typeData){
        this.processMessage(notification, "fr", typeNotification, typeData, new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject message) {
                try {
                    ossFcm.sendNotifications(new JsonObject().put("message", message.put("condition", condition)));
                } catch (Exception e) {
                    log.error("[sendNotificationToCondition] Issue while sending notification (" + notificationName + ").", e);

                }
            }
        });
    }


    public void processMessage(final JsonObject notification, String language, final boolean typeNotification,final boolean typeData, final Handler<JsonObject> handler){
        final JsonObject message = new JsonObject();

        translateMessage(language, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject keys) {
                final JsonObject notif = new JsonObject();
                final JsonObject data = new JsonObject();
                final JsonObject pushNotif = notification.getJsonObject("pushNotif");
                String body = pushNotif.getString("body", "");
                body = body.length() < MAX_BODY_LENGTH ? body : body.substring(0, MAX_BODY_LENGTH)+"...";

                notif.put("title", HtmlUtils.unescapeHtmlEntities(keys.getString(pushNotif.getString("title"), pushNotif.getString("title", ""))));
                notif.put("body",HtmlUtils.unescapeHtmlEntities(body));
                if(typeData) {
                    if (notification.containsKey("params"))
                        data.put("params", notification.getJsonObject("params").toString());
                    if (notification.containsKey("resource"))
                        data.put("resource", notification.getString("resource"));
                    if (notification.containsKey("sub-resource"))
                        data.put("sub-resource", notification.getString("sub-resource"));
                    if(!typeNotification)
                        data.put("notification", notif);
                    message.put("data", data);
                }
                if(typeNotification)
                    message.put("notification", notif);

                handler.handle(message);
            }
        });
    }

    public void translateMessage(String language, Handler<JsonObject> handler){
        String i18n = eventsI18n.get(language.split(",")[0].split("-")[0]);
        final JsonObject translations;
        if (i18n == null) {
            translations = new JsonObject();
        } else {
            translations = new JsonObject("{" + i18n.substring(0, i18n.length() - 1) + "}");
        }
        handler.handle(translations);
    }

    public void setConfigService(TimelineConfigService configService) {
        this.configService = configService;
    }

    public void setEventsI18n(LocalMap<String,String> eventsI18n) {
        this.eventsI18n = eventsI18n;
    }

    public String getUserLanguage(JsonObject userPref) {
        String mutableLanguage = "fr";
        try {
            mutableLanguage = getOrElse(new JsonObject(getOrElse(userPref.getString("language"), "{}", false)).getString("default-domain"), "fr", false);
        } catch(Exception e) {
            log.error("UserId [" + userPref.getString("userId", "") + "] - Bad language preferences format");
        }
        return mutableLanguage;
    }



}
