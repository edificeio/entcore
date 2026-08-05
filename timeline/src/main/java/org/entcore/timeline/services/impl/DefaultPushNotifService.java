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

import com.google.common.collect.Lists;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.entcore.common.notification.TimelineNotificationsLoader;
import org.entcore.common.notification.push.PushNotifBuilder;
import org.entcore.common.notification.push.PushNotifDto;
import org.entcore.common.notification.push.PushNotifService;
import org.entcore.common.notification.push.impl.SqlPushNotifService;
import org.entcore.common.notification.ws.OssFcm;
import org.entcore.common.user.dto.QuietHoursPreference;
import org.entcore.common.user.dto.TimezonePreference;
import org.entcore.common.utils.HtmlUtils;
import org.entcore.timeline.controllers.helper.QuietHoursHelper;
import org.entcore.timeline.services.TimelinePushNotifService;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.wseduc.webutils.Utils.getOrElse;
import static org.entcore.timeline.controllers.helper.NotificationHelper.*;
import static org.entcore.timeline.controllers.helper.QuietHoursHelper.resolveTimezone;


public class DefaultPushNotifService extends Renders implements TimelinePushNotifService {

    private static final Logger log = LoggerFactory.getLogger(DefaultPushNotifService.class);

    private static final String DELAYED_TYPE = "DELAYED_TYPE";
    private static final String TIMELINE_QUIET_HOUR_RECAP_BODY = "timeline.notification.quiet-hour.body";
    private static final String TIMELINE_QUIET_HOUR_RECAP_BODIES = "timeline.notification.quiet-hour.bodies";
    private static final String TIMELINE_QUIET_HOUR_RECAP_TITLE = "timeline.notification.quiet-hour.title";
    private static final int MAX_BODY_LENGTH = 50;

    private PushNotifService pushNotifService = new SqlPushNotifService();
    private final EventBus eb;
    private final boolean legacy;
    private final OssFcm ossFcm;
    private Map<String,String> eventsI18n;
    private Map<String,JsonObject> cacheI18N = new HashMap<>();

    public DefaultPushNotifService(Vertx vertx, JsonObject config, OssFcm ossFcm) {
        super(vertx, config);
        eb = Server.getEventBus(vertx);
        this.ossFcm = ossFcm;
        this.legacy = config.getBoolean("legacy-push-notif", true);
    }

    @Override
    public void sendPushNotifs(String notificationName, JsonObject notification, JsonArray userList, JsonObject notificationProperties) {
        sendUsers(notificationName, notification, userList, notificationProperties);
    }

    private void sendUsers(final String notificationName,final JsonObject notification, final JsonArray userList, final JsonObject notificationProperties){

        for(Object userObj : userList){
            final JsonObject userPref = ((JsonObject) userObj);

            JsonObject notificationPreference = userPref
                    .getJsonObject("preferences", new JsonObject())
                    .getJsonObject("config", new JsonObject())
                    .getJsonObject(notificationName, new JsonObject());

            if( notificationPreference.getBoolean("push-notif", notificationProperties.getBoolean("push-notif")) &&
                !TimelineNotificationsLoader.Restrictions.INTERNAL.name().equals(
                        notificationPreference.getString("restriction", notificationProperties.getString("restriction"))) &&
                !TimelineNotificationsLoader.Restrictions.HIDDEN.name().equals(
                        notificationPreference.getString("restriction", notificationProperties.getString("restriction"))) &&
                userPref.getJsonArray("tokens") != null && !userPref.getJsonArray("tokens").isEmpty()){
                boolean deferred = notification.getJsonArray("recipients", new JsonArray())
                                               .stream()
                                               .map(JsonObject.class::cast)
                                                .anyMatch( r -> r.getString("userId").equals(userPref.getString("userId")) && r.getBoolean(DEFERRED_TO_DAILY, false));
                String language =  this.getUserLanguage(userPref);

                if(legacy && deferred) {
                    processMessage(notification, this.getUserLanguage(userPref), message -> {
                            for (Object token : userPref.getJsonArray("tokens")) {
                                if ("null".equals(token)) {
                                    continue;
                                }
                                try {
                                    ossFcm.sendNotifications(userPref.getString("userId"),
                                            new JsonObject().put("message", message.copy().put("token", token)));
                                } catch (Exception e) {
                                    log.error("[sendNotificationToUsers] Issue while sending notification (" + notificationName + ").", e);
                                }

                    }});
                } else {
                    //pas l'ancien sender ou bien deferred, ce que l'on ne peut pas traitre en legacy
                    QuietHoursPreference quietHoursPreference = parseQuietHours(userPref);
                    TimezonePreference timezonePreference = parseTimezone(userPref);
                    ZoneId zoneId = resolveTimezone(timezonePreference);
                    Instant nextSendTime = !deferred ? null :  QuietHoursHelper.computeNextSendTime(Instant.now(), quietHoursPreference, zoneId);

                    if(deferred) {
                        processQuietHourMessage(notification, language, h -> {
                            pushNotifService.findPending(userPref.getString("userId"), DELAYED_TYPE)
                                    .onSuccess( pendings -> upsertDeferredNotification(pendings, notification, nextSendTime, h, userPref, language))
                                    .onFailure(t -> log.error("Error while retrieving current pending push notif", t));
                        });
                    } else {
                        processMessage(notification, language, message -> {
                            PushNotifBuilder pushNotifBuilder = PushNotifBuilder.create()
                                    .withMessage(message)
                                    .withNotificationIds(Lists.newArrayList(notification.getString("_id")))
                                    .immediate()
                                    .withUserId(userPref.getString("userId"))
                                    .withStatus(PushNotifDto.Status.PENDING)
                                    .scheduledAt(nextSendTime)
                                    .withNotifType(notification.getString("type"))
                                    .withNotifSubType(notification.getString("event-type"));
                            pushNotifService.create(pushNotifBuilder)
                                    .onFailure(t -> log.error("Error while creating pushNotif", t))
                                    .onSuccess((v) -> log.info("Successfully insert notification " + pushNotifBuilder.getId() + " for user " + userPref.getString("userId")));
                        });
                    }
                }
            }
        }
    }

    private void upsertDeferredNotification(List<PushNotifDto> pushNotifs, JsonObject notification, Instant nextSendingTime, JsonObject message, JsonObject userPref, String language) {
        if (CollectionUtils.isEmpty(pushNotifs)) {
            String body = I18n.getInstance().translate(TIMELINE_QUIET_HOUR_RECAP_BODY, I18n.getLocale(language));
            body = body.length() < MAX_BODY_LENGTH ? body : body.substring(0, MAX_BODY_LENGTH)+"...";
            String updatedBody = body.replace("[[count]]", "1");

            message.getJsonObject("notification").put("body", updatedBody);
            message.getJsonObject("notification").remove("bodies");
            PushNotifBuilder pushNotifBuilder = PushNotifBuilder.create()
              .withMessage(message)
              .withNotificationIds(Lists.newArrayList(notification.getString("_id")))
              .withStatus(PushNotifDto.Status.PENDING)
              .scheduledAt(nextSendingTime)
              .withNotifType(DELAYED_TYPE)
              .withUserId(userPref.getString("userId"))
              .withMessageParams(new JsonObject().put("count", 1));
            pushNotifService.create(pushNotifBuilder)
               .onFailure(t -> log.error("Error while creating pushNotif", t));
        } else {
            PushNotifDto pushNotif = pushNotifs.get(0);
            pushNotif.getNotificationIds().add(notification.getString("_id"));
            Integer count = pushNotif.getMessageParams().getInteger("count", 1) + 1;

            String body = I18n.getInstance().translate(TIMELINE_QUIET_HOUR_RECAP_BODIES, I18n.getLocale(language));
            body = body.length() < MAX_BODY_LENGTH ? body : body.substring(0, MAX_BODY_LENGTH)+"...";
            String updatedBody = body.replace("[[count]]", count.toString());
            pushNotif.getMessageParams().put("count", count);
            PushNotifBuilder builder = PushNotifBuilder.from(pushNotif);
            JsonObject previousMessage = pushNotif.getMessage();
            previousMessage.getJsonObject("message").getJsonObject("notification").put("body", updatedBody);
            builder.withMessage(previousMessage)
                            .scheduledAt(pushNotif.getScheduleAt())
                            .withMessageParams(pushNotif.getMessageParams())
                            .withMessage(pushNotif.getMessage().getJsonObject("message"))
                            .withNotificationIds(pushNotif.getNotificationIds());
            pushNotifService.update(builder);
        }
    }

    public void processQuietHourMessage(final JsonObject notification, String language, final Handler<JsonObject> handler){
        final JsonObject message = new JsonObject();

            final JsonObject notif = new JsonObject();
            final JsonObject data = new JsonObject();
            notif.put("title", HtmlUtils.unescapeHtmlEntities(I18n.getInstance().translate(TIMELINE_QUIET_HOUR_RECAP_TITLE, I18n.getLocale(language))));
            data.put("type", "QUIET_HOUR_RECAP");
            if (notification.containsKey("sender"))
                data.put("sender", notification.getString("sender"));
            message.put("data", data);
            message.put("notification", notif);
            handler.handle(message);
    }

    public void processMessage(final JsonObject notification, String language, final Handler<JsonObject> handler){
        final JsonObject message = new JsonObject();

        translateMessage(language, keys -> {
            final JsonObject notif = new JsonObject();
            final JsonObject data = new JsonObject();
            final JsonObject apns = new JsonObject();
            final JsonObject pushNotif = notification.getJsonObject("pushNotif", new JsonObject());
            String body = pushNotif.getString("body", "");
            body = body.length() < MAX_BODY_LENGTH ? body : body.substring(0, MAX_BODY_LENGTH)+"...";

            // Caution : Push-notif length can't exceed 4kb
            // @see https://firebase.google.com/docs/cloud-messaging/http-server-ref#downstream-http-messages-plain-text

            notif.put("title", HtmlUtils.unescapeHtmlEntities(keys.getString(pushNotif.getString("title"), pushNotif.getString("title", ""))));
            notif.put("body",HtmlUtils.unescapeHtmlEntities(body));
            if (notification.containsKey("type"))
                data.put("type", notification.getString("type"));
            if (notification.containsKey("event-type"))
                data.put("event-type", notification.getString("event-type"));
            if (notification.containsKey("params"))
                data.put("params", notification.getJsonObject("params").toString());
            if (notification.containsKey("resource"))
                data.put("resource", notification.getString("resource"));
            if (notification.containsKey("sender"))
                data.put("sender", notification.getString("sender"));
            if (notification.containsKey("sub-resource"))
                data.put("sub-resource", notification.getString("sub-resource"));
            message.put("data", data);
            message.put("notification", notif);
            // "content-avaiable" is required here to make the mobile app awake every time it receives a notification.
            // When the back will be able to put the right number for the "badge" value, "content-available" could be removed to preserve user battery life.
            apns.put("payload", new JsonObject().put("aps", new JsonObject().put("content-available", 1)));
            message.put("apns", apns);

            handler.handle(message);
        });
    }

    public void translateMessage(final String language, final Handler<JsonObject> handler){
        final String key = language.split(",")[0].split("-")[0];
        if(!this.cacheI18N.containsKey(key)){
            //create cache
            final JsonObject translations;
            final String i18n = eventsI18n.get(key);
            if (i18n == null || i18n.length() == 0) {
                translations = new JsonObject();
            } else {
                translations = new JsonObject("{" + i18n.substring(0, i18n.length() - 1) + "}");
            }
            this.cacheI18N.put(key, translations);
        }
        final JsonObject translations = this.cacheI18N.get(key);
        handler.handle(translations);
    }

    public void setEventsI18n(Map<String,String> eventsI18n) {
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
