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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.entcore.common.notification.NotificationUtils;


public class NotificationHelper {
    private static final Logger log = LoggerFactory.getLogger(NotificationHelper.class);
    private List<TimelinePushNotifService> pushNotifServices;
    private TimelineMailerService mailerService;
    private TimelineConfigService configService;
    private static final String DEFERRED_TO_DAILY = "deferredToDaily";
    private final EventBus eb;


    public NotificationHelper(Vertx vertx, TimelineConfigService configService) {
        this.configService = configService;
        this.eb = Server.getEventBus(vertx);
    }

    /**
     * Single Neo4j query: fetches all user data needed for delivery,
     * identifies quiet-hours recipients, and flags them with deferredToDaily.
     * Returns the userList (with language, displayName, tokens, etc.) on success,
     * null on Neo4j failure.
     *
     * <p><strong>Invariant:</strong> {@code notification.recipientsIds} and {@code notification.recipients}
     * must be synchronized (same userIds). If a userId exists in {@code recipientsIds} but is missing
     * from {@code recipients}, a warning is logged and that user is treated as non-deferred.
     */
    public void resolveUsersAndMarkDeferredRecipients(JsonArray recipientsIds, JsonObject notification, Handler<JsonArray> handler) {
        NotificationUtils.getUsersPreferences(eb, recipientsIds,
                "language: uac.language, " +
                "displayName: u.displayName, " +
                "tokens: uac.fcmTokens, " +
                "uai: head([(u)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure) | s.UAI]), " +
                "quietHours: uac.quietHours, " +
                "timezone: uac.timezone ",
                userList -> {
                    if (userList == null) {
                        log.error("[NotificationHelper] Neo4j unavailable during user resolution");
                        handler.handle(null);
                        return;
                    }
                    final Instant now = Instant.now();
                    final JsonArray recipients = notification.getJsonArray("recipients", new JsonArray());

                    final Map<String, JsonObject> recipientById = new HashMap<>();
                    for (int j = 0; j < recipients.size(); j++) {
                        JsonObject r = recipients.getJsonObject(j);
                        String uid = r.getString("userId");
                        if (uid != null) recipientById.put(uid, r);
                    }

                    for (int i = 0; i < recipientsIds.size(); i++) {
                        String uid = recipientsIds.getString(i);
                        if (uid != null && !recipientById.containsKey(uid)) {
                            log.warn("[NotificationHelper] RecipientId " + uid + " not found in recipients array");
                        }
                    }

                    for (int i = 0; i < userList.size(); i++) {
                        JsonObject user = userList.getJsonObject(i);
                        String userId = user.getString("userId");
                        if (userId == null) continue;

                        QuietHoursPreference quietHoursPreference = parseQuietHours(user);
                        TimezonePreference timezonePreference = parseTimezone(user);
                        String uai = user.getString("uai");
                        boolean isQuiet = QuietHoursHelper.isQuietHour(now, quietHoursPreference, timezonePreference, uai);

                        if (isQuiet) {
                            JsonObject recipient = recipientById.get(userId);
                            if (recipient != null) recipient.put(DEFERRED_TO_DAILY, true);
                        }
                    }
                    handler.handle(userList);
                });
    }

    /** Marks every recipient as deferred (fail-close fallback). */
    public void markAllRecipientsDeferred(JsonObject notification) {
        final JsonArray recipients = notification.getJsonArray("recipients", new JsonArray());
        for (int i = 0; i < recipients.size(); i++) {
            JsonObject recipient = recipients.getJsonObject(i);
            if (recipient != null) recipient.put(DEFERRED_TO_DAILY, true);
        }
    }

    /** Builds a Set of userIds whose recipient entry has deferredToDaily=true. */
    public Set<String> extractDeferredUserIds(JsonObject notification) {
        final Set<String> deferred = new HashSet<>();
        final JsonArray recipients = notification.getJsonArray("recipients", new JsonArray());
        for (int i = 0; i < recipients.size(); i++) {
            JsonObject recipient = recipients.getJsonObject(i);
            if (recipient != null && recipient.getBoolean(DEFERRED_TO_DAILY, false)) {
                String uid = recipient.getString("userId");
                if (uid != null) deferred.add(uid);
            }
        }
        return deferred;
    }

    /**
     * Sends immediate notifications using pre-resolved user data (NO Neo4j call).
     * Filters out any recipient whose userId is in deferredUserIds.
     *
     * @param json              The notification payload (must contain {@code recipientsIds})
     * @param deferredUserIds   Set of userIds to exclude from immediate delivery (quiet-hours recipients)
     * @param userList          FULL pre-resolved user list for ALL recipients of this notification,
     *                          not just the current chunk. Active users are computed as:
     *                          {@code json.recipientsIds ∩ userList \ deferredUserIds}
     */
    public void sendImmediateNotifications(final HttpServerRequest request, final JsonObject json, final Set<String> deferredUserIds, final JsonArray userList) {
        final String notificationName = json.getString("notificationName");
        if (!isImmediateNotification(json)) {
            log.debug("[NotificationHelper] Not sending future notification " + notificationName);
            return;
        }

        final JsonArray activeUserList = buildActiveUserList(json, deferredUserIds, userList);
        if (activeUserList.isEmpty()) {
            log.debug("[NotificationHelper] No active users for " + notificationName + ", skipping immediate send");
            return;
        }

        configService.getNotificationProperties(notificationName, properties -> {
            if (properties.isLeft() || properties.right().getValue() == null) {
                log.error("[NotificationHelper] Cannot retrieve properties for " + notificationName);
                return;
            }
            final JsonObject notificationProperties = properties.right().getValue();
            sendMails(request, json, notificationName, activeUserList, notificationProperties);
            sendPushNotifications(json, notificationName, activeUserList, notificationProperties);
        });
    }

    private JsonArray buildActiveUserList(JsonObject json, Set<String> deferredUserIds, JsonArray userList) {
        final Set<String> activeRecipientIds = new HashSet<>();
        final JsonArray recipientIds = json.getJsonArray("recipientsIds", new JsonArray());
        for (int i = 0; i < recipientIds.size(); i++) {
            String uid = recipientIds.getString(i);
            if (uid != null && !deferredUserIds.contains(uid)) {
                activeRecipientIds.add(uid);
            }
        }

        final JsonArray activeUserList = new JsonArray();
        for (int i = 0; i < userList.size(); i++) {
            JsonObject user = userList.getJsonObject(i);
            if (user != null) {
                String userId = user.getString("userId");
                if (userId != null && activeRecipientIds.contains(userId)) {
                    activeUserList.add(user);
                }
            }
        }
        return activeUserList;
    }

    private void sendMails(HttpServerRequest request, JsonObject json, String notificationName, JsonArray activeUserList, JsonObject notificationProperties) {
        if (!json.getBoolean("disableMailNotification", false)) {
            mailerService.sendImmediateMails(request, notificationName, json.getJsonObject("notification"), json.getJsonObject("params"), activeUserList, notificationProperties);
        }
    }

    private void sendPushNotifications(JsonObject json, String notificationName, JsonArray activeUserList, JsonObject notificationProperties) {
        if (pushNotifServices == null || pushNotifServices.isEmpty()) {
            return;
        }
        if (json.getJsonObject("pushNotif") == null) {
            return;
        }
        if (!Boolean.TRUE.equals(notificationProperties.getBoolean("push-notif"))) {
            return;
        }
        String restriction = notificationProperties.getString("restriction", "");
        if (TimelineNotificationsLoader.Restrictions.INTERNAL.name().equals(restriction)) {
            return;
        }
        if (TimelineNotificationsLoader.Restrictions.HIDDEN.name().equals(restriction)) {
            return;
        }
        pushNotifServices.forEach(pushNotifService -> {
            pushNotifService.sendImmediateNotifs(notificationName, json, activeUserList, notificationProperties);
        });
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
        final JsonObject dateWrapper = notification.getJsonObject("date");
        if (dateWrapper == null || dateWrapper.getString("$date") == null) {
            return true;
        }
        Instant now = Instant.now();
        Instant publishDate = OffsetDateTime.parse(dateWrapper.getString("$date")).toInstant();
        return publishDate.isBefore(now);
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
        String json = user.getString("timezone");
        if (json == null) return null;

        try {
            return Json.decodeValue(json, TimezonePreference.class);
        } catch (Exception e) {
            log.warn("[NotificationHelper] Cannot parse timezone preference: " + e.getMessage());
            return null;
        }
    }

    public void setPushNotifServices(List<TimelinePushNotifService> pushNotifServices) {
        this.pushNotifServices = pushNotifServices;
    }

    public void setMailerService(TimelineMailerService mailerService) {
        this.mailerService = mailerService;
    }
}
