package org.entcore.timeline.services.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import org.entcore.common.notification.TimelineNotificationsLoader;
import org.entcore.timeline.services.TimelineConfigService;
import org.entcore.timeline.services.TimelinePushNotifService;
import org.entcore.timeline.ws.OssFcm;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;


import java.util.*;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class DefaultPushNotifService extends Renders implements TimelinePushNotifService {

    private static final Logger log = LoggerFactory.getLogger(DefaultPushNotifService.class);
    private static final String USERBOOK_ADDRESS = "userbook.preferences";
    private static final int MAX_BODY_LENGTH = 50;
    private TimelineConfigService configService;
    private final EventBus eb;
    private final OssFcm ossFcm;
    private LocalMap<String,String> eventsI18n;
    private Map<String, String> registeredNotifications;


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
        getNotificationProperties(notificationName, new Handler<Either<String, JsonObject>>() {
            public void handle(final Either<String, JsonObject> properties) {
                if(properties.isLeft() || properties.right().getValue() == null){
                    log.error("[sendPushNotification] Issue while retrieving notification (" + notificationName + ") properties.");
                    return;
                }
                //Get users preferences (overrides notification properties)
                getUsersPreferences(recipientIds, new Handler<JsonArray>() {
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
        getNotificationProperties(notificationName, new Handler<Either<String, JsonObject>>() {
            public void handle(final Either<String, JsonObject> properties) {
                if(properties.isLeft() || properties.right().getValue() == null){
                    log.error("[sendPushNotification] Issue while retrieving notification (" + notificationName + ") properties.");
                    return;
                }
                //Get users preferences (overrides notification properties)
                getUsersPreferences(recipientIds, new Handler<JsonArray>() {
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
                    processMessage(notification, "fr", typeNotification, typeData, new Handler<JsonObject>() {
                        @Override
                        public void handle(final JsonObject message) {
                            try {
                                ossFcm.sendNotifications(new JsonObject().put("message", message.put("token", (String) token)));
                            } catch (Exception e) {
                                e.printStackTrace();
                                log.error("[sendNotificationToUsers] Issue while sending notification (" + notificationName + ").");

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
                    e.printStackTrace();
                    log.error("[sendNotificationToTopic] Issue while sending notification (" + notificationName + ").");

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
                    e.printStackTrace();
                    log.error("[sendNotificationToCondition] Issue while sending notification (" + notificationName + ").");

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

                notif.put("title", keys.getString(pushNotif.getString("title"), ""));
                notif.put("body", body);
                if(typeData) {
                    if (notification.containsKey("params"))
                        notification.getJsonObject("params").forEach(field -> data.put(field.getKey(), field.getValue()));
                    if (notification.containsKey("resource"))
                        data.put("resource", notification.getString("resource"));
                    if (notification.containsKey("sub-resource"))
                        data.put("resource", notification.getString("resource"));
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



    public void getNotificationProperties(final String notificationKey, final Handler<Either<String, JsonObject>> handler) {
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
                        notification.put("push-notif",
                                notifConfig.getBoolean("push-notif", notification.getBoolean("push-notif")));
                        notification.put("restriction",
                                notifConfig.getString("restriction", ""));
                        break;
                    }
                }
                handler.handle(
                        new Either.Right<String, JsonObject>(notification));
            }
        });
    }

    private void getUsersPreferences(JsonArray userIds, final Handler<JsonArray> handler){
        eb.send(USERBOOK_ADDRESS, new JsonObject()
                .put("action", "get.userlist")
                .put("application", "timeline")
                .put("additionalMatch", ", u-[:IN]->(g:Group)-[:AUTHORIZED]->(r:Role)-[:AUTHORIZE]->(act:WorkflowAction) ")
                .put("additionalWhere", "AND act.name = \"org.entcore.timeline.controllers.TimelineController|mixinConfig\" ")
                .put("additionalCollectFields", ", tokens: uac.fcmTokens ")
                .put("userIds", userIds), handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> event) {
                if (!"error".equals(event.body().getString("status"))) {
                    handler.handle(event.body().getJsonArray("results"));
                } else {
                    handler.handle(null);
                }
            }
        }));
    }

    public void setConfigService(TimelineConfigService configService) {
        this.configService = configService;
    }

    public void setRegisteredNotifications(Map<String, String> registeredNotifications) {
        this.registeredNotifications = registeredNotifications;
    }

    public void setEventsI18n(LocalMap<String,String> eventsI18n) {
        this.eventsI18n = eventsI18n;
    }



}
