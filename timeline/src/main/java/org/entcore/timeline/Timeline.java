package org.entcore.timeline;

import fr.wseduc.cron.CronTrigger;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.request.filter.UserAuthFilter;
import fr.wseduc.webutils.security.oauth.DefaultOAuthResourceProvider;

import java.text.ParseException;

import org.entcore.common.http.BaseServer;
import org.entcore.common.notification.TimelineMailer;
import org.entcore.timeline.controllers.TimelineController;
import org.entcore.timeline.cron.DailyMailingCronTask;
import org.entcore.timeline.cron.WeeklyMailingCronTask;

public class Timeline extends BaseServer {

	@Override
	public void start() {
		clearFilters();
		setOauthClientGrant(true);
		addFilter(new UserAuthFilter(new DefaultOAuthResourceProvider(getEventBus(vertx))));
		super.start();
		addController(new TimelineController());

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
