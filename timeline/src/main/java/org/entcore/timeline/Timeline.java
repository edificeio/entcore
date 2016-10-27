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
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.request.filter.UserAuthFilter;
import fr.wseduc.webutils.security.oauth.DefaultOAuthResourceProvider;

import java.text.ParseException;

import org.entcore.common.http.BaseServer;
import org.entcore.common.notification.TimelineMailer;
import org.entcore.timeline.controllers.FlashMsgController;
import org.entcore.timeline.controllers.TimelineController;
import org.entcore.timeline.cron.DailyMailingCronTask;
import org.entcore.timeline.cron.WeeklyMailingCronTask;
import org.entcore.timeline.services.impl.FlashMsgRepositoryEventsSql;

public class Timeline extends BaseServer {

	@Override
	public void start() {
		clearFilters();
		setOauthClientGrant(true);
		addFilter(new UserAuthFilter(new DefaultOAuthResourceProvider(getEventBus(vertx))));
		super.start();
		addController(new TimelineController());
		addController(new FlashMsgController());
		setRepositoryEvents(new FlashMsgRepositoryEventsSql());

		TimelineMailer mailer = new TimelineMailer(vertx, Server.getEventBus(vertx), container, container.config().getInteger("users-loop-limit", 25));
		final String dailyMailingCron = container.config().getString("daily-mailing-cron", "0 0 2 * * ?");
		final String weeklyMailingCron = container.config().getString("weekly-mailing-cron", "0 0 5 ? * MON");
		final int dailyDayDelta = container.config().getInteger("daily-day-delta", -1);
		final int weeklyDayDelta = container.config().getInteger("weekly-day-delta", -1);

		try {
			new CronTrigger(vertx, dailyMailingCron).schedule(new DailyMailingCronTask(mailer, dailyDayDelta));
			new CronTrigger(vertx, weeklyMailingCron).schedule(new WeeklyMailingCronTask(mailer, weeklyDayDelta));
		} catch (ParseException e) {
			log.error("Failed to start mailing crons.");
		}
	}

}
