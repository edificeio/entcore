package org.entcore.timeline.controllers.helper;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Server;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.timeline.services.TimelineConfigService;
import org.entcore.timeline.services.TimelineMailerService;
import org.entcore.timeline.services.TimelinePushNotifService;
import org.entcore.common.notification.TimelineNotificationsLoader;
import org.entcore.common.notification.NotificationUtils;


public class NotificationHelper {
    private static final Logger log = LoggerFactory.getLogger(NotificationHelper.class);
    private static final String USERBOOK_ADDRESS = "userbook.preferences";
    private TimelinePushNotifService pushNotifService;
    private TimelineMailerService mailerService;
    private TimelineConfigService configService;
    private final EventBus eb;


    public NotificationHelper(Vertx vertx, TimelineConfigService configServices) {
        this.configService = configServices;
        this.eb = Server.getEventBus(vertx);
    }

    public void sendImmediateNotifications(final HttpServerRequest request, final JsonObject json){
        //Get notification properties (mixin : admin console configuration which overrides default properties)
        final String notificationName = json.getString("notificationName");
        final JsonObject notification = json.getJsonObject("notification");
        configService.getNotificationProperties(notificationName,  new Handler<Either<String, JsonObject>>() {
            public void handle(final Either<String, JsonObject> properties) {
                if(properties.isLeft() || properties.right().getValue() == null){
                    log.error("[NotificationHelper] Issue while retrieving notification (" + notificationName + ") properties.");
                    return;
                }
                final JsonObject notificationProperties = properties.right().getValue();
                //Get users preferences (overrides notification properties)
                NotificationUtils.getUsersPreferences(eb, json.getJsonArray("recipientsIds"), "language: uac.language, tokens: uac.fcmTokens ", new Handler<JsonArray>() {
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


    public void setPushNotifService(TimelinePushNotifService pushNotifService) {
        this.pushNotifService = pushNotifService;
    }

    public void setMailerService(TimelineMailerService mailerService) {
        this.mailerService = mailerService;
    }
}
