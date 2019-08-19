/*
 * Copyright © "Open Digital Education", 2016
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

package org.entcore.timeline;

import fr.wseduc.cron.CronTrigger;

import java.net.URI;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import io.vertx.core.shareddata.LocalMap;
import fr.wseduc.webutils.http.oauth.OAuth2Client;
import org.entcore.common.http.BaseServer;
import org.entcore.common.utils.MapFactory;
import org.entcore.timeline.controllers.helper.NotificationHelper;
import org.entcore.timeline.services.FlashMsgService;
import org.entcore.timeline.services.impl.*;
import org.entcore.timeline.controllers.FlashMsgController;
import org.entcore.timeline.controllers.TimelineController;
import org.entcore.timeline.cron.DailyMailingCronTask;
import org.entcore.timeline.cron.WeeklyMailingCronTask;
import org.entcore.common.notification.ws.OssFcm;
import io.vertx.core.json.JsonObject;

public class Timeline extends BaseServer {

	@Override
	public void start() throws Exception {
		super.start();

		final Map<String, String> registeredNotifications = MapFactory.getSyncClusterMap("notificationsMap", vertx);
		final LocalMap<String,String> eventsI18n = vertx.sharedData().getLocalMap("timelineEventsI18n");
		final HashMap<String, JsonObject> lazyEventsI18n = new HashMap<>();

		final DefaultTimelineConfigService configService = new DefaultTimelineConfigService("timeline.config");
		configService.setRegisteredNotifications(registeredNotifications);
		final DefaultTimelineMailerService mailerService = new DefaultTimelineMailerService(vertx, config);
		mailerService.setConfigService(configService);
		mailerService.setRegisteredNotifications(registeredNotifications);
		mailerService.setEventsI18n(eventsI18n);
		mailerService.setLazyEventsI18n(lazyEventsI18n);

		final NotificationHelper notificationHelper = new NotificationHelper(vertx, configService);
		notificationHelper.setMailerService(mailerService);

		JsonObject pushNotif = config.getJsonObject("push-notif");

		final TimelineController timelineController = new TimelineController();
		timelineController.setConfigService(configService);
		timelineController.setMailerService(mailerService);
		timelineController.setRegisteredNotifications(registeredNotifications);
		timelineController.setEventsI18n(eventsI18n);
		timelineController.setLazyEventsI18n(lazyEventsI18n);

		if(pushNotif != null){

			OAuth2Client googleOAuth2SSO = new OAuth2Client(URI.create(pushNotif.getString("uri")),
					null, null, null,
					pushNotif.getString("tokenUrn"), null, vertx,
					pushNotif.getInteger("poolSize", 16), true);
			OssFcm oss = new OssFcm(googleOAuth2SSO, pushNotif.getString("client_mail") , pushNotif.getString("scope"),
					pushNotif.getString("aud"), pushNotif.getString("url"), pushNotif.getString("key"));

			final DefaultPushNotifService pushNotifService = new DefaultPushNotifService(vertx, config, oss);
			pushNotifService.setEventsI18n(eventsI18n);
			pushNotifService.setConfigService(configService);
			timelineController.setPushNotifService(pushNotifService);
			notificationHelper.setPushNotifService(pushNotifService);
		}

		timelineController.setNotificationHelper(notificationHelper);

		final FlashMsgService flashMsgService = new FlashMsgServiceSqlImpl("flashmsg", "messages");
		final FlashMsgController flashMsgController = new FlashMsgController();
		flashMsgController.setFlashMessagesService(flashMsgService);
		addController(flashMsgController);

		setRepositoryEvents(new FlashMsgRepositoryEventsSql());

		addController(timelineController);

		final String dailyMailingCron = config.getString("daily-mailing-cron", "0 0 2 * * ?");
		final String weeklyMailingCron = config.getString("weekly-mailing-cron", "0 0 5 ? * MON");
		final int dailyDayDelta = config.getInteger("daily-day-delta", -1);
		final int weeklyDayDelta = config.getInteger("weekly-day-delta", -1);

		try {
			new CronTrigger(vertx, dailyMailingCron).schedule(new DailyMailingCronTask(mailerService, dailyDayDelta));
			new CronTrigger(vertx, weeklyMailingCron).schedule(new WeeklyMailingCronTask(mailerService, weeklyDayDelta));
		} catch (ParseException e) {
			log.error("Failed to start mailing crons.");
		}

		final String purgeMessagesReadCron = config.getString("purge-messages-read-cron", "0 0 2 * * ?");
		if (purgeMessagesReadCron != null) {
			try {
				new CronTrigger(vertx, purgeMessagesReadCron).schedule(l -> {
					flashMsgService.purgeMessagesRead(res -> {
						if (res.isLeft()) {
							log.error("[Timeline - FlashMessages] - Purge of flashmsg.messages_read failed - " + res.left().getValue());
						} else {
							log.info("[Timeline - FlashMessages] - Purge of flashmsg.messages_read succeeded");
						}
					});
				});
			} catch (ParseException e) {
				log.error("Invalid cron expression.", e);
			}
		}
	}

}
