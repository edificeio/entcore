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

package org.entcore.timeline;

import fr.wseduc.cron.CronTrigger;
import fr.wseduc.webutils.request.filter.UserAuthFilter;
import fr.wseduc.webutils.security.oauth.DefaultOAuthResourceProvider;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import io.vertx.core.shareddata.LocalMap;
import org.entcore.common.http.BaseServer;
import org.entcore.common.utils.MapFactory;
import org.entcore.timeline.services.TimelineConfigService;
import org.entcore.timeline.services.impl.DefaultTimelineConfigService;
import org.entcore.timeline.services.impl.DefaultTimelineMailerService;
import org.entcore.timeline.controllers.FlashMsgController;
import org.entcore.timeline.controllers.TimelineController;
import org.entcore.timeline.cron.DailyMailingCronTask;
import org.entcore.timeline.cron.WeeklyMailingCronTask;
import org.entcore.timeline.services.impl.FlashMsgRepositoryEventsSql;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;

public class Timeline extends BaseServer {

	@Override
	public void start() throws Exception {
		super.start();

		final Map<String, String> registeredNotifications = MapFactory.getSyncClusterMap("notificationsMap", vertx);
		final LocalMap<String,String> eventsI18n = vertx.sharedData().getLocalMap("timelineEventsI18n");
		final HashMap<String, JsonObject> lazyEventsI18n = new HashMap<>();

		final TimelineConfigService configService = new DefaultTimelineConfigService("timeline.config");

		final DefaultTimelineMailerService mailerService = new DefaultTimelineMailerService(vertx, config);
		mailerService.setConfigService(configService);
		mailerService.setRegisteredNotifications(registeredNotifications);
		mailerService.setEventsI18n(eventsI18n);
		mailerService.setLazyEventsI18n(lazyEventsI18n);

		final TimelineController timelineController = new TimelineController();
		timelineController.setConfigService(configService);
		timelineController.setMailerService(mailerService);
		timelineController.setRegisteredNotifications(registeredNotifications);
		timelineController.setEventsI18n(eventsI18n);
		timelineController.setLazyEventsI18n(lazyEventsI18n);

		addController(new FlashMsgController());

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
	}

}
