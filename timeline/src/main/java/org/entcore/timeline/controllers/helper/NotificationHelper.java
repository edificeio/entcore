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

package org.entcore.timeline.controllers.helper;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Server;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.user.dto.QuietHoursPreference;
import org.entcore.common.user.dto.TimezonePreference;
import org.entcore.timeline.services.TimelineConfigService;
import org.entcore.timeline.services.TimelineMailerService;
import org.entcore.timeline.services.TimelinePushNotifService;
import org.entcore.common.notification.TimelineNotificationsLoader;

import java.time.*;
import java.util.List;

import org.entcore.common.notification.NotificationUtils;


public class NotificationHelper {
    private static final Logger log = LoggerFactory.getLogger(NotificationHelper.class);
    private static final String USERBOOK_ADDRESS = "userbook.preferences";
    private List<TimelinePushNotifService> pushNotifServices;
    private TimelineMailerService mailerService;
    private TimelineConfigService configService;
    private final EventBus eb;


    public NotificationHelper(Vertx vertx, TimelineConfigService configServices) {
        this.configService = configServices;
        this.eb = Server.getEventBus(vertx);
    }

    public void sendImmediateNotifications(final HttpServerRequest request, final JsonObject json){
        final String notificationName = json.getString("notificationName");
        final JsonObject notification = json.getJsonObject("notification");
        if(isImmediateNotification(json)) {
            //Get notification properties (mixin : admin console configuration which overrides default properties)
            final Boolean disableMailNotification = json.getBoolean("disableMailNotification", false);
            configService.getNotificationProperties(notificationName, new Handler<Either<String, JsonObject>>() {
                public void handle(final Either<String, JsonObject> properties) {
                    if (properties.isLeft() || properties.right().getValue() == null) {
                        log.error("[NotificationHelper] Issue while retrieving notification (" + notificationName + ") properties.");
                        return;
                    }
                    final JsonObject notificationProperties = properties.right().getValue();
                    //Get users preferences (overrides notification properties)
                     NotificationUtils.getUsersPreferences(eb, json.getJsonArray("recipientsIds"),
                             "language: uac.language, displayName: u.displayName, tokens: uac.fcmTokens, " +
                             "uai: head([(u)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure) | s.UAI]), " +
                             "quietHours: uac.quietHours, " +
                             "timezone: uac.timezone ",
                            new Handler<JsonArray>() {
                        public void handle(final JsonArray userList) {
                            if (userList == null) {
                                log.error("[NotificationHelper] Issue while retrieving users preferences.");
                                return;
                            }
                            // Filter recipients currently in quiet hours based on their preference or structure's timezone
                            final JsonArray activeUserList = new JsonArray();
                            final Instant now = Instant.now();
                             for (int i = 0; i < userList.size(); i++) {
                                 JsonObject user = userList.getJsonObject(i);
                                 QuietHoursPreference userPrefQuietHours = parseQuietHours(user);
                                 TimezonePreference userPrefTimezone = parseTimezone(user);
                                 String uai = user.getString("uai");
                                  if (!QuietHoursHelper.isQuietHour(now, userPrefQuietHours, userPrefTimezone, uai)) {
                                      activeUserList.add(user);
                                  }
                             }
                            if (disableMailNotification == null || !disableMailNotification.booleanValue()) {
                                mailerService.sendImmediateMails(request, notificationName, notification, json.getJsonObject("params"), activeUserList, notificationProperties);
                            }

                            if (pushNotifServices != null && pushNotifServices.size() > 0
                                    && json.containsKey("pushNotif")
                                    && json.getJsonObject("pushNotif") != null
                                    && notificationProperties.getBoolean("push-notif")
                                    && !TimelineNotificationsLoader.Restrictions.INTERNAL.name().equals(notificationProperties.getString("restriction"))
                                    && !TimelineNotificationsLoader.Restrictions.HIDDEN.name().equals(notificationProperties.getString("restriction"))) {
                                pushNotifServices.forEach(pushNotifService -> {
                                    pushNotifService.sendImmediateNotifs(notificationName, json, activeUserList, notificationProperties);
                                });
                            }
                        }
                    });
                }
            });
        } else {
            log.debug("[NotificationHelper] Not sending future notification " + notificationName);
        }
    }

    /**
     * Determine if a notification can be sent immediately. The following checks are performed :
     * <ul>
     *     <li>if not date field is present, the notification is immediate</li>
     *     <li>if a date field is present and the date is today or before today, the notification is immediate</li>
     *     <li>if a date field is present and the date is after today (excluded), the notification is postponed</li>
     *     <li>in any other case, the notification is immediate</li>
     * </ul>
     * @param notification Notification's data
     * @return {@code true} if the notification can be sent immediately, {@code false} otherwise
     */
    private boolean isImmediateNotification(final JsonObject notification) {
        final boolean isImmediate;
        final JsonObject dateWrapper = notification.getJsonObject("date");
        if(dateWrapper == null || dateWrapper.getString("$date") == null) {
            isImmediate = true;
        } else {
            Instant now = Instant.now();
            Instant publishDate = OffsetDateTime.parse(dateWrapper.getString("$date")).toInstant();
            isImmediate = publishDate.isBefore(now);
        }
        return isImmediate;
    }



    /**
     * Parses the quietHours preference from the user JSON object.
     * Returns null if absent or malformed.
     */
    static QuietHoursPreference parseQuietHours(JsonObject user) {
        String json = user.getString("quietHours");
        if (json == null) return null;
        
        try {
            return Json.decodeValue(json, QuietHoursPreference.class);
        } catch (Exception e) {
            log.warn("[NotificationHelper] Cannot parse quietHours preference: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parses the timezone preference from the user JSON object.
     * Returns null if absent or malformed.
     */
    static TimezonePreference parseTimezone(JsonObject user) {
        String tz = user.getString("timezone");
        if (tz == null) return null;
        TimezonePreference pref = new TimezonePreference();
        pref.setTimezone(tz);
        return pref;
    }



    public void setPushNotifServices(List<TimelinePushNotifService> pushNotifServices) {
        this.pushNotifServices = pushNotifServices;
    }

    public void setMailerService(TimelineMailerService mailerService) {
        this.mailerService = mailerService;
    }
}
