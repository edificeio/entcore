package org.entcore.timeline.controllers.Helper;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Server;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.timeline.services.TimelineConfigService;
import org.entcore.timeline.services.TimelineMailerService;
import org.entcore.timeline.services.TimelinePushNotifService;
import org.entcore.common.notification.TimelineNotificationsLoader;


import java.util.Map;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class NotificationHelper {
    private static final Logger log = LoggerFactory.getLogger(NotificationHelper.class);
    private static final String USERBOOK_ADDRESS = "userbook.preferences";
    private TimelinePushNotifService pushNotifService;
    private TimelineMailerService mailerService;
    private TimelineConfigService configService;
    private Map<String, String> registeredNotifications;
    private final EventBus eb;


    public NotificationHelper(Vertx vertx, TimelineConfigService configService, Map<String, String> registeredNotifications) {
        this.configService = configService;
        this.registeredNotifications = registeredNotifications;
        this.eb = Server.getEventBus(vertx);
    }

    public void sendImmediateNotifications(final HttpServerRequest request, final JsonObject json){
        //Get notification properties (mixin : admin console configuration which overrides default properties)
        final String notificationName = json.getString("notificationName");
        final JsonObject notification = json.getJsonObject("notification");
        getNotificationProperties(notificationName, new Handler<Either<String, JsonObject>>() {
            public void handle(final Either<String, JsonObject> properties) {
                if(properties.isLeft() || properties.right().getValue() == null){
                    log.error("[NotificationHelper] Issue while retrieving notification (" + notificationName + ") properties.");
                    return;
                }
                final JsonObject notificationProperties = properties.right().getValue();
                //Get users preferences (overrides notification properties)
                getUsersPreferences(json.getJsonArray("recipientsIds"), new Handler<JsonArray>() {
                    public void handle(final JsonArray userList) {
                        if(userList == null){
                            log.error("[NotificationHelper] Issue while retrieving users preferences.");
                            return;
                        }
                        mailerService.sendImmediateMails(request, notificationName, notification, json.getJsonObject("params"), userList, notificationProperties);

                        if(pushNotifService != null && json.containsKey("pushNotif") && notificationProperties.getBoolean("push-notif") &&
                                !TimelineNotificationsLoader.Restrictions.INTERNAL.name().equals(notificationProperties.getString("restriction")) &&
                                !TimelineNotificationsLoader.Restrictions.HIDDEN.name().equals(notificationProperties.getString("restriction")))

                            pushNotifService.sendImmediateNotifs(notificationName, json, userList, notificationProperties);
                    }
                });
            }
        });
    }


    private void getNotificationProperties(final String notificationKey, final Handler<Either<String, JsonObject>> handler) {
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
                        notification.put("defaultFrequency",
                                notifConfig.getString("defaultFrequency", ""));
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
                .put("additionalCollectFields", ", language: uac.language, tokens: uac.fcmTokens ")
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


    public void setPushNotifService(TimelinePushNotifService pushNotifService) {
        this.pushNotifService = pushNotifService;
    }

    public void setMailerService(TimelineMailerService mailerService) {
        this.mailerService = mailerService;
    }
}
